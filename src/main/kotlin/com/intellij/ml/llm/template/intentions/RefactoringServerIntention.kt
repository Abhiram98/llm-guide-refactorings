package com.intellij.ml.llm.template.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.ml.llm.template.LLMBundle
import com.intellij.ml.llm.template.models.LLMBaseResponse
import com.intellij.ml.llm.template.models.sendChatRequest
import com.intellij.ml.llm.template.prompts.MethodPromptBase
import com.intellij.ml.llm.template.prompts.SuggestRefactoringPrompt
import com.intellij.ml.llm.template.server.RefactoringServer
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

class RefactoringServerIntention: IntentionAction {

//    override fun getFamilyName(): String = LLMBundle.message("intentions.apply.suggest.refactoring.family.name")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return editor != null && file != null
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        invokePlugin(project, editor!!, file!!)
    }

    protected fun invokePlugin(
        project: Project,
        editor: Editor,
        file: PsiFile
    ) {

        val task = object : Task.Backgroundable(
            project, LLMBundle.message("intentions.refactoring.server.background.process.title")
        ) {
            var server: RefactoringServer? = null
            override fun run(indicator: ProgressIndicator) {
                server = RefactoringServer(project, editor, file)
                server?.start()
            }

            override fun onFinished() {
                server?.stop()
            }
        }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    override fun startInWriteAction(): Boolean = false
    override fun getFamilyName(): String {
        return LLMBundle.message("intentions.apply.suggest.refactoring.agent.family.name")
    }

    override fun getText(): String {
        return "Start refactoring server."
    }
}
