package org.boulderse.ijserver.intentions

import com.intellij.codeInsight.intention.IntentionAction
import org.boulderse.ijserver.LLMBundle
import org.boulderse.ijserver.agents.RefactoringAgentLauncher
import org.boulderse.ijserver.server.RefactoringServer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class RefactoringAgentIntention: IntentionAction {

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
            var agentLauncher: RefactoringAgentLauncher? = null
            override fun run(indicator: ProgressIndicator) {
                agentLauncher = RefactoringAgentLauncher(project, editor, file)
                agentLauncher!!.launch()
            }

            override fun onFinished() {

            }
        }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    override fun startInWriteAction(): Boolean = false
    override fun getFamilyName(): String {
        return LLMBundle.message("intentions.apply.suggest.refactoring.agent.family.name")
    }

    override fun getText(): String {
        return "Start refactoring agent."
    }
}
