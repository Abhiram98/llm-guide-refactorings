package org.boulderse.ijserver.hooks

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.psi.PsiElement
import com.intellij.refactoring.listeners.RefactoringEventData
import com.intellij.refactoring.listeners.RefactoringEventListener
import org.boulderse.ijserver.createNotificationGroup
import org.boulderse.ijserver.settings.RefAgentSettingsManager
import org.boulderse.ijserver.showUnauthorizedNotification
import org.boulderse.ijserver.toolwindow.logViewer
import org.boulderse.ijserver.utils.PsiUtils
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.idea.base.psi.getLineNumber

class RenameHook : ProjectActivity {
    var coRenameInProgress = false
    var seedOldName: String? = null
    var seedNewName: String? = null
    var seedElement: PsiElement? = null

    fun registerHook(project: Project) {
        project.messageBus.connect().subscribe(
            RefactoringEventListener.REFACTORING_EVENT_TOPIC,
            object : RefactoringEventListener {
                override fun refactoringStarted(
                    refactoringId: String,
                    beforeData: RefactoringEventData?,
                ) {
                    print("refactoring started: $refactoringId")
                    super.refactoringStarted(refactoringId, beforeData)
                    if ("rename" in refactoringId && !coRenameInProgress) {
                        print("Found a rename refactoring")
                        val element = beforeData?.getUserData(RefactoringEventData.PSI_ELEMENT_KEY)
                        seedElement = element
                        seedOldName = element?.namedUnwrappedElement?.name
                    }
                }

                override fun refactoringDone(
                    refactoringId: String,
                    afterData: RefactoringEventData?,
                ) {
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

    fun triggerAgent(project: Project) {
        coRenameInProgress = true

        val llmKey = RefAgentSettingsManager.getInstance().getOpenAiKey()
        if (llmKey == "") {
            showUnauthorizedNotification(project)
            return
        }

        val command =
            mutableListOf(
                "docker",
                "run",
                "-d",
                "-e",
                "IJ_SERVER_URL=http://host.docker.internal:8082",
                "-e",
                "GRAZIE_JWT_TOKEN",
                "renameagent",
                "--seed_old_name",
                seedOldName!!,
                "--seed_new_name",
                seedNewName!!,
                "--seed_line_num",
                seedElement?.getLineNumber()?.plus(1)?.toString()?:"unknown",
                "--seed_element_type",
                PsiUtils.getElementTypeStr(seedElement!!),
                "--seed_file",
                seedElement
                    ?.containingFile
                    ?.virtualFile
                    ?.path
                    ?.removePrefix(project.basePath + "/")!!,
            )
        println("Running command: ${command.joinToString(" ")}")
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val cmd =
                    ProcessBuilder(command)
                cmd.environment()["GRAZIE_JWT_TOKEN"] = llmKey
                val process = cmd.start()
                process.waitFor()
                val containerId =
                    process.inputStream
                        .bufferedReader()
                        .use { it.readText() }
                        .removeSuffix("\n")
                println("Containerid=$containerId")

                logViewer.registerAgentContainerId(containerId)
                val waitProcess = ProcessBuilder(listOf("docker", "container", "wait", containerId)).start()
                val exitCode = waitProcess.waitFor()
                val output = waitProcess.inputStream.bufferedReader().use { it.readText() }
                val stderr = waitProcess.errorStream.bufferedReader().use { it.readText() }
                println("refagent exit=$exitCode output:\n$output\nstderr:\n$stderr")
            } catch (e: Exception) {
                println("Failed to run refagent: ${e.message}")
            } finally {
                agentComplete()
            }
        }
    }

    fun agentComplete() {
        logViewer.resetViewer()
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
                triggerAgent(project)
                notification.expire()
            },
        )
        notification.notify(project)
    }
}
