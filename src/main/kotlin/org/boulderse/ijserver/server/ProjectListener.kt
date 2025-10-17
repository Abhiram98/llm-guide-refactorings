package org.boulderse.ijserver.server

import com.intellij.notification.Notification
import com.intellij.notification.NotificationsManager
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemProgressNotificationManager
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataImportListener
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.util.PopupUtil
import com.intellij.openapi.wm.ex.WindowManagerEx
import kotlinx.coroutines.*
import java.awt.AWTEvent
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.WindowEvent
import java.util.*
import javax.swing.SwingUtilities
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


class ProjectListener {

    var indexingCount = 0
    var importCount = 0
    var resolveCount = 0

    val badWindows: MutableMap<Window, Int> = mutableMapOf()


    fun registerListeners(project: Project){

        setupImportListener(project)
        setupResolveListener()
        setupIndexingListener(project)
        // comment line below to stop auto closing popups.
        registerAwtListener()

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

    suspend fun waitForFinish(preSleep: Long, maxWaitDuration: Duration): Boolean{
        Thread.sleep(preSleep) // sleep 10s for auto-reload to kick in.
        val result = waitForCondition(maxWaitDuration) { indexingCount==0 && importCount==0 && resolveCount==0 }
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

        notificationManager.addNotificationListener(object : ExternalSystemTaskNotificationListener {
            override fun onStart(projectPath: String, id: ExternalSystemTaskId) {
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

            override fun onEnd(projectPath: String, id: ExternalSystemTaskId) {
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

    fun expireNotifications(project: Project){
        val notificationsManager = NotificationsManager.getNotificationsManager()
        val activeNotifications =
            notificationsManager.getNotificationsOfType(Notification::class.java, project)
        PopupUtil.getActiveComponent()
        var windowManager = WindowManagerEx.getInstanceEx()
        val window = windowManager.mostRecentFocusedWindow
//        windowManager.
        for (notification in activeNotifications)
            notification.expire()
    }

    fun registerAwtListener(){
        Toolkit.getDefaultToolkit().addAWTEventListener({ event ->
            if (event.getID() === WindowEvent.WINDOW_OPENED) {
                val window = (event as WindowEvent).window
                print("opened window -> ${window.name}")
                if (window.parent!=null)
                    badWindows[window] = 0
            }
        }, AWTEvent.WINDOW_EVENT_MASK)

        startWindowMonitor()
    }

    fun startWindowMonitor() {
        val timer = Timer(true) // Daemon timer
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val toRemove = mutableListOf<Window>()

                for (window in badWindows.keys) {
                    if (badWindows[window]!! < 1){
                        badWindows[window] = badWindows[window]!! + 1
                    }else if (window.isShowing) {
                        println("Closing window -> ${window.name}")
                        SwingUtilities.invokeLater {
                            window.dispose()
                        }
                        toRemove.add(window)
                    } else {
                        toRemove.add(window) // Already closed
                    }
                }

                toRemove.forEach { badWindows.remove(it) }
            }
        }, 0L, 30 * 1000L) // every 30 seconds
    }



}