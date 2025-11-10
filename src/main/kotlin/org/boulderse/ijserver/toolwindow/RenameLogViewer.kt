package org.boulderse.ijserver.toolwindow

import com.intellij.openapi.application.invokeLater
import com.intellij.ui.components.JBScrollPane
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import org.boulderse.ijserver.telemetry.RenameAgentTelemetryManager
import org.boulderse.ijserver.utils.DockerManager
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.JScrollPane
import javax.swing.JTextArea
import kotlin.time.Duration.Companion.minutes

class RenameLogViewer : LogViewer("Rename agent logs") {
    private val patternText = JTextArea()
    private val guardText = JTextArea()
    val confirmScopeButton = JButton("Confirm Scope")
    private var scopeConfirmed = CompletableDeferred<Boolean>()
    val renameSuggestions = JPanel()
    val actionPanel = JPanel()
    val progressPanel = JPanel()
    private val progressBar = JProgressBar(0, 100)
    private val progressBarRenames = JProgressBar(0, 100)
    private val progressLabel = JLabel("0 / 0 (0%)")
    private val progressLabelRenames = JLabel("0 / 0 (0%)")
    private var completedRenames: Int = 0
    private var totalRenames: Int = 0
    private var completedFiles: Int = 0
    private var totalFiles: Int = 0
    private var inspectingFile: String = "FileName.java"
    private val renameProgressLabel = JLabel("Renames Inspected in $inspectingFile: ")
    private val telemetryManager: RenameAgentTelemetryManager = RenameAgentTelemetryManager.getInstance()
    val actionLabel = JTextArea("Agent is not yet running.")

    val robotSpinnerLabel = JLabel()
    val robotSpinnerIcon = loadScaledIcon("/gifs/robot_spinner.gif", 1.0)
    val robotIdleIcon = loadScaledIcon("/gifs/robot_idle.png", 0.66)

    val humanSpinnerLabel = JLabel()
    val humanIcon = loadScaledIcon("/gifs/human_spinner.gif", 1.0)
    val humanIdleIcon = loadScaledIcon("/gifs/human_idle.png", 0.66)

    var containerId: String? = null

    init {

        val northPanel = createNorthPanel()
        add(northPanel, BorderLayout.NORTH)

        val southPanel = createSouthPanel()
        add(southPanel, BorderLayout.SOUTH)

        val centerPanel = createCenterPanel()
        add(centerPanel, BorderLayout.CENTER)
    }

    private fun createCenterPanel(): JPanel {
        val centerPanel = JPanel(BorderLayout())

        renameSuggestions.layout = BoxLayout(renameSuggestions, BoxLayout.Y_AXIS)
        renameSuggestions.border = BorderFactory.createTitledBorder("Rename Suggestions")

        val jbScrollPane =
            JBScrollPane(
                renameSuggestions,
                JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER,
            )
        centerPanel.add(jbScrollPane, BorderLayout.CENTER)
        return centerPanel
    }

    private fun createNorthPanel(): JPanel {
        val northPanel = JPanel()
        northPanel.layout = BoxLayout(northPanel, BoxLayout.Y_AXIS)
        actionPanel.layout = BoxLayout(actionPanel, BoxLayout.Y_AXIS)
        actionPanel.border = BorderFactory.createTitledBorder("Action Item:")
        actionLabel.isEditable = false
        val scrollPane = JBScrollPane(actionLabel)
        actionPanel.add(scrollPane)
        robotSpinnerLabel.icon = robotIdleIcon
        humanSpinnerLabel.icon = humanIdleIcon

        val actionRow = JPanel()
        actionRow.layout = BoxLayout(actionRow, BoxLayout.Y_AXIS)
        val spinnerRow = JPanel()
        spinnerRow.layout = BoxLayout(spinnerRow, BoxLayout.X_AXIS)

        val robotPane = JPanel()
//        robotPane.layout = BoxLayout(robotPane, BoxLayout.Y_AXIS)
        robotPane.add(robotSpinnerLabel)
        robotPane.add(JLabel("Agent"))

        val humanPane = JPanel()
//        humanPane.layout = BoxLayout(robotPane, BoxLayout.Y_AXIS)
        humanPane.add(humanSpinnerLabel)
        humanPane.add(JLabel("Developer"))

        spinnerRow.add(robotPane)
        spinnerRow.add(humanPane)
        actionRow.add(spinnerRow)
        actionRow.add(actionPanel)
        northPanel.add(actionRow)
        return northPanel
    }

    private fun createSouthPanel(): JPanel {
        val progressPanel = setupProgressPanel()

        val southPanel = JPanel()

        southPanel.layout = BoxLayout(southPanel, BoxLayout.Y_AXIS)
        southPanel.add(progressPanel)

        // Button row
        val buttonRow = JPanel()
        val stopButton = JButton("Stop Agent")
        stopButton.addActionListener { _: ActionEvent? -> stopAgent() }
        buttonRow.add(stopButton)

        southPanel.add(buttonRow)
        return southPanel
    }

    private fun createScopePanel(): JPanel {
        patternText.lineWrap = true // Wrap lines if they are too long
        patternText.wrapStyleWord = true // Wrap at word boundaries
        guardText.lineWrap = true
        guardText.wrapStyleWord = true

        // Prepare pattern and guard panes
        val patternPane = JScrollPane(patternText)
        patternPane.preferredSize = Dimension(500, 100)

        val guardPane = JScrollPane(guardText)
        guardPane.preferredSize = Dimension(500, 100)

        patternText.text = "<Rename pattern>"
        guardText.text = "<Guard conditions here>"

        // Create titled sub-panels for Pattern and Guard, and wrap them in a "Renaming Scope" panel
        val patternPanel = JPanel(BorderLayout())
        patternPanel.border = BorderFactory.createTitledBorder("Pattern: A Find-Replace style rename pattern")
        patternPanel.add(patternPane, BorderLayout.CENTER)

        val guardPanel = JPanel(BorderLayout())
        guardPanel.border = BorderFactory.createTitledBorder("Guard: The conditions where the pattern should apply")
        guardPanel.add(guardPane, BorderLayout.CENTER)
        val renamingScopePanel = JPanel()
        renamingScopePanel.layout = BoxLayout(renamingScopePanel, BoxLayout.Y_AXIS)
//        renamingScopePanel.border = BorderFactory.createTitledBorder("Renaming Scope")
        renamingScopePanel.add(patternPanel)
        renamingScopePanel.add(Box.createVerticalStrut(8))
        renamingScopePanel.add(guardPanel)

        confirmScopeButton.addActionListener { _: ActionEvent? -> confirmScope() }
        val buttonRowScope = JPanel()
        buttonRowScope.add(confirmScopeButton)
        confirmScopeButton.isEnabled = false
        renamingScopePanel.add(buttonRowScope)
        return renamingScopePanel
    }

    private fun setupProgressPanel(): JPanel {
        // Configure progress panel (titled) and add progress bar + label
        progressPanel.layout = BoxLayout(progressPanel, BoxLayout.Y_AXIS)
        progressPanel.border = BorderFactory.createTitledBorder("Progress")
        progressPanel.add(Box.createVerticalStrut(4))

        // Configure determinate progress bar
        progressBar.isStringPainted = false // we use our own label
        progressBar.minimum = 0
        progressBar.maximum = 100
        progressBar.value = 0

        // Add components to the progress panel
        val progressPanel = JPanel()
        progressPanel.layout = BoxLayout(progressPanel, BoxLayout.Y_AXIS)

        val inspectedRow = JPanel()
        inspectedRow.layout = BoxLayout(inspectedRow, BoxLayout.Y_AXIS)
        inspectedRow.add(JLabel("Files Inspected:"))

        val inspectedProgressLine = JPanel()
        inspectedProgressLine.layout = BoxLayout(inspectedProgressLine, BoxLayout.X_AXIS)
        inspectedProgressLine.add(progressBar)
        inspectedProgressLine.add(Box.createHorizontalStrut(8))
        inspectedProgressLine.add(progressLabel)

        inspectedRow.add(inspectedProgressLine)

        val renameProgressRow = JPanel()
        renameProgressRow.layout = BoxLayout(renameProgressRow, BoxLayout.Y_AXIS)
        renameProgressRow.add(renameProgressLabel)

        val renameProgressLine = JPanel()
        renameProgressLine.layout = BoxLayout(renameProgressLine, BoxLayout.X_AXIS)
        renameProgressLine.add(progressBarRenames)
        renameProgressLine.add(Box.createHorizontalStrut(8))
        renameProgressLine.add(progressLabelRenames)

        renameProgressRow.add(renameProgressLine)

        progressPanel.add(inspectedRow)
        progressPanel.add(Box.createVerticalStrut(8))
        progressPanel.add(renameProgressRow)
        return progressPanel
    }

    private fun stopAgent() {
        if (containerId != null) {
            println("Stopping process $containerId")
            val dockerManager = DockerManager.getInstance()
            Runtime.getRuntime().exec("${dockerManager.dockerPath} kill $containerId")
            telemetryManager.stoppedEarly()
        }
    }

    fun registerAgentContainerId(containerId: String) {
        this.containerId = containerId
    }

    fun getPatternText(): String = patternText.text

    fun getGuardText(): String = guardText.text

    fun setPattern(pattern: String) {
        patternText.text = pattern
    }

    fun setGuard(guard: String) {
        guardText.text = guard
    }

    suspend fun waitForConfirmation(): Boolean =
        withTimeoutOrNull(5.minutes) {
            scopeConfirmed.await()
        } ?: throw Exception("User did not review the scope within 5 minutes")

    fun confirmScope() {
        scopeConfirmed.complete(true)
    }

    fun resetConfirmationWait() {
        scopeConfirmed = CompletableDeferred<Boolean>()
    }

    fun setLogMessage(message: String) {
        this.clear()
        this.appendLog(message)
    }

    fun resetScope() {
        patternText.text = "<Rename pattern>"
        guardText.text = "<Guard conditions here>"
    }

    fun setFileInspect(fileName: String) {
        inspectingFile = fileName
        renameProgressLabel.text = "Renames Inspected in $inspectingFile: "
        renameProgressLabel.repaint()
    }

    fun setTotalRenames(num: Int) {
        totalRenames = completedRenames + num
        repaintRenameProgress()
    }

    fun incCompletedRenames() {
        completedRenames++
        repaintRenameProgress()
    }

    fun incCompletedFiles() {
        completedFiles++
        repaintFileProgress()
    }

    fun incTotalFiles() {
        totalFiles++
        repaintFileProgress()
    }

    private fun repaintRenameProgress() {
        val percent =
            if (totalRenames <=
                0
            ) {
                0
            } else {
                ((completedRenames.toDouble() / totalRenames.toDouble()) * 100).toInt().coerceIn(0, 100)
            }
        progressBarRenames.value = percent
        progressLabelRenames.text = "$completedRenames / $totalRenames ($percent%)"
        // ensure UI updates on EDT
        javax.swing.SwingUtilities.invokeLater {
            progressBarRenames.repaint()
            progressLabelRenames.repaint()
        }
    }

    private fun repaintFileProgress() {
        val percent =
            if (totalFiles <=
                0
            ) {
                0
            } else {
                ((completedFiles.toDouble() / totalFiles.toDouble()) * 100).toInt().coerceIn(0, 100)
            }
        progressBar.value = percent
        progressLabel.text = "$completedFiles / $totalFiles ($percent%)"
        // ensure UI updates on EDT
        javax.swing.SwingUtilities.invokeLater {
            progressBar.repaint()
            progressLabel.repaint()
        }
    }

    fun resetViewer() {
        resetProgress()
        resetProgressFiles()
        resetScope()
        setFileInspect("<Filename.java>")
        setActionItem("Agent is not running.")
        stopRobotAnimation()
        stopHumanAnimation()
        resetRenameSuggestions()
    }

    fun resetProgress() {
        completedRenames = 0
        totalRenames = 0
        progressBarRenames.value = 0
        progressLabelRenames.text = "0 / 0 (0%)"
        javax.swing.SwingUtilities.invokeLater {
            progressBarRenames.repaint()
            progressLabelRenames.repaint()
        }
    }

    fun resetProgressFiles() {
        completedFiles = 0
        totalFiles = 0
        progressBar.value = 0
        progressLabel.text = "0 / 0 (0%)"
        javax.swing.SwingUtilities.invokeLater {
            progressBar.repaint()
            progressLabel.repaint()
        }
    }

    fun setActionItem(item: String) {
        actionLabel.text = item
        actionPanel.revalidate()
        actionPanel.repaint()
    }

    fun startRobotAnimation() {
        invokeLater {
            robotSpinnerLabel.icon = robotSpinnerIcon
            actionPanel.revalidate()
            actionPanel.repaint()
        }
    }

    fun stopRobotAnimation() {
        invokeLater {
            robotSpinnerLabel.icon = robotIdleIcon
            actionPanel.revalidate()
            actionPanel.repaint()
        }
    }

    fun startHumanAnimation() {
        invokeLater {
            humanSpinnerLabel.icon = humanIcon
            actionPanel.revalidate()
        }
    }

    fun stopHumanAnimation() {
        invokeLater {
            humanSpinnerLabel.icon = humanIdleIcon
        }
    }

    private fun loadScaledIcon(
        path: String,
        scale: Double,
    ): ImageIcon {
        val originalIcon = ImageIcon(javaClass.getResource(path))
        if (scale == 1.0) {
            return originalIcon
        }
        val width = (originalIcon.iconWidth * scale).toInt()
        val height = (originalIcon.iconHeight * scale).toInt()
        val scaledImage = originalIcon.image.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH)
        return ImageIcon(scaledImage)
    }

    fun resetRenameSuggestions() {
        renameSuggestions.removeAll()
        renameSuggestions.border = BorderFactory.createTitledBorder("Rename Suggestions")
        renameSuggestions.revalidate()
        renameSuggestions.repaint()
    }

    fun showScopePanel() {
        renameSuggestions.removeAll()
        renameSuggestions.border = BorderFactory.createTitledBorder("Rename Scope")
        val scopeConfirmPanel = createScopePanel()
        renameSuggestions.add(scopeConfirmPanel)
        renameSuggestions.revalidate()
        renameSuggestions.repaint()
    }

    fun noOpReview(statusString: String? = null) {
        val agentStatus = statusString ?: "Sit back and relax :)"
        this.setActionItem("Agent is thinking. $agentStatus")
        this.startRobotAnimation()
        this.stopHumanAnimation()
        this.resetRenameSuggestions()
    }
}
