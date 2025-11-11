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
import org.boulderse.ijserver.telemetry.RenameAgentTelemetryManager
import org.boulderse.ijserver.toolwindow.logViewer
import org.boulderse.ijserver.utils.DockerManager
import org.boulderse.ijserver.utils.PsiUtils
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.idea.base.psi.getLineNumber

class RenameHook : ProjectActivity {
    var coRenameInProgress = false
    var seedOldName: String? = null
    var seedNewName: String? = null
    var seedElement: PsiElement? = null
    val dockerManager = DockerManager.getInstance()

    val telemetryManager = RenameAgentTelemetryManager.getInstance()

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
                        seedElement = element
                        showNotification(
                            project,
                            "Trigger coRenameAgent",
                            "You triggered a rename. Do you want to trigger the CoRenameAgent?",
                            "Trigger agent",
                        )
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
        logViewer.noOpReview()
        if (dockerManager.testDockerPath() == false) {
            showNotification(
                project,
                "Docker Error",
                "Could not find docker installed on machine. Please install docker and retry",
                "Retry CoRename...",
            )
        } else if (dockerManager.checkDockerDaemon() == false) {
            showNotification(
                project,
                "Docker Error",
                "Could not connect to docker daemon. Please check that docker daemon is running and retry",
                "Retry CoRename...",
            )
        }

        telemetryManager.startNewSession()
        if (seedElement!=null)
            PsiUtils.getElementTypeStr(seedElement!!)?.let {  telemetryManager.setSeedType(it) }

        val llmKey = RefAgentSettingsManager.getInstance().getOpenAiKey()
        if (llmKey == "") {
            showUnauthorizedNotification(project)
            return
        }

        val command =
            mutableListOf(
                dockerManager.dockerPath,
                "run",
                "-d",
                "-e",
                "IJ_SERVER_URL=http://host.docker.internal:8082",
                "-e",
                "LLM_TOKEN",
                "bellurabhiram/renameagent",
                "--vendor",
                RefAgentSettingsManager.getInstance().getAiModelVendor(),
                "--seed_old_name",
                seedOldName!!,
                "--seed_new_name",
                seedNewName!!,
                "--seed_line_num",
                seedElement?.getLineNumber()?.plus(1)?.toString() ?: "unknown",
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
        val dockerEnvOverrides = dockerManager.prepareCredentialHelperWorkaround()
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                pullDockerImage(project)

                val cmd =
                    ProcessBuilder(command)
                dockerManager.applyEnvironmentOverrides(cmd, dockerEnvOverrides)
                cmd.environment()["LLM_TOKEN"] = llmKey
                val process = cmd.start()
                val exitCode = process.waitFor()
                val stdout =
                    process.inputStream
                        .bufferedReader()
                        .use { it.readText() }
                        .trim()
                val stderr =
                    process.errorStream
                        .bufferedReader()
                        .use { it.readText() }

                if (exitCode != 0 || stdout.isBlank()) {
                    println(
                        "Failed to start rename agent container (exit=$exitCode). " +
                            "stdout:\n$stdout\nstderr:\n$stderr",
                    )
                    return@executeOnPooledThread
                }

                val containerId = stdout
                println("Containerid=$containerId")

                logViewer.registerAgentContainerId(containerId)
                val waitCommand = listOf(dockerManager.dockerPath, "container", "wait", containerId)
                val waitProcessBuilder = ProcessBuilder(waitCommand)
                dockerManager.applyEnvironmentOverrides(waitProcessBuilder, dockerEnvOverrides)
                val waitProcess = waitProcessBuilder.start()
                val waitExitCode = waitProcess.waitFor()
                val output = waitProcess.inputStream.bufferedReader().use { it.readText() }
                val waitStderr = waitProcess.errorStream.bufferedReader().use { it.readText() }
                println("refagent exit=$waitExitCode output:\n$output\nstderr:\n$waitStderr")

                fetchContainerLogs(containerId, dockerEnvOverrides)

            } catch (e: Exception) {
                println("Failed to run refagent: ${e.message}")
            } finally {
                agentComplete()
            }
        }
    }

    private fun fetchContainerLogs(containerId: String, dockerEnvOverrides: Map<String, String>) {
        try{
            val logsCommand = listOf(dockerManager.dockerPath, "logs", containerId)
            val logsProcessBuilder = ProcessBuilder(logsCommand)
            dockerManager.applyEnvironmentOverrides(logsProcessBuilder, dockerEnvOverrides)
            val process = logsProcessBuilder.start()
            val exitCode = process.waitFor()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val stderr = process.errorStream.bufferedReader().use { it.readText() }
            println("container logs exit=$exitCode output:\n$output\nstderr:\n$stderr")
        }catch (e:Exception){
            println("Failed to fetch container logs: ${e.message}")
        }
    }

    private fun pullDockerImage(project: Project) {
        logViewer.noOpReview("Pulling docker image.")
        val command = listOf(dockerManager.dockerPath, "pull", "bellurabhiram/renameagent")
        val dockerEnvOverrides = dockerManager.prepareCredentialHelperWorkaround()
        val cmd =
            ProcessBuilder(command)
        dockerManager.applyEnvironmentOverrides(cmd, dockerEnvOverrides)
        try {
            val exitCode = cmd.start().waitFor()
        } catch (e: Exception) {
            println("Failed to pull docker image: ${e.message}")
            showNotification(project, "Docker Error", "Failed to pull docker image: ${e.message}", "Retry CoRename...")
            throw e
        }
    }

    fun agentComplete() {
        logViewer.resetViewer()
        telemetryManager.endSession()
        coRenameInProgress = false
    }

    fun showNotification(
        project: Project,
        title: String,
        content: String,
        actionText: String,
    ) {
        val notification =
            createNotificationGroup().createNotification(
                title,
                content,
                NotificationType.INFORMATION,
            )

        notification.addAction(
            NotificationAction.createSimple(actionText) {
                try {
                    triggerAgent(project)
                } catch (e: Exception) {
                    println("Failed to trigger agent")
                    coRenameInProgress = false
                    throw e
                } finally {
                    notification.expire()
                }
            },
        )
        notification.notify(project)
    }
}
