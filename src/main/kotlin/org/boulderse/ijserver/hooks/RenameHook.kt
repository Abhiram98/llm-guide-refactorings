package org.boulderse.ijserver.hooks

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.refactoring.listeners.RefactoringEventData
import com.intellij.refactoring.listeners.RefactoringEventListener
import org.boulderse.ijserver.createNotificationGroup
import org.jetbrains.kotlin.asJava.namedUnwrappedElement

class RenameHook : ProjectActivity {
    var coRenameInProgress = false
    var seedOldName: String? = null
    var seedNewName: String? = null

    fun registerHook(project: Project) {
        project.messageBus.connect().subscribe(
            RefactoringEventListener.REFACTORING_EVENT_TOPIC,

            object : RefactoringEventListener {
                override fun refactoringStarted(refactoringId: String, beforeData: RefactoringEventData?) {
                    print("refactoring started: $refactoringId")
                    super.refactoringStarted(refactoringId, beforeData)
                    if ("rename" in refactoringId && !coRenameInProgress) {
                        print("Found a rename refactoring")
                        val element = beforeData?.getUserData(RefactoringEventData.PSI_ELEMENT_KEY)
                        seedOldName = element?.namedUnwrappedElement?.name
                    }
                }

                override fun refactoringDone(refactoringId: String, afterData: RefactoringEventData?) {
                    print("refactoring done: $refactoringId")
                    if ("rename" in refactoringId && !coRenameInProgress) {
                        print("Found a rename refactoring")
                        val element = afterData?.getUserData(RefactoringEventData.PSI_ELEMENT_KEY)
                        seedNewName = element?.namedUnwrappedElement?.name
                        showNotification(project)
                    }

                }
            },
        )
    }

    override suspend fun execute(project: Project) {
        registerHook(project)
    }

    fun triggerAgent() {
        coRenameInProgress = true
        seedNewName!!
        seedOldName!!
        // todo: trigger python agent.
        //  docker run cuboulderse/renameagent --args
    }

    fun agentComplete() {
        coRenameInProgress = false
    }

    fun showNotification(project: Project) {
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
    }
}
