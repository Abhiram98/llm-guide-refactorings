package org.boulderse.ijserver.intentions

import com.intellij.codeInsight.unwrap.ScopeHighlighter
import org.boulderse.ijserver.LLMBundle
import org.boulderse.ijserver.models.LLMBaseResponse
import org.boulderse.ijserver.models.ollama.localOllamaMistral
import org.boulderse.ijserver.models.sendChatRequest
import org.boulderse.ijserver.refactoringobjects.AbstractRefactoring
import org.boulderse.ijserver.settings.RefAgentSettingsManager
import org.boulderse.ijserver.suggestrefactoring.AbstractRefactoringValidator
import org.boulderse.ijserver.suggestrefactoring.AtomicSuggestion
import org.boulderse.ijserver.suggestrefactoring.SimpleRefactoringValidator
import org.boulderse.ijserver.telemetry.*
import org.boulderse.ijserver.ui.CompletedRefactoringsPanel
import org.boulderse.ijserver.utils.*
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.ui.awt.RelativePoint
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.model.chat.ChatLanguageModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.idea.base.codeInsight.handlers.fixers.endLine
import org.jetbrains.kotlin.idea.base.codeInsight.handlers.fixers.startLine
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import java.awt.Point
import java.awt.Rectangle
import java.util.concurrent.atomic.AtomicReference


@Suppress("UnstableApiUsage")
open class ApplySuggestRefactoringAgentIntention(
    override var llmChatModel: ChatLanguageModel = RefAgentSettingsManager.getInstance().createAndGetAiModel()!!,
    private val useDelays: Boolean = true
) : ApplySuggestRefactoringIntention(llmChatModel) {
    val refactoringLimit: Int = 10
    private var MAX_ITERS: Int = RefAgentSettingsManager.getInstance().getNumberOfIterations()
    private val performedRefactorings = mutableListOf<AbstractRefactoring>()
    private val logger = Logger.getInstance(ApplySuggestRefactoringAgentIntention::class.java)
    private val telemetryIds = mutableListOf<String>()

    override fun getText(): String {
        return LLMBundle.message("intentions.apply.suggest.refactoring.agent.family.name")
    }

    override fun getFamilyName(): String = LLMBundle.message("intentions.apply.suggest.refactoring.agent.family.name")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return editor != null && file != null
    }

    private fun setFunctionSrc(
        editor: Editor,
    ): Pair<Int, String> {
        return runReadAction {
            val startLineNumber = editor.document.getLineNumber(functionPsiElement.startOffset) + 1
            val withLineNumbers = addLineNumbersToCodeSnippet(functionPsiElement.text, startLineNumber)
            functionSrc = withLineNumbers
            Pair(startLineNumber, withLineNumbers)
        }
    }

    override fun invokeLLM(
        project: Project,
        promptIterator: Iterator<MutableList<ChatMessage>>,
        editor: Editor,
        file: PsiFile
    ) {
        llmChatModel = RefAgentSettingsManager.getInstance().createAndGetAiModel()!!
        MAX_ITERS = RefAgentSettingsManager.getInstance().getNumberOfIterations()
        performedRefactorings.removeAll({it->true})
        telemetryIds.clear() // to reset the data in the session.
        for (iter in 1..MAX_ITERS) {
            setTelemetryData(editor, file)
            val now = System.nanoTime()
            log2fileAndViewer(ASSISTANT_HEADER, logger)
            log2fileAndViewer("Asking for refactoring suggestions! ($iter/$MAX_ITERS)", logger)

            if (iter != 1)
                setFunctionSrc(editor)

            val newMessageList = prompter.getPrompt(functionSrc)

            val cacheKey = functionSrc + iter
            val response = llmResponseCache.get(cacheKey) ?: sendChatRequest(
                project, newMessageList, llmChatModel
            )
            if (response != null) {
                llmResponseCache.get(cacheKey) ?: llmResponseCache.put(cacheKey, response)
                llmResponseTime = System.nanoTime() - now
                if (response.getSuggestions().isEmpty()) {
    //                                showEFNotification(
    //                                    project,
    //                                    LLMBundle.message("notification.extract.function.with.llm.no.suggestions.message"),
    //                                    NotificationType.INFORMATION
    //                                )
                } else {
                    runBlocking { processLLMResponse(response, project, editor, file) }
                }
            }
        }

        showExecutedRefactorings(project, editor, file)
    }

    private fun setTelemetryData(editor: Editor, file: PsiFile) {
        telemetryDataManager.newSession()
        telemetryDataManager.addHostFunctionTelemetryData(
            EFTelemetryDataUtils.buildHostFunctionTelemetryData(
                codeSnippet = functionSrc,
                lineStart = functionPsiElement.startLine(editor.document),
                bodyLineStart = functionPsiElement.endLine(editor.document),
                language = file.language.id.toLowerCaseAsciiOnly(),
                filePath = file.virtualFile.path,
                hostClassPsi = functionPsiElement as? PsiClass
            )
        )

        telemetryIds.add(telemetryDataManager.currentSession())

    }

    private fun showExecutedRefactorings(project: Project, editor: Editor, file: PsiFile) {
        invokeLater {
            showCompletedRefactoringOptionsPopup(
                project, editor, file, performedRefactorings, codeTransformer,
            )
        }
    }


    override fun processLLMResponse(response: LLMBaseResponse, project: Project, editor: Editor, file: PsiFile) {

//        delay(3000)
        val now = System.nanoTime()

        val llmResponse = response.getSuggestions()[0]
        val validatorModel = if(RefAgentSettingsManager.getInstance().getUseLocalLLM()) localOllamaMistral else llmChatModel
        val validator = SimpleRefactoringValidator(validatorModel,
            project,
            editor,
            file,
            functionSrc,
            apiResponseCache
        )
        if (useDelays)
            Thread.sleep(2000)
        val filteredSuggestions = filterSuggestions(response, refactoringLimit, validator) ?: return
        log2fileAndViewer(ASSISTANT_HEADER, logger)
        log2fileAndViewer("Processing LLM Recommendations...", logger)
        logger.info("\n")
        val refactoringCandidates: List<AbstractRefactoring> =
            runBlocking {
                validator.buildObjectsFromImprovementsList(filteredSuggestions)
            }
        if (refactoringCandidates.isEmpty()) {
            telemetryDataManager.addCandidatesTelemetryData(buildCandidatesTelemetryData(0, emptyList()))
            telemetryDataManager.setRefactoringObjects(emptyList())
            buildProcessingTimeTelemetryData(llmResponseTime, System.nanoTime() - now)
            sendTelemetryData()
        } else {
            telemetryDataManager.setRefactoringObjects(refactoringCandidates)
            val candidatesApplicationTelemetryObserver = EFCandidatesApplicationTelemetryObserver()
//            val filteredCandidates = filterCandidates(candidates, candidatesApplicationTelemetryObserver, editor, file)
            val validRefactoringCandidates = refactoringCandidates.filter {
                runReadAction{ it.isValid(project, editor, file) }
            }

            telemetryDataManager.addCandidatesTelemetryData(
                buildCandidatesTelemetryData(
                    refactoringCandidates.size,
                    candidatesApplicationTelemetryObserver.getData()
                )
            )
            buildProcessingTimeTelemetryData(llmResponseTime, System.nanoTime() - now)

            logger.info("\n")
            if (validRefactoringCandidates.isEmpty()) {
                log2fileAndViewer("No valid refactoring objects created.", logger)
                sendTelemetryData()
            } else {
                log2fileAndViewer("Performing Refactoring Actions:", logger)
                runBlocking { executeRefactorings(validRefactoringCandidates, project, editor, file) }

            }
        }
    }


    private fun filterSuggestions(response: LLMBaseResponse,
                                  limit: Int,
                                  validator: AbstractRefactoringValidator): List<AtomicSuggestion>? {
        log2fileAndViewer(LLM_HEADER, logger)
        val suggestions = response.getSuggestions()


        if (suggestions.isNotEmpty()){
            val suggestion = suggestions[0]
            val improvements = AbstractRefactoringValidator.getRawSuggestions(suggestion.text)?.improvements?: return emptyList()
            val realLimit = if(improvements.size > limit){
                limit
            } else{
                improvements.size
            }
            val improvementsSorted =
                improvements
                    .sortedBy { selectionPriority(it, validator) }
                    .subList(0, realLimit)
                    .sortedBy { validator.isExtractMethod(it) }

            logLLMResponse(improvementsSorted, useDelays)
            return improvementsSorted
        }
        return null
    }

    private fun showCompletedRefactoringOptionsPopup(
        project: Project,
        editor: Editor,
        file: PsiFile,
        candidates: List<AbstractRefactoring>,
        codeTransformer: CodeTransformer
    ) {
        if (candidates.isEmpty()){
            log2fileAndViewer("No refactorings executed.", logger)
            return
        }
        val highlighter = AtomicReference(ScopeHighlighter(editor))
        val efPanel = CompletedRefactoringsPanel(
            project = project,
            editor = editor,
            file = file,
            candidates = candidates,
            codeTransformer = codeTransformer,
            efTelemetryDataManager = telemetryDataManager
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
                .setTitle(LLMBundle.message("ef.candidates.completed.popup.title"))
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
                elapsedTimeTelemetryDataObserver.buildElapsedTimeTelemetryData(telemetryDataManager)
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


    override fun startInWriteAction(): Boolean = false


    private suspend fun executeRefactorings(
        validRefactoringCandidates: List<AbstractRefactoring>,
        project: Project,
        editor: Editor,
        file: PsiFile
    ) {
        val myHighlighter = AtomicReference(ScopeHighlighter(editor))
        var count = 1
        for (refCandidate in getExecutionOrder(validRefactoringCandidates)){
                val scopeHighlighter: ScopeHighlighter = myHighlighter.get()
                invokeLater {
                    editor.scrollingModel.scrollTo(
                        LogicalPosition(editor.document.getLineNumber(refCandidate.getStartOffset()), 0),
                        ScrollType.CENTER
                    )

                    scopeHighlighter.dropHighlight()
                    val range = TextRange(refCandidate.getStartOffset(), refCandidate.getEndOffset())
                    scopeHighlighter.highlight(com.intellij.openapi.util.Pair(range, listOf(range)))


                    editor.selectionModel.setSelection(refCandidate.getStartOffset(), refCandidate.getStartOffset())
                }
                log2fileAndViewer("$count. Executing refactoring: ${refCandidate.getRefactoringPreview()}".prependIndent("     "), logger)
                if (useDelays)
                    delay(3_000)
                invokeLater {
                    refCandidate.performRefactoring(project, editor, file)
                    scopeHighlighter.dropHighlight()
                }
                performedRefactorings.add(refCandidate)
                if (useDelays)
                    delay(3_000)
                count+=1

        }
    }




    fun sendTelemetryData() {
        val agenticTelemetry = AgenticTelemetry.createFromSessionIds(
            telemetryIds, telemetryDataManager
        )
        TelemetryDataObserver().update(EFNotification(agenticTelemetry))
    }

    companion object {
        private const val ASSISTANT_HEADER = "\n\n************************** Refactoring Assistant **************************"
        private const val LLM_HEADER = "\n\n******************************** LLM ********************************"

    }
}

fun selectionPriority(
    atomicSuggestion: AtomicSuggestion,
    validator: AbstractRefactoringValidator
): Int{
    if (validator.isFor2While(atomicSuggestion) || validator.isEnhacedForRefactoring(atomicSuggestion)){
        return 10;
    }
    if (validator.isExtractMethod(atomicSuggestion))
        return 1;
    return 5;
}