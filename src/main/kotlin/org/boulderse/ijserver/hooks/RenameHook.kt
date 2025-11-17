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
        set(value) {
            field = value
            updateStopButtonState()
        }
    var seedOldName: String? = null
    var seedNewName: String? = null
    var seedElement: PsiElement? = null
    val dockerManager = DockerManager.getInstance()

    val telemetryManager = RenameAgentTelemetryManager.getInstance()

    companion object {
        @Volatile
        private var instance: RenameHook? = null

        fun getInstance(): RenameHook? {
            return instance
        }
    }

    init {
        instance = this
    }

    private fun updateStopButtonState() {
        logViewer.updateStopButtonState(coRenameInProgress)
    }

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
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                if (!dockerManager.testDockerPath()) {
                    showNotification(
                        project,
                        "Docker Error",
                        "Could not find docker installed on machine. Please install docker and retry",
                        "Retry CoRename...",
                    )
                    return@executeOnPooledThread
                } else if (!dockerManager.checkDockerDaemon()) {
                    showNotification(
                        project,
                        "Docker Error",
                        "Could not connect to docker daemon. Please check that docker daemon is running and retry",
                        "Retry CoRename...",
                    )
                    return@executeOnPooledThread
                }

                telemetryManager.startNewSession()
                if (seedElement!=null)
                    PsiUtils.getElementTypeStr(seedElement!!)?.let {  telemetryManager.setSeedType(it) }

                val llmKey = RefAgentSettingsManager.getInstance().getOpenAiKey()
                if (llmKey == "") {
                    showUnauthorizedNotification(project)
                    return@executeOnPooledThread
                }

                val command =
                    dockerManager.dockerCommand!! +
                    mutableListOf(
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
                val waitCommand = dockerManager.dockerCommand!! + listOf("container", "wait", containerId)
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
        try {
            val logsCommand = dockerManager.dockerCommand!! + listOf("logs", "--tail", "1000", containerId)
            val logsProcessBuilder = ProcessBuilder(logsCommand)
            dockerManager.applyEnvironmentOverrides(logsProcessBuilder, dockerEnvOverrides)

            logsProcessBuilder.redirectErrorStream(true)

            val process = logsProcessBuilder.start()
            val output = process.inputStream.bufferedReader().use { it.readText() }

            val finished = process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                println("fetchContainerLogs: process timed out; logs may be truncated")
            }

            val exitCode = process.exitValue()
            println("container logs exit=$exitCode output:\n$output")
        } catch (e: Exception) {
            println("Failed to fetch container logs: ${e.message}")
        }
    }

    private fun pullDockerImage(project: Project) {
        logViewer.noOpReview("Starting the agent...")
        val command = dockerManager.dockerCommand!! + listOf("pull", "bellurabhiram/renameagent")
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
        telemetryManager.currentTelemetryData?.let { logViewer.showStats(it) }
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
