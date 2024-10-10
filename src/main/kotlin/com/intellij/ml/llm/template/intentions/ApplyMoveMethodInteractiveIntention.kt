package com.intellij.ml.llm.template.intentions

import ai.grazie.utils.isCapitalized
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import com.intellij.codeInsight.unwrap.ScopeHighlighter
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
    val SUGGESTIONS4USER = 20
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
        val psiMethod: PsiMethod?
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

        val vanillaLLMSuggestions = mutableListOf<MoveMethodSuggestion>()

        for (iter in 1..MAX_ITERS){
            log2fileAndViewer("******** ITERATION-$iter ********", logger)
            val cacheKey = functionSrc + iter.toString()
            val response : LLMBaseResponse?
            val llmRequestTime = measureTimeMillis{ response = llmResponseCache[cacheKey] ?: sendChatRequest(project, promptIterator.next(), llmChatModel) }

            if (response!=null) {
                val llmText = response.getSuggestions()[0]
                val processed = JsonUtils.sanitizeJson(llmText.text)
                val refactoringSuggestions =try {
                    (JsonParser.parseString(processed) as JsonArray)
                        .map {
                            try{
                                Gson().fromJson(it, MoveMethodSuggestion::class.java )
                            } catch (e: Exception){
                                print("failed to decode json ->$it")
                                null
                            }
                        }.filterNotNull()
                } catch (e: Exception) {
                    print("Failed to parse ${processed}")
                    log2fileAndViewer("LLM response: ${processed}", logger)
                    e.printStackTrace()
                    logMethods(listOf(MoveMethodSuggestion("failed to unparse",
                        "failed to unparse","failed to unparse",
                        processed, null)), iter, llmRequestTime)
                    null
                }
                if (refactoringSuggestions!=null) {
                    llmResponseCache[cacheKey] ?: llmResponseCache.put(cacheKey, response) // cache response
//                    val augSuggestions = refactoringSuggestions.map {
//                        functionPsiElement
////                        it.methodSignature
////                        val psiMethod = PsiUtils.getMethodWithSignatureFromClass(functionPsiElement)
////                        MoveMethodSuggestion(it.methodName, it.methodSignature, it.targetClass, it.rationale, psiMethod)
//                    }
                    vanillaLLMSuggestions.addAll(refactoringSuggestions)
                    logMethods(refactoringSuggestions, iter, llmRequestTime)
                }
            }
        }

        val uniqueSuggestions = vanillaLLMSuggestions.distinctBy { it.methodName }
        var methods : List<PsiMethod> = emptyList()
        val bruteForceSuggestions = runReadAction {
            methods = PsiUtils.getAllMethodsInClass(functionPsiElement as PsiClass)
            telemetryDataManager.addMethodCount(methods.size)
            methods
                .filter { !it.name.isCapitalized() } // filter constructors
                .filter { !(it.name.startsWith("set") && it.parameterList.parameters.size==1)} // filter out getters and setters.
//                .filter{ !it.isOverridableElement() }
                .filter { !it.text.contains("@Override") }
                .filter { !it.text.contains("@Test")}
                .filter { !it.hasModifierProperty(PsiModifier.ABSTRACT) }
                .filter { method ->
                            method.body != null &&
                                !method.body!!.statements.all { it is PsiComment }
                }
                .map { MoveMethodSuggestion(it.name, it.getSignature(PsiSubstitutor.EMPTY).toString(), "", "", it) }
        }

        val methodCompatibilitySuggestionsWithSore = getMethodCompatibility(bruteForceSuggestions, functionPsiElement as PsiClass, project)

        if (methodCompatibilitySuggestionsWithSore == null) {
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
        }
        else {
            log2fileAndViewer("Prioritising suggestions...", logger)
//            val priority = getSuggestionPriority(methodCompatibilitySuggestions, project)
            val priority = methodCompatibilitySuggestionsWithSore
            if (priority!=null){
                logPriority(priority)
                if (priority.size==0) {
                    telemetryDataManager.addCandidatesTelemetryData(buildCandidatesTelemetryData(0, emptyList()))
                    telemetryDataManager.setRefactoringObjects(emptyList())
                    invokeLater { showEFNotification(
                        project,
                        LLMBundle.message("notification.extract.function.with.llm.no.suggestions.message"),
                        NotificationType.INFORMATION
                    ) }
                    sendTelemetryData()
                }else {
                    createRefactoringObjectsAndShowSuggestions(
                        priority.subList(
                            0,
                            min(SUGGESTIONS4USER, priority.size)
                        )
                    )
                }
            }else{
                telemetryDataManager.addCandidatesTelemetryData(buildCandidatesTelemetryData(0, emptyList()))
                telemetryDataManager.setRefactoringObjects(emptyList())
                log2fileAndViewer("No methods are important to move.", logger)
                invokeLater { showEFNotification(
                    project,
                    LLMBundle.message("notification.extract.function.with.llm.no.suggestions.message"),
                    NotificationType.INFORMATION
                ) }
                sendTelemetryData()
            }
        }
    }

    private fun countPrivateFieldAccesses(method: PsiMethod): Int {
        var count = 0
        method.body?.accept(object : JavaRecursiveElementVisitor() {
            override fun visitReferenceExpression(expression: PsiReferenceExpression) {
                val resolved = expression.resolve()
                if (resolved is PsiField && resolved.hasModifierProperty(PsiModifier.PRIVATE)) {
                    count++
                }
            }
        })
        return count
    }

    private fun createRefactoringObjectsAndShowSuggestions(moveMethodSuggestions: List<MoveMethodSuggestion>) {
            val refObjs = moveMethodSuggestions
                .map {
                    MoveMethodFactory.createMoveMethodFromName(
                        currentEditor,
                        currentFile,
                        it.methodName,
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

    private fun getMethodCompatibility(
        uniqueSuggestions: List<MoveMethodSuggestion>,
        psiClass: PsiClass,
        project: Project
    ): List<MoveMethodSuggestion>? {
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
                val methodText = suggestion.psiMethod.text
//                val cbSimilarity = CodeBertScore.computeCodeBertScore(methodText, classTextWithoutMethod)
                val similarity = PsiUtils.computeCosineSimilarity(methodText, classTextWithoutMethod)
//                val voyageAiEmbeddingModelIT = VoyageAiEmbeddingModelIT()
//                val similarity = voyageAiEmbeddingModelIT.computeVoyageAiCosineSimilarity(methodText, classTextWithoutMethod, VoyageAiEmbeddingModelName.VOYAGE_3_LITE)
                Pair(suggestion, similarity)
            }.sortedBy { it.second }
            //                .map { it.first }

        }

        val top15Suggestions = methodSimilarity.subList(0, min(15, methodSimilarity.size))
        val top15MM = top15Suggestions.map { it.first }
        logMethods(top15MM, -2, 0)
        val messages =
            (prompter as MoveMethodRefactoringPrompt).askForMethodPriorityPrompt(
                condenseMethodCode(functionPsiElement, uniqueSuggestions),
                uniqueSuggestions, top15Suggestions)
//        val messages = (prompter as MoveMethodRefactoringPrompt).askForMethodPriorityPromptWithSimilarity(
//            condenseMethodCode(functionPsiElement, uniqueSuggestions),
//            top15Suggestions)
        val response: LLMBaseResponse?
        val llmResponseTime = measureTimeMillis { response = llmResponseCache[messages.toString()]?:sendChatRequest(project, messages, llmChatModel)}
        if (response != null) {
            var methodPriority = mutableListOf<String>()
            val methodPriority2 = try {
                Gson().fromJson(
                    JsonUtils.sanitizeJson(response.getSuggestions()[0].text),
                    methodPriority::class.java
                )
            } catch (e: Exception) {
                log2fileAndViewer("LLM Response: " + response.getSuggestions()[0].text, logger)
                telemetryDataManager.addLLMPriorityResponse(response.getSuggestions()[0].text, llmResponseTime)
                null
            }
            if (methodPriority2!=null) {
                llmResponseCache.put(messages.toString(), response)
                val sortedSuggestions = top15MM.sortedBy {
                    val index = methodPriority2.indexOf(it.methodName)
                    if (index == -1) {
                        top15MM.size + 1
                    } else {
                        index
                    }
                }
                telemetryDataManager.addLLMPriorityResponse(sortedSuggestions.map{it.methodName}, llmResponseTime)
                return sortedSuggestions
            }
        }
        return null

//        return top15MM
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
                .createComponentPopupBuilder(panel, efPanel.myExtractFunctionsCandidateTable)
                .setRequestFocus(true)
                .setTitle(LLMBundle.message("ef.candidates.popup.title"))
                .setResizable(true)
                .setMovable(true)
                .setCancelOnClickOutside(false)
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
        val psiClass = runReadAction{ functionPsiElement.context as? PsiClass }

        if (psiClass == null) {
            log2fileAndViewer("Error: functionPsiElement is not a PsiClass", logger)
            return emptyList()
        }

        val methodSimilarity = runReadAction {
            uniqueSuggestions.map { suggestion ->
                val method = psiClass.findMethodsByName(suggestion.methodName, false).firstOrNull()
                if (method == null) {
                    log2fileAndViewer("Method ${suggestion.methodName} not found in class", logger)
                    return@map Pair(suggestion, -1.0)
                }

                val methodText = method.text?: ""
                val classTextWithoutMethod = psiClass.methods
                    .filter { it != method }
                    .joinToString("\n") { it.text }

                val similarity = PsiUtils.computeCosineSimilarity(methodText, classTextWithoutMethod)
                Pair(suggestion, similarity)
            }.sortedBy { it.second }
//                .map { it.first }
        }

//        val methodSimilarity = runReadAction {
//            psiClass.methods.map { method ->
//                val methodText = method.body?.text ?: ""
//                val classTextWithoutMethod = psiClass.methods
//                    .filter { it != method }
//                    .joinToString("\n") { it.text }
//
//                val similarity = PsiUtils.computeCosineSimilarity(methodText, classTextWithoutMethod)
////                val similarity = CodeBertScore.computeCodeBertScore(methodText, classTextWithoutMethod)
//                Pair(method, similarity)
//            }.sortedBy { it.second }
//        }

        val messages =
            (prompter as MoveMethodRefactoringPrompt).askForMethodPriorityPrompt(
                condenseMethodCode(functionPsiElement, uniqueSuggestions),
                uniqueSuggestions, methodSimilarity)
        val response: LLMBaseResponse?
        val llmResponseTime = measureTimeMillis { response = llmResponseCache[messages.toString()]?:sendChatRequest(project, messages, llmChatModel)}
        if (response != null) {
            var methodPriority = mutableListOf<String>()
            val methodPriority2 = try {
                Gson().fromJson(
                    JsonUtils.sanitizeJson(response.getSuggestions()[0].text),
                    methodPriority::class.java
                )
            } catch (e: Exception) {
                log2fileAndViewer("LLM Response: " + response.getSuggestions()[0].text, logger)
                telemetryDataManager.addLLMPriorityResponse(response.getSuggestions()[0].text, llmResponseTime)
                null
            }
            if (methodPriority2!=null) {
                llmResponseCache.put(messages.toString(), response)
                val sortedSuggestions = uniqueSuggestions.sortedBy {
                    val index = methodPriority2.indexOf(it.methodName)
                    if (index == -1) {
                        uniqueSuggestions.size + 1
                    } else {
                        index
                    }
                }
                telemetryDataManager.addLLMPriorityResponse(sortedSuggestions.map{it.methodName}, llmResponseTime)
                return sortedSuggestions
            }
        }
        return null
    }

    private fun condenseMethodCode(
        functionPsi: PsiElement,
        uniqueSuggestions: List<MoveMethodSuggestion>
    ): String {
        if (runReadAction{ functionPsi.text }.split("\\s+".toRegex()).size > llmContextLimit) {
            return runReadAction{
                val classPsi = functionPsi as PsiClass
                val onlyMethodNames = uniqueSuggestions.map { it.methodName }
                val classSourceBuilder = StringBuilder()
                classSourceBuilder.append(classPsi.text.substring(0, classPsi.lBrace?.textOffset ?: 0))
                classPsi.methods.filter { method ->
                    onlyMethodNames.contains(method.name)
                }.forEach { method ->
                    classSourceBuilder.append("\n").append(method.text).append("\n")
                }
                classSourceBuilder.append("}")
                return@runReadAction classSourceBuilder.toString()
            }
        }else
            return runReadAction{ functionPsi.text }
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
             index, moveMethodSuggestion ->  log2fileAndViewer(logMessage = "${index+1}. ${moveMethodSuggestion.methodName}", logger = logger)
        }
        log2fileAndViewer(logMessage = "LLM took $llmRequestTime ms to respond", logger = logger)
        telemetryDataManager.addMovesSuggestedInIteration(
            iter,
            moveMethodSuggestions,
            llmRequestTime
        )
    }

    private fun logPriority(moveMethodSuggestions: List<MoveMethodSuggestion>){
        log2fileAndViewer(logMessage = "Priority of methods to move, according to LLM:", logger = logger)
        moveMethodSuggestions.forEachIndexed {
                index, moveMethodSuggestion ->  log2fileAndViewer(logMessage = "${index+1}. ${moveMethodSuggestion.methodName}", logger = logger)
        }
    }


}