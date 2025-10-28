package org.boulderse.ijserver.hooks

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.psi.PsiElement
import com.intellij.refactoring.listeners.RefactoringEventData
import com.intellij.refactoring.listeners.RefactoringEventListener
import org.boulderse.ijserver.LLMBundle
import org.boulderse.ijserver.createNotificationGroup
import org.boulderse.ijserver.settings.openSettingsDialog

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
                    val notification =
                        createNotificationGroup().createNotification(
                            "Trigger coRenameAgent",
                            "You triggered a rename. Do you want to trigger the CoRenameAgent.",
                            NotificationType.INFORMATION,
                        )

                    val action = "Trigger agent"
                    notification.addAction(
                        NotificationAction.createSimple(action) {
                            triggerAgent()
                            notification.expire()
                        },
                    )
                    notification.notify(project)
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
