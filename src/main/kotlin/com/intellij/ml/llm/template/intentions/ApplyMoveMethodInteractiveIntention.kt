package com.intellij.ml.llm.template.intentions

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.intellij.codeInsight.unwrap.ScopeHighlighter
import com.intellij.icons.AllIcons
import com.intellij.lang.jvm.JvmModifier
import com.intellij.ml.llm.template.LLMBundle
import com.intellij.ml.llm.template.models.LLMBaseResponse
import com.intellij.ml.llm.template.models.sendChatRequest
import com.intellij.ml.llm.template.prompts.MethodPromptBase
import com.intellij.ml.llm.template.prompts.MoveMethodRefactoringPrompt
import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.refactoringobjects.movemethod.MoveMethodFactory
import com.intellij.ml.llm.template.settings.RefAgentSettingsManager
import com.intellij.ml.llm.template.showEFNotification
import com.intellij.ml.llm.template.telemetry.EFTelemetryDataElapsedTimeNotificationPayload
import com.intellij.ml.llm.template.telemetry.TelemetryDataAction
import com.intellij.ml.llm.template.telemetry.TelemetryElapsedTimeObserver
import com.intellij.ml.llm.template.toolwindow.logViewer
import com.intellij.ml.llm.template.ui.RefactoringSuggestionsPanel
import com.intellij.ml.llm.template.utils.*
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.IconButton
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.psi.*
import com.intellij.ui.awt.RelativePoint
import dev.langchain4j.data.message.ChatMessage
import java.awt.Point
import java.awt.Rectangle
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.min
import kotlin.system.measureTimeMillis


open class ApplyMoveMethodInteractiveIntention : ApplySuggestRefactoringIntention() {
    var MAX_ITERS = RefAgentSettingsManager.getInstance().getNumberOfIterations()
    override var prompter: MethodPromptBase = MoveMethodRefactoringPrompt()
    lateinit var currentEditor: Editor
    lateinit var currentFile: PsiFile
    lateinit var currentProject: Project
    val SUGGESTIONS4USER = 3
    val tokenLimit = 128000
    val logger = Logger.getInstance(this::class.java)
    var showSuggestions = true

    data class MoveMethodSuggestion(
        @SerializedName("method_name")
        val methodName:String,
        @SerializedName("method_signature")
        val methodSignature: String,
        @SerializedName("target_class")
        val targetClass: String,
        @SerializedName("rationale")
        val rationale:String,

        @Transient
        val psiMethod: PsiMethod
    )

    override fun getFamilyName(): String {
        return LLMBundle.message("intentions.apply.suggest.refactoring.move.method.family.name")
    }

    override fun getText(): String {
        return LLMBundle.message("intentions.apply.suggest.refactoring.move.method.family.name")
    }

    override fun invokeLLM(project: Project, promptIterator: Iterator<MutableList<ChatMessage>>, editor: Editor, file: PsiFile) {
        try{
            val totalPluginTime = measureTimeMillis { invokeMoveMethodPlugin(project, promptIterator, editor, file) }
            telemetryDataManager.setTotalTime(totalPluginTime)
        } catch (e: Exception){
            e.printStackTrace()
            telemetryDataManager.addCandidatesTelemetryData(buildCandidatesTelemetryData(0, emptyList()))
            telemetryDataManager.setRefactoringObjects(emptyList())
            sendTelemetryData()
        }
    }
    private fun invokeMoveMethodPlugin(project: Project, promptIterator: Iterator<MutableList<ChatMessage>>, editor: Editor, file: PsiFile) {

        currentFile = file
        currentEditor = editor
        currentProject = project

        MAX_ITERS = RefAgentSettingsManager.getInstance().getNumberOfIterations()
        llmChatModel = RefAgentSettingsManager.getInstance().createAndGetAiModel()!!
        logViewer.clear()

        var methods : List<PsiMethod> = emptyList()
        val bruteForceSuggestions = runReadAction {
            methods = PsiUtils.getAllMethodsInClass(functionPsiElement as PsiClass)
            telemetryDataManager.addMethodCount(methods.size)
            methods.filter { !it.isConstructor } // filter constructors
                .filter { !it.isDeprecated }    //filter deprecated methods
                .filter { !it.name[0].isUpperCase() } // filter constructors
                .filter { !isGetter(it) } // filter out getters and setters.
                .filter { !isSetter(it) } // filter out getters and setters.
                .filter { !it.text.contains("@Override") } // remove methods in an inheritance chain
                .filter { !it.text.contains("@Test") } // remove test methods.
                .filter { !it.hasModifier(JvmModifier.ABSTRACT) } // filter out abstract methods because they have no body.
                .filter { it.body?.statements?.isNotEmpty() ?: false } // Exclude methods with empty bodies
                .filter { it.body!!.statements.any { statement -> statement !is PsiComment } } // filter out methods with a body that only contains comments
                .map { MoveMethodSuggestion(it.name, getSignatureString(it), "", "", it) }
        }
        val methodCompatibilitySuggestionsWithSore = getMethodCompatibility(bruteForceSuggestions, functionPsiElement as PsiClass)
        val methodCompatibilitySuggestions = methodCompatibilitySuggestionsWithSore.map { it.first }
        addMethodCompatibilityData(methodCompatibilitySuggestionsWithSore)
        logMethods(bruteForceSuggestions, -1, 0)
        logMethods(methodCompatibilitySuggestions, -2, 0)
//        telemetryDataManager.setRefactoringObjects(emptyList())
//        sendTelemetryData()
        log2fileAndViewer("*** Combining responses from iterations ***", logger)
//        sendTelemetryData()
        val priority = getSuggestionPriority(methodCompatibilitySuggestions, project)

        if (priority != null){
            log2fileAndViewer("Prioritising suggestions...", logger)
            logPriority(priority)
            if (priority.size == 0) {
                telemetryDataManager.addCandidatesTelemetryData(buildCandidatesTelemetryData(0, emptyList()))
                telemetryDataManager.setRefactoringObjects(emptyList())
                sendTelemetryData()
            }
//            sendTelemetryData()
        }

        if (methodCompatibilitySuggestions.isEmpty()) {
            telemetryDataManager.addCandidatesTelemetryData(buildCandidatesTelemetryData(0, emptyList()))
            telemetryDataManager.setRefactoringObjects(emptyList())
            // show message to user.
            invokeLater {
                showEFNotification(
                    project,
                    LLMBundle.message("notification.extract.function.with.llm.no.suggestions.message"),
                    NotificationType.INFORMATION
                )
            }
            sendTelemetryData()
        } else {
            createRefactoringObjectsAndShowSuggestions(
                methodCompatibilitySuggestions.subList(
                    0,
                    min(SUGGESTIONS4USER, methodCompatibilitySuggestions.size)
                )
            )
//            sendTelemetryData()
        }
    }

    private fun isSetter(it: PsiMethod) : Boolean {
        return ((it.name.startsWith("set") && it.parameterList.parameters.size == 1)) || setSingleVariable(it)
    }

    private fun setSingleVariable(it: PsiMethod): Boolean {
        if (it.body==null)
            return false
        return it.body!!.statements.size==1
                && it.body!!.statements[0] is PsiExpressionStatement
                && (it.body!!.statements[0] as PsiExpressionStatement).children[0] is PsiAssignmentExpression
    }

    private fun isGetter(it: PsiMethod) : Boolean {
        return ((it.name.startsWith("get") && it.parameterList.isEmpty))
                || returnsSingleField(it)
    }

    private fun returnsSingleField(it: PsiMethod): Boolean {
        if (it.body==null)
            return false
        return (
                it.body!!.statements.size == 1
                && it.body!!.statements.get(0) is PsiReturnStatement
                && (it.body!!.statements.get(0) as PsiReturnStatement).returnValue is PsiReferenceExpression)
    }

    private fun getSignatureString(psiMethod: PsiMethod) = "${psiMethod.modifierList.text} ${psiMethod.name}${psiMethod.parameterList.text}"

    private fun createRefactoringObjectsAndShowSuggestions(moveMethodSuggestions: List<MoveMethodSuggestion>) {
            val refObjs = moveMethodSuggestions
                .map {
                    MoveMethodFactory.createMoveMethodFromPsiMethod(
                        currentEditor,
                        currentFile,
                        it.psiMethod,
                        currentProject,
                        llmChatModel,
                        telemetryDataManager
                    )
                }
                .reduce { acc, abstractRefactorings -> acc + abstractRefactorings }
            telemetryDataManager.setRefactoringObjects(refObjs)
            if (refObjs.isEmpty()){
                invokeLater {
                    showEFNotification(
                        currentProject,
                        LLMBundle.message("notification.extract.function.with.llm.no.extractable.candidates.message"),
                        NotificationType.INFORMATION
                    )
                    sendTelemetryData()
                }
                return
            }
            val candidatesApplicationTelemetryObserver = EFCandidatesApplicationTelemetryObserver()
            telemetryDataManager.addCandidatesTelemetryData(
                buildCandidatesTelemetryData(
                    refObjs.size,
                    candidatesApplicationTelemetryObserver.getData()
                )
            )
            if(showSuggestions) {
                invokeLater { showRefactoringOptionsPopup(currentProject, currentEditor, currentFile, refObjs, codeTransformer)}
            }
            else
                sendTelemetryData()


    }
    fun showRefactoringOptionsPopup(
        project: Project,
        editor: Editor,
        file: PsiFile,
        candidates: List<AbstractRefactoring>,
        codeTransformer: CodeTransformer
    ) {
        val highlighter = AtomicReference(ScopeHighlighter(editor))
        val efPanel = RefactoringSuggestionsPanel(
            project = project,
            editor = editor,
            file = file,
            candidates = candidates,
            codeTransformer = codeTransformer,
            highlighter = highlighter,
            efTelemetryDataManager = telemetryDataManager,
            button_name = LLMBundle.message("ef.candidates.popup.extract.function.button.title")
        )
        efPanel.initTable()
        val elapsedTimeTelemetryDataObserver = TelemetryElapsedTimeObserver()
        efPanel.addObserver(elapsedTimeTelemetryDataObserver)
        val panel = efPanel.createPanel()

        // Create the popup
        val efPopup =
            JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, efPanel.myRefactoringCandidateTable)
                .setRequestFocus(true)
                .setTitle(LLMBundle.message("ef.candidates.popup.title"))
                .setResizable(true)
                .setMovable(true)
                .setCancelOnClickOutside(false)
                .setCancelButton(IconButton("Close", AllIcons.Actions.Close))
                .setCancelOnOtherWindowOpen(false)
                .setCancelOnWindowDeactivation(false)
                .createPopup()

        // Add onClosed listener
        efPopup.addListener(object : JBPopupListener {
            override fun onClosed(event: LightweightWindowEvent) {
                elapsedTimeTelemetryDataObserver.update(
                    EFNotification(
                        EFTelemetryDataElapsedTimeNotificationPayload(TelemetryDataAction.STOP, 0)
                    )
                )
                buildElapsedTimeTelemetryData(elapsedTimeTelemetryDataObserver)
                highlighter.getAndSet(null).dropHighlight()
                sendTelemetryData()
            }

            override fun beforeShown(event: LightweightWindowEvent) {
                super.beforeShown(event)
                elapsedTimeTelemetryDataObserver.update(
                    EFNotification(
                        EFTelemetryDataElapsedTimeNotificationPayload(TelemetryDataAction.START, 0)
                    )
                )
            }
        })

        // set the popup as delegate to the Extract Function panel
        efPanel.setDelegatePopup(efPopup)

        // Show the popup at the top right corner of the current editor
        val contentComponent = editor.contentComponent
        val visibleRect: Rectangle = contentComponent.visibleRect
        val point = Point(visibleRect.x + visibleRect.width - 500, visibleRect.y)
        efPopup.show(RelativePoint(contentComponent, point))
    }

    private fun getSuggestionPriority(
        uniqueSuggestions: List<MoveMethodSuggestion>,
        project: Project
    ) : List<MoveMethodSuggestion>? {
        val psiClass = runReadAction{ functionPsiElement as? PsiClass }
        if (psiClass == null) {
            log2fileAndViewer("Error: functionPsiElement is not a PsiClass", logger)
            return emptyList()
        }
        val messages =
            (prompter as MoveMethodRefactoringPrompt).askForMethodPriorityPrompt(
                condenseMethodCode(functionPsiElement, uniqueSuggestions),
                uniqueSuggestions, uniqueSuggestions.size
            )
        val response: LLMBaseResponse?
        val llmResponseTime = measureTimeMillis { response = llmResponseCache[messages.toString()]?:sendChatRequest(project, messages, llmChatModel)}
        if (response != null) {
            val methodPrioritySignatures = try {
                Gson().fromJson(
                    JsonUtils.sanitizeJson(response.getSuggestions()[0].text),
                    mutableListOf<String>()::class.java
                )
            } catch (e: Exception) {
                log2fileAndViewer("LLM Response: " + response.getSuggestions()[0].text, logger)
                telemetryDataManager.addLLMPriorityResponse(response.getSuggestions()[0].text, llmResponseTime)
                null
            }
            if (methodPrioritySignatures!=null) {
                llmResponseCache.put(messages.toString(), response)
                val filteredSuggestions = uniqueSuggestions.filter { suggestion ->
                    methodPrioritySignatures.any {suggestion.methodName in it}
                }
                val sortedSuggestions = filteredSuggestions.sortedBy {
                    val index = methodPrioritySignatures.indexOf(it.methodSignature)
                    if (index == -1) {
                        uniqueSuggestions.size + 1
                    } else {
                        index
                    }
                }
//                methodPrioritySignatures.get(0) == uniqueSuggestions.get(0).methodSignature
                telemetryDataManager.addLLMPriorityResponse(sortedSuggestions.map{it.methodName}, llmResponseTime)
                return sortedSuggestions
            }
        }
        return null
    }

    private fun getMethodCompatibility(
        uniqueSuggestions: List<MoveMethodSuggestion>,
        psiClass: PsiClass
    ): List<Pair<MoveMethodSuggestion, Double>> {
        val methodSimilarity = runReadAction {
            uniqueSuggestions.map { suggestion ->
                if (suggestion.psiMethod == null) {
                    log2fileAndViewer("Method ${suggestion.methodName} not found in class", logger)
                    return@map Pair(suggestion, -1.0)
                }
                val methodIndex = psiClass.text.indexOf(suggestion.psiMethod.text)
                val classTextWithoutMethod = if (methodIndex==-1){
                    psiClass.text
                }else {

                    psiClass.text.substring(
                        0,
                        methodIndex
                    ) + psiClass.text.substring(methodIndex + suggestion.psiMethod.text.length, psiClass.text.length)
                }

                val methodText = suggestion.psiMethod.body?.text ?: ""                
                val similarity = PsiUtils.computeCosineSimilarity(methodText, classTextWithoutMethod)
//                val voyageAiEmbeddingModelIT = VoyageAiEmbeddingModelIT()
//                val voyageSimilarity = voyageAiEmbeddingModelIT.computeVoyageAiCosineSimilarity(methodText, classTextWithoutMethod, VoyageAiEmbeddingModelName.VOYAGE_3_LITE)
                Pair(suggestion, similarity)
            }.sortedBy { it.second }
    //                .map { it.first }
        }

        val top15SuggestionsWithScore = methodSimilarity.subList(0, min(15, methodSimilarity.size))
//        val top15MM = top15Suggestions.map { it.first }
//        logMethods(top15MM, -2, 0)
        return top15SuggestionsWithScore
    }

    private fun isInputSizeWithinLimit(text: String): Boolean {
        logger.warn("Input size is ${text.length / 4} characters.")
        val inputTextSize = text.length / 4
        return inputTextSize <= llmContextLimit
    }

    private fun condenseMethodCode(
        functionPsi: PsiElement,
        uniqueSuggestions: List<MoveMethodSuggestion>
    ): String {
        if (!isInputSizeWithinLimit(runReadAction{functionPsi.text})){
            logger.warn("Input size exceeds the LLM context limit of $llmContextLimit characters.")
        }

        return runReadAction{ functionPsi.text }
//        if (runReadAction{functionPsi.text }.split("\\s+".toRegex()).size > llmContextLimit) {
//            return runReadAction{
//                val classPsi = functionPsi as PsiClass
//                val onlyMethodNames = uniqueSuggestions.map { it.methodName }
//                val classSourceBuilder = StringBuilder()
//                classSourceBuilder.append(classPsi.text.substring(0, classPsi.lBrace?.textOffset ?: 0))
//                classPsi.methods.filter { method ->
//                    onlyMethodNames.contains(method.name)
//                }.forEach { method ->
//                    classSourceBuilder.append("\n").append(method.text).append("\n")
//                }
//                classSourceBuilder.append("}")
//                return@runReadAction classSourceBuilder.toString()
//            }
//        } else {
//
//        }
    }

    override fun processLLMResponse(response: LLMBaseResponse, project: Project, editor: Editor, file: PsiFile) {
        throw Exception("Shouldn't be here.")
    }

    private fun logMethods(moveMethodSuggestions: List<MoveMethodSuggestion>, iter: Int, llmRequestTime: Long){
        if (moveMethodSuggestions.isEmpty()){
            log2fileAndViewer("No suggestions from llm", logger)
            telemetryDataManager.addMovesSuggestedInIteration(iter, emptyList(), llmRequestTime)
            return
        }

        log2fileAndViewer(logMessage = "LLM suggested to move:", logger = logger)
        moveMethodSuggestions.forEachIndexed {
             index, moveMethodSuggestion ->  log2fileAndViewer(logMessage = "${index+1}. ${moveMethodSuggestion.methodSignature}", logger = logger)
        }
        log2fileAndViewer(logMessage = "LLM took $llmRequestTime ms to respond", logger = logger)
        telemetryDataManager.addMovesSuggestedInIteration(
            iter,
            moveMethodSuggestions,
            llmRequestTime
        )
    }

    private fun addMethodCompatibilityData(methodCompatibilitySuggestions: List<Pair<MoveMethodSuggestion, Double>>) {
        telemetryDataManager.addMethodCompatibility(
            methodCompatibilitySuggestions
        )

    }

    private fun logPriority(moveMethodSuggestions: List<MoveMethodSuggestion>){
        log2fileAndViewer(logMessage = "Priority of methods to move, according to LLM:", logger = logger)
        moveMethodSuggestions.forEachIndexed {
                index, moveMethodSuggestion ->  log2fileAndViewer(logMessage = "${index+1}. ${moveMethodSuggestion.methodSignature}", logger = logger)
        }
    }


}