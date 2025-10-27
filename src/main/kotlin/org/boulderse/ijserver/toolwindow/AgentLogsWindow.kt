package org.boulderse.ijserver.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.JComponent
import javax.swing.JPanel

val logViewer = RenameLogViewer()

class AgentLogsWindow : ToolWindowFactory {
    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow,
    ) {
//        val toolWindowContent: CalendarToolWindowContent = CalendarToolWindowContent(toolWindow)
        val content =
            ContentFactory.getInstance().createContent(logViewer, "", false)
        toolWindow.contentManager.addContent(content)
    }
}
