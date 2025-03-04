package com.intellij.ml.llm.template.intentions

import com.intellij.codeInsight.unwrap.ScopeHighlighter
import com.intellij.ml.llm.template.LLMBundle
import com.intellij.ml.llm.template.models.LLMBaseResponse
import com.intellij.ml.llm.template.models.ollama.localOllamaMistral
import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.settings.RefAgentSettingsManager
import com.intellij.ml.llm.template.showEFNotification
import com.intellij.ml.llm.template.suggestrefactoring.AbstractRefactoringValidator
import com.intellij.ml.llm.template.suggestrefactoring.SimpleRefactoringValidator
import com.intellij.ml.llm.template.telemetry.*
import com.intellij.ml.llm.template.ui.RefactoringSuggestionsPanel
import com.intellij.ml.llm.template.utils.*
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.psi.PsiFile
import com.intellij.ui.awt.RelativePoint
import dev.langchain4j.model.chat.ChatLanguageModel
import kotlinx.coroutines.runBlocking
import java.awt.Point
import java.awt.Rectangle
import java.util.concurrent.atomic.AtomicReference


@Suppress("UnstableApiUsage")
open class ApplySuggestRefactoringInteractiveIntention(
    private var efLLMRequestProvider: ChatLanguageModel = RefAgentSettingsManager.getInstance().createAndGetAiModel()!!,
) : ApplySuggestRefactoringIntention(efLLMRequestProvider) {
    val logger = Logger.getInstance(ApplySuggestRefactoringInteractiveIntention::class.java)

    override fun getFamilyName(): String = LLMBundle.message("intentions.apply.suggest.refactoring.family.name")

    override fun processLLMResponse(response: LLMBaseResponse, project: Project, editor: Editor, file: PsiFile) {
        efLLMRequestProvider = RefAgentSettingsManager.getInstance().createAndGetAiModel()!!
        val now = System.nanoTime()

        val llmResponse = response.getSuggestions()[0]
        println("LLM Response -> ${llmResponse.text}")
        val validatorChatModel =
            if(RefAgentSettingsManager.getInstance().getUseLocalLLM()) localOllamaMistral
            else efLLMRequestProvider

        val validator = SimpleRefactoringValidator(
            validatorChatModel,
            project,
            editor,
            file,
            functionSrc,
            apiResponseCache
        )
        val refactoringCandidates: List<AbstractRefactoring> =
            runBlocking {
                validator.getRefactoringSuggestions(llmResponse.text, MAX_REFACTORINGS)
            }
        if (refactoringCandidates.isEmpty()) {
            showEFNotification(
                project,
                LLMBundle.message("notification.extract.function.with.llm.no.suggestions.message"),
                NotificationType.INFORMATION
            )
            telemetryDataManager.addCandidatesTelemetryData(buildCandidatesTelemetryData(0, emptyList()))
            buildProcessingTimeTelemetryData(llmResponseTime, System.nanoTime() - now)
            sendTelemetryData()
        } else {
            val rawSuggestions = AbstractRefactoringValidator.getRawSuggestions(llmResponse.text)
            if (rawSuggestions!=null) {
                logLLMResponse(
                    rawSuggestions.improvements,
                    false
                )
                telemetryDataManager.setRefactoringObjects(refactoringCandidates)
                val candidatesApplicationTelemetryObserver = EFCandidatesApplicationTelemetryObserver()
//            val filteredCandidates = filterCandidates(candidates, candidatesApplicationTelemetryObserver, editor, file)
                val validRefactoringCandidates = refactoringCandidates.filter {
                    it.isValid(project, editor, file)
                }

                telemetryDataManager.addCandidatesTelemetryData(
                    buildCandidatesTelemetryData(
                        refactoringCandidates.size,
                        candidatesApplicationTelemetryObserver.getData()
                    )
                )
                buildProcessingTimeTelemetryData(llmResponseTime, System.nanoTime() - now)

                if (validRefactoringCandidates.isEmpty()) {
                    showEFNotification(
                        project,
                        LLMBundle.message("notification.extract.function.with.llm.no.extractable.candidates.message"),
                        NotificationType.INFORMATION
                    )
                    sendTelemetryData()
                } else {
//                refactoringObjectsCache.get(functionSrc)?:refactoringObjectsCache.put(functionSrc, validRefactoringCandidates)
                    showRefactoringOptionsPopup(
                        project, editor, file, validRefactoringCandidates, codeTransformer,
                    )
                }
            }
        }
    }

    private fun showRefactoringOptionsPopup(
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
    override fun getText(): String {
        return LLMBundle.message("intentions.apply.suggest.refactoring.family.name")
    }

}