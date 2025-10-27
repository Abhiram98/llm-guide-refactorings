package org.boulderse.ijserver.hooks

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.psi.PsiElement
import com.intellij.refactoring.listeners.RefactoringEventData
import com.intellij.refactoring.listeners.RefactoringEventListener

class RenameHook : ProjectActivity {
    var coRenameInProgress = false

    fun registerHook(project: Project) {
        project.messageBus.connect().subscribe(
            RefactoringEventListener.REFACTORING_EVENT_TOPIC,
            RefactoringEventListener { refactoringId, afterData ->
                print("refactoring done: $refactoringId")
                if ("rename" in refactoringId && !coRenameInProgress) {
                    print("Found a rename refactoring")
                    print(afterData)
                    // todo: send a notification asking the
                    //  developer if they want to trigger agent
                }
            },
        )
    }

    override suspend fun execute(project: Project) {
        registerHook(project)
    }

    fun triggerAgent() {
        coRenameInProgress = true
        // todo: trigger python agent.
    }
}
