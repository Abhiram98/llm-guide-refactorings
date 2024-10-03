package com.intellij.ml.llm.template.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.ml.llm.template.LLMBundle
import com.intellij.ml.llm.template.models.LLMBaseResponse
import com.intellij.ml.llm.template.models.sendChatRequest
import com.intellij.ml.llm.template.prompts.MethodPromptBase
import com.intellij.ml.llm.template.prompts.SuggestRefactoringPrompt
import com.intellij.ml.llm.template.settings.RefAgentSettingsManager
import com.intellij.ml.llm.template.showEFNotification
import com.intellij.ml.llm.template.suggestrefactoring.AtomicSuggestion
import com.intellij.ml.llm.template.telemetry.*
import com.intellij.ml.llm.template.toolwindow.logViewer
import com.intellij.ml.llm.template.utils.*
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.PsiJavaFileImpl
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.model.chat.ChatLanguageModel
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import java.util.concurrent.TimeUnit

@Suppress("UnstableApiUsage")
abstract class ApplySuggestRefactoringIntention(
    open var llmChatModel: ChatLanguageModel = RefAgentSettingsManager.getInstance().createAndGetAiModel()!!,
) : IntentionAction {
    private val logger = Logger.getInstance("#com.intellij.ml.llm")
    val codeTransformer = CodeTransformer()
    val telemetryDataManager = EFTelemetryDataManager()
    var llmResponseTime = 0L
    var functionSrc = ""
    lateinit var functionPsiElement: PsiElement
    var llmResponseCache = mutableMapOf<String, LLMBaseResponse>()
    var apiResponseCache = mutableMapOf<String, MutableMap<String, LLMBaseResponse>>()
    val MAX_REFACTORINGS = 10
    var finishedBackgroundTask: Boolean? = null
    protected val llmContextLimit = 128000

    open var prompter: MethodPromptBase = SuggestRefactoringPrompt();

    init {
        codeTransformer.addObserver(EFLoggerObserver(logger))
        codeTransformer.addObserver(TelemetryDataObserver())
    }

//    override fun getFamilyName(): String = LLMBundle.message("intentions.apply.suggest.refactoring.family.name")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return editor != null && file != null
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        invokePlugin(project, editor, file)
    }

    protected fun invokePlugin(
        project: Project,
        editor: Editor?,
        file: PsiFile?
    ) {
        llmChatModel = RefAgentSettingsManager.getInstance().createAndGetAiModel()!!

        if (editor == null || file == null) return
        val selectionModel = editor.selectionModel
        //        val namedElement =
        //            PsiUtils.getParentFunctionOrNull(editor, file.language)
        //                ?: PsiUtils.getParentClassOrNull(editor, file.language)
        val namedElement = (file as PsiJavaFileImpl).classes[0]
        if (namedElement != null) {

            telemetryDataManager.newSession()
            val codeSnippet = namedElement.text

            val textRange = namedElement.textRange
            selectionModel.setSelection(textRange.startOffset, textRange.endOffset)
            val startLineNumber = editor.document.getLineNumber(selectionModel.selectionStart) + 1
            val withLineNumbers = addLineNumbersToCodeSnippet(codeSnippet, startLineNumber)
            functionSrc = withLineNumbers
            functionPsiElement = namedElement

            val bodyLineStart = when (namedElement) {
                is PsiClass -> PsiUtils.getClassBodyStartLine(namedElement)
                else -> PsiUtils.getFunctionBodyStartLine(namedElement)
            }
            telemetryDataManager.addHostFunctionTelemetryData(
                EFTelemetryDataUtils.buildHostFunctionTelemetryData(
                    codeSnippet = codeSnippet,
                    lineStart = startLineNumber,
                    bodyLineStart = bodyLineStart,
                    language = file.language.id.toLowerCaseAsciiOnly(),
                    filePath = file.virtualFile.path
                )
            )

            getPromptAndRunBackgroundable(withLineNumbers, project, editor, file)
        }
    }

    private fun isInputSizeWithinLimit(text: String): Boolean {
        logger.warn("Input size is ${text.length / 4} characters.")
        val inputTextSize = text.length / 4
        return inputTextSize <= llmContextLimit
    }

    //    abstract fun invokeLlm(text: String, project: Project, editor: Editor, file: PsiFile)
fun getPromptAndRunBackgroundable(text: String, project: Project, editor: Editor, file: PsiFile) {
        logger.info("Invoking LLM with text: $text")
        val promptIterator = createPromptIterator(project, text)
//        val promptText = prompter.getPrompt(text)
        val task = object : Task.Backgroundable(
            project, LLMBundle.message("intentions.request.extract.function.background.process.title")
        ) {
            override fun run(indicator: ProgressIndicator) {
                finishedBackgroundTask = false
                invokeLLM(project, promptIterator, editor, file)
            }

            override fun onFinished() {
                finishedBackgroundTask = true
                super.onFinished()
            }
        }
        finishedBackgroundTask = false
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    private fun createPromptIterator(
        project: Project,
        text: String,
    ): Iterator<MutableList<ChatMessage>> {
        val promptIterator: Iterator<MutableList<ChatMessage>>

        if (!isInputSizeWithinLimit(text)) {
            // Notify the user that the input exceeds the context limit
            invokeLater{
                showEFNotification(
                    project,
                    LLMBundle.message("notification.extract.function.with.llm.exceeds.context.limit.message"),
                    NotificationType.WARNING
                )
            }
            logger.warn("Input size exceeds the LLM context limit of $llmContextLimit characters.")
        }

        promptIterator = sequence {
            while (true)
                yield(prompter.getPrompt(text))
        }.iterator()

        return promptIterator
    }

    open fun invokeLLM(
        project: Project,
        promptIterator: Iterator<MutableList<ChatMessage>>,
        editor: Editor,
        file: PsiFile
    ) {
        val now = System.nanoTime()
        val response = llmResponseCache.get(functionSrc) ?: sendChatRequest(
            project, promptIterator.next(), llmChatModel
        )
        if (response != null) {
            llmResponseCache.get(functionSrc) ?: llmResponseCache.put(functionSrc, response)
            invokeLater {
                llmResponseTime = System.nanoTime() - now
                if (response.getSuggestions().isEmpty()) {
                    showEFNotification(
                        project,
                        LLMBundle.message("notification.extract.function.with.llm.no.suggestions.message"),
                        NotificationType.INFORMATION
                    )
                } else {
                    processLLMResponse(response, project, editor, file)
                }
            }
        }
    }


    abstract fun processLLMResponse(response: LLMBaseResponse, project: Project, editor: Editor, file: PsiFile)

    override fun startInWriteAction(): Boolean = false


    open fun sendTelemetryData() {
        val efTelemetryData = telemetryDataManager.getData()
        if (efTelemetryData != null) {
            TelemetryDataObserver().update(EFNotification(efTelemetryData))
        }
    }

    fun buildProcessingTimeTelemetryData(llmResponseTime: Long, pluginProcessingTime: Long) {
        val llmResponseTimeMillis = TimeUnit.NANOSECONDS.toMillis(llmResponseTime)
        val pluginProcessingTimeMillis = TimeUnit.NANOSECONDS.toMillis(pluginProcessingTime)
        val efTelemetryData = telemetryDataManager.getData()
        if (efTelemetryData != null) {
            efTelemetryData.processingTime = EFTelemetryDataProcessingTime(

                llmResponseTime = llmResponseTimeMillis,
                pluginProcessingTime = pluginProcessingTimeMillis,
                totalTime = llmResponseTimeMillis + pluginProcessingTimeMillis
            )
        }
    }

    fun buildCandidatesTelemetryData( //TODO: this function should include telemetry information
                                        // generic to all refactoring objects
        numberOfSuggestions: Int, notificationPayloadList: List<EFCandidateApplicationPayload>
    ): RefCandidatesTelemetryData {
        val candidateTelemetryDataList = EFTelemetryDataUtils.buildCandidateTelemetryData(notificationPayloadList)
        return RefCandidatesTelemetryData(
            numberOfSuggestions = numberOfSuggestions, candidates = candidateTelemetryDataList
        )
    }

    fun buildElapsedTimeTelemetryData(elapsedTimeTelemetryDataObserver: TelemetryElapsedTimeObserver) {
        val elapsedTimeTelemetryData = elapsedTimeTelemetryDataObserver.getTelemetryData()
        val efTelemetryData = telemetryDataManager.getData()
        if (efTelemetryData != null) {
            efTelemetryData.elapsedTime = elapsedTimeTelemetryData
        }
    }

    protected fun logLLMResponse(improvementsList: List<AtomicSuggestion>, useDelays: Boolean) {
        for (atomicSuggestion in improvementsList.withIndex()) {
            log2fileAndViewer("${atomicSuggestion.index + 1}: ${atomicSuggestion.value.shortDescription}", logger)
            log2fileAndViewer("Suggestion: ${atomicSuggestion.value.longDescription}".prependIndent("    "), logger)
            logger.info("\n")
            if (useDelays)
                Thread.sleep(3000)
        }
    }

    companion object {
        @JvmStatic
        fun log2fileAndViewer(logMessage: String, logger: Logger){
            logViewer.appendLog(logMessage)
            logger.info(logMessage)
        }
    }
}