package com.intellij.ml.llm.template.server

import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemProgressNotificationManager
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataImportListener
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.configuration.GRADLE_SYSTEM_ID
import java.lang.Exception
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.seconds

class ProjectListener {

    var indexingCount = 0
    var importCount = 0
    var resolveCount = 0

    fun registerListeners(project: Project){

        setupImportListener(project)
        setupResolveListener()
        setupIndexingListener(project)

    }

    suspend fun waitForCondition(
        timeout: Duration,
        checkInterval: Duration = 5.seconds,
        condition: () -> Boolean
    ): Boolean {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeout.inWholeMilliseconds) {
            if (condition()) {
                return true
            }
            delay(checkInterval.inWholeMilliseconds)
        }
        return false
    }

    suspend fun waitForFinish(): Boolean{
        Thread.sleep(10000) // sleep 10s for auto-reload to kick in.
        val result = waitForCondition(180.seconds) { indexingCount==0 && importCount==0 && resolveCount==0 }
        return result
    }

    fun reset(){
        indexingCount = 0
        importCount =0
        resolveCount = 0
    }


    private fun setupImportListener(project: Project) {
        project.messageBus.connect().subscribe(
            ProjectDataImportListener.TOPIC,
            object : ProjectDataImportListener {
                override fun onImportStarted(projectPath: String?) {
                    println("import started")
                    importCount += 1
                    super.onImportStarted(projectPath)
                }

                override fun onImportFinished(projectPath: String?) {
                    println("Import finished.")
                    importCount -= 1
                    super.onImportFinished(projectPath)
                }
            }
        )
    }

    private fun setupResolveListener() {
        val notificationManager =
            ExternalSystemProgressNotificationManager.getInstance()

        notificationManager.addNotificationListener(object : ExternalSystemTaskNotificationListenerAdapter() {
            override fun onStart(id: ExternalSystemTaskId, workingDir: String?) {
                println("Starting resolve.")
                resolveCount += 1
//                if (isResolveProjectTask(id)) {
//                    resolveCount += 1
//                }
            }

//            override fun onSuccess(id: ExternalSystemTaskId) {
////                if (isResolveProjectTask(id)) {
////                    resolveInProgress = false
////                }
//                resolveCount -= 1
//            }

//            override fun onFailure(id: ExternalSystemTaskId, e: Exception) {
//                super.onFailure(id, e)
////                resolveInProgress = false
//            }

            override fun onEnd(id: ExternalSystemTaskId) {
                println("Finished resolve.")
                super.onEnd(id)
                resolveCount -= 1
//                resolveInProgress = false
            }

//            private fun isResolveProjectTask(id: ExternalSystemTaskId): Boolean {
//                return id.getType() === ExternalSystemTaskType.RESOLVE_PROJECT
//            }
        })
    }

    private fun setupIndexingListener(project: Project) {
        class IndexingLifecycleListener(private val project: Project) : DumbService.DumbModeListener {
            override fun enteredDumbMode() {
                println("Indexing started for " + project.name)
//                indexingInProgress = true
                indexingCount += 1
            }

            override fun exitDumbMode() {
                println("Indexing completed for " + project.name)
//                indexingInProgress = false
                indexingCount -= 1
            }
        }

        project.messageBus.connect().subscribe(DumbService.DUMB_MODE, IndexingLifecycleListener(project))
    }



}