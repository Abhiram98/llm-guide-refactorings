package com.intellij.ml.llm.template.intentions

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonParser
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
import com.intellij.ml.llm.template.utils.PsiUtils.Companion.computeCosineSimilarity
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
import dev.langchain4j.model.voyageai.VoyageAiEmbeddingModelName
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
        getVanillaLlmSuggestions(project, promptIterator) // get vanilla llm suggestions, just for ablation.

        val allMethodsInClass: List<PsiMethod> = runReadAction { PsiUtils.getAllMethodsInClass(functionPsiElement as PsiClass) }
        val allClassNames = listOf((functionPsiElement as PsiClass).name!!)+ (functionPsiElement as PsiClass).allInnerClasses.map { it.name }.filterNotNull()
        val bruteForceSuggestions = runReadAction {
            allMethodsInClass
                .filter { it.name !in allClassNames } // filter constructors
                .filter { !isGetter(it) } // filter out getters and setters.
                .filter { !isSetter(it) } // filter out getters and setters.
                .filter { !it.text.contains("@Override") } // remove methods in an inheritance chain
                .filter { !it.text.contains("@Test") } // remove test methods.
                .filter { !it.hasModifier(JvmModifier.ABSTRACT) } // filter out abstract methods because they have no body.
                .filter { hasSomeEnvy(it) }
                .map { MoveMethodSuggestion(it.name, getSignatureString(it), "", "", it) }
        }
        val methodCompatibilityTfIdf = getMethodCompatibility(
            bruteForceSuggestions, functionPsiElement as PsiClass, allMethodsInClass, "tf-idf")
        val methodCompatibilityVoyage = getMethodCompatibility(
            bruteForceSuggestions, functionPsiElement as PsiClass, allMethodsInClass, "voyage")
        val tfidfSuggestions = methodCompatibilityTfIdf.map { it.first }
        val voyageSuggestions = methodCompatibilityVoyage.map { it.first }
        val allSuggestions = mutableListOf<MoveMethodSuggestion>()
        allSuggestions.addAll(tfidfSuggestions)
        val allSuggestionPsiMethods = allSuggestions.map { it.psiMethod }
        voyageSuggestions.forEach {
            if (it.psiMethod !in allSuggestionPsiMethods)
                allSuggestions.add(it)
        }

        addMethodCompatibilityData(methodCompatibilityTfIdf, "tf-idf")
        addMethodCompatibilityData(methodCompatibilityVoyage, "voyage")


        logMethods(bruteForceSuggestions, -1, 0)
        logMethods(allSuggestions, -2, 0)
        log2fileAndViewer("*** Combining responses from iterations ***", logger)
        if (allSuggestions.isEmpty()) {
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
            log2fileAndViewer("Prioritising suggestions...", logger)
            getPriorities(tfidfSuggestions, voyageSuggestions) // only for ablation purposes, ask LLM to prioritise.
            val priority = allSuggestions
            if (priority != null) {
                logPriority(priority)
                if (priority.size == 0) {
                    telemetryDataManager.addCandidatesTelemetryData(buildCandidatesTelemetryData(0, emptyList()))
                    telemetryDataManager.setRefactoringObjects(emptyList())
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
                        priority.subList(
                            0,
                            min(SUGGESTIONS4USER, priority.size)
                        )
                    )
                }
            } else {
                telemetryDataManager.addCandidatesTelemetryData(buildCandidatesTelemetryData(0, emptyList()))
                telemetryDataManager.setRefactoringObjects(emptyList())
                log2fileAndViewer("No methods are important to move.", logger)
                invokeLater {
                    showEFNotification(
                        project,
                        LLMBundle.message("notification.extract.function.with.llm.no.suggestions.message"),
                        NotificationType.INFORMATION
                    )
                }
                sendTelemetryData()
            }
        }

    }

    private fun getPriorities(
        tfidfSuggestions: List<MoveMethodSuggestion>,
        voyageSuggestions: List<MoveMethodSuggestion>
    ) {
        val tfIdfPriorityAll = getSuggestionPriority(tfidfSuggestions, currentProject, "tf-idf")
        val tfIdfPriorityTop5 = getSuggestionPriority(tfidfSuggestions, currentProject, "tf-idf-5", 5)
        val tfIdfPriorityTop3 = getSuggestionPriority(tfidfSuggestions, currentProject, "tf-df-3", 3)
        val voyageIdfPriorityAll = getSuggestionPriority(voyageSuggestions, currentProject, "voyage")
        val voyageIdfPriorityTop5 = getSuggestionPriority(voyageSuggestions, currentProject, "voyage-5", 5)
        val voyageIdfPriorityTop3 = getSuggestionPriority(voyageSuggestions, currentProject, "voyage-3", 3)

        if (tfIdfPriorityAll != null) {
            logPriority(tfIdfPriorityAll)
        }
        if (tfIdfPriorityTop5 != null) {
            logPriority(tfIdfPriorityTop5)
        }
        if (tfIdfPriorityTop3 != null) {
            logPriority(tfIdfPriorityTop3)
        }
        if (voyageIdfPriorityAll != null) {
            logPriority(voyageIdfPriorityAll)
        }
        if (voyageIdfPriorityTop5 != null) {
            logPriority(voyageIdfPriorityTop5)
        }
        if (voyageIdfPriorityTop3 != null) {
            logPriority(voyageIdfPriorityTop3)
        }
    }

    private fun hasSomeEnvy(psiMethod: PsiMethod): Boolean{
        val references = PsiUtils.getAllReferenceExpressions(psiMethod)
        // foo.func()
        val envyReferences = mutableListOf<PsiReferenceExpression>()
        for (reference in references){
            if (reference.children.isEmpty() || reference.children[0] !is PsiReferenceExpression)
                continue
            val resolvedReference = (reference.children[0] as PsiReferenceExpression).reference?.resolve()
            val filterCondition = if (resolvedReference is PsiField){
                PsiUtils.isInProject(resolvedReference.type.canonicalText, psiMethod.project) ||
                        PsiUtils.isInProject(resolvedReference.type.canonicalText.split("<")[0], psiMethod.project)
            }else if (resolvedReference is PsiParameter){
                PsiUtils.isInProject(resolvedReference.type.canonicalText, psiMethod.project)
                        || PsiUtils.isInProject(resolvedReference.type.canonicalText.split("<")[0], psiMethod.project)
            } else {
                false
            }
            if (filterCondition)
                envyReferences.add(reference)
        }
        return envyReferences.isNotEmpty()
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
        // Create the popup

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
        efPopup.show(RelativePoint.getNorthEastOf(contentComponent))
    }

    private fun getSuggestionPriority(
        uniqueSuggestions: List<MoveMethodSuggestion>,
        project: Project,
        similarityType: String,
        limit: Int =-1,
    ) : List<MoveMethodSuggestion>? {
        val psiClass = runReadAction{ functionPsiElement as? PsiClass }
        if (psiClass == null) {
            log2fileAndViewer("Error: functionPsiElement is not a PsiClass", logger)
            return emptyList()
        }
        val messages =
            (prompter as MoveMethodRefactoringPrompt).askForMethodPriorityPrompt(
                condenseMethodCode(functionPsiElement, uniqueSuggestions),
                if (limit==-1) uniqueSuggestions else uniqueSuggestions.subList(0, min(uniqueSuggestions.size, limit))
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
                telemetryDataManager.addLLMPriorityResponse(response.getSuggestions()[0].text, llmResponseTime, similarityType)
                null
            }
            if (methodPrioritySignatures!=null) {
                llmResponseCache.put(messages.toString(), response)
                val filteredSuggestions = uniqueSuggestions.filter { it.methodSignature in methodPrioritySignatures }
                val sortedSuggestions = filteredSuggestions.sortedBy {
                    val index = methodPrioritySignatures.indexOf(it.methodSignature)
                    if (index == -1) {
                        uniqueSuggestions.size + 1
                    } else {
                        index
                    }
                }
                telemetryDataManager.addLLMPriorityResponse(sortedSuggestions.map{it.methodSignature}, llmResponseTime, similarityType)
                return sortedSuggestions
            }
        }
        return null
    }

    private fun getMethodCompatibility(
        uniqueSuggestions: List<MoveMethodSuggestion>,
        psiClass: PsiClass,
        allMethodsInClass: List<PsiMethod>,
        compatibilityType: String = "tf-idf"
    ): List<Pair<MoveMethodSuggestion, Double>> {
        val methodSimilarity = runReadAction {
            uniqueSuggestions.map { suggestion ->
                if (suggestion.psiMethod == null) {
                    log2fileAndViewer("Method ${suggestion.methodName} not found in class", logger)
                    return@map Pair(suggestion, -1.0)
                }
                val otherMethods = allMethodsInClass.filter { it.name == suggestion.methodName && it != suggestion.psiMethod}
                var classTextWithoutMethod = getClassWithoutMethod(psiClass.text, suggestion.psiMethod)
                for (method in otherMethods){
                    classTextWithoutMethod = getClassWithoutMethod(classTextWithoutMethod, method)
                }

                val similarity = if (compatibilityType=="tf-idf") {

                    computeCosineSimilarity(suggestion.psiMethod.text, classTextWithoutMethod)
                }
                else{
                    VoyageAiEmbeddingModelIT().computeVoyageAiCosineSimilarity(suggestion.psiMethod.text, classTextWithoutMethod, VoyageAiEmbeddingModelName.VOYAGE_3_LITE)
                }
                Pair(suggestion, similarity)
            }.sortedBy { it.second }
    //                .map { it.first }
        }

        val top15SuggestionsWithScore = methodSimilarity.subList(0, min(15, methodSimilarity.size))
//        val top15MM = top15Suggestions.map { it.first }
//        logMethods(top15MM, -2, 0)
        return top15SuggestionsWithScore
    }

    private fun getClassWithoutMethod(
        psiClassText: String,
        psiMethod: PsiMethod
    ): String {
        val methodIndex = psiClassText.indexOf(psiMethod.text)
        val classTextWithoutMethod = if (methodIndex == -1) {
            psiClassText
        } else {

            psiClassText.substring(
                0,
                methodIndex
            ) + psiClassText.substring(methodIndex + psiMethod.text.length, psiClassText.length)
        }
        return classTextWithoutMethod
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
             index, moveMethodSuggestion ->  log2fileAndViewer(logMessage = "${index+1}. ${moveMethodSuggestion.methodSignature}", logger = logger)
        }
        log2fileAndViewer(logMessage = "LLM took $llmRequestTime ms to respond", logger = logger)
        telemetryDataManager.addMovesSuggestedInIteration(
            iter,
            moveMethodSuggestions,
            llmRequestTime
        )
    }

    private fun addMethodCompatibilityData(methodCompatibilitySuggestions: List<Pair<MoveMethodSuggestion, Double>>,
                                           similarityType: String
                                           ) {
        telemetryDataManager.addMethodCompatibility(
            methodCompatibilitySuggestions, similarityType
        )

    }

    private fun logPriority(moveMethodSuggestions: List<MoveMethodSuggestion>){
        log2fileAndViewer(logMessage = "Priority of methods to move, according to LLM:", logger = logger)
        moveMethodSuggestions.forEachIndexed {
                index, moveMethodSuggestion ->  log2fileAndViewer(logMessage = "${index+1}. ${moveMethodSuggestion.methodSignature}", logger = logger)
        }
    }
    
    private fun getVanillaLlmSuggestions(project: Project, promptIterator: Iterator<MutableList<ChatMessage>>) {
        val vanillaLLMSuggestions = mutableListOf<MoveMethodSuggestion>()

        for (iter in 1..MAX_ITERS) {
            log2fileAndViewer("******** ITERATION-$iter ********", logger)
            val cacheKey = functionSrc + iter.toString()
            val response: LLMBaseResponse?
            val llmRequestTime = measureTimeMillis {
                response = llmResponseCache[cacheKey] ?: sendChatRequest(project, promptIterator.next(), llmChatModel)
            }

            if (response != null) {
                val llmText = response.getSuggestions()[0]
                val processed = JsonUtils.sanitizeJson(llmText.text)
                val refactoringSuggestions = try {
                    (JsonParser.parseString(processed) as JsonArray)
                        .map {
                            try {
                                Gson().fromJson(it, MoveMethodSuggestion::class.java)
                            } catch (e: Exception) {
                                print("failed to decode json ->$it")
                                null
                            }
                        }.filterNotNull()
                } catch (e: Exception) {
                    print("Failed to parse ${processed}")
                    log2fileAndViewer("LLM response: ${processed}", logger)
                    e.printStackTrace()
                    logMethods(listOf(), iter, llmRequestTime)
                    null
                }
                if (refactoringSuggestions != null) {
                    llmResponseCache[cacheKey] ?: llmResponseCache.put(cacheKey, response) // cache response
                    vanillaLLMSuggestions.addAll(refactoringSuggestions)
                    logMethods(refactoringSuggestions, iter, llmRequestTime)
                }
            }
        }
    }
            


}