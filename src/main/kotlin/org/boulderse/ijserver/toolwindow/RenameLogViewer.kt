package org.boulderse.ijserver.toolwindow

import com.intellij.openapi.application.invokeLater
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.JScrollPane
import javax.swing.JTextArea
import kotlin.time.Duration.Companion.minutes
import com.intellij.util.SVGLoader

class RenameLogViewer : LogViewer("Rename agent logs") {
    private val patternText = JTextArea()
    private val guardText = JTextArea()
    private val confirmScopeButton = JButton("Confirm Scope")
    private var scopeConfirmed = CompletableDeferred<Boolean>()
    val renamingScopePanel = JPanel()
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
    val actionLabel = JLabel("Agent is not yet running.")

    val robotSpinnerLabel = JLabel()
    val spinnerIcon = javax.swing.ImageIcon(javaClass.getResource("/gifs/robot_spinner.gif"))
    val idleIcon = javax.swing.ImageIcon(javaClass.getResource("/gifs/robot_idle.png"))

    val humanSpinnerLabel = JLabel()
    val humanIcon = javax.swing.ImageIcon(javaClass.getResource("/gifs/human_spinner.gif"))
    val humanIdleIcon = javax.swing.ImageIcon(javaClass.getResource("/gifs/human_idle.png"))

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

        centerPanel.add(renameSuggestions, BorderLayout.CENTER)
        return centerPanel
    }

    private fun createNorthPanel(): JPanel {
        val northPanel = JPanel()
        northPanel.layout = BoxLayout(northPanel, BoxLayout.Y_AXIS)
        actionPanel.layout = BoxLayout(actionPanel, BoxLayout.Y_AXIS)
        actionPanel.border = BorderFactory.createTitledBorder("Action Item:")
        actionPanel.add(Box.createVerticalStrut(16))
        actionPanel.add(actionLabel)


        robotSpinnerLabel.icon = idleIcon
        humanSpinnerLabel.icon = humanIdleIcon


        val actionRow = JPanel()
        actionRow.layout = BoxLayout(actionRow, BoxLayout.X_AXIS)
        actionRow.add(robotSpinnerLabel)
        actionRow.add(humanSpinnerLabel)
        actionRow.add(Box.createVerticalStrut(4))

        actionPanel.add(actionRow)
        northPanel.add(actionPanel)
        return northPanel
    }

    private fun createSouthPanel(): JPanel {

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
        patternPanel.border = javax.swing.BorderFactory.createTitledBorder("Pattern: A Find-Replace style rename pattern")
        patternPanel.add(patternPane, BorderLayout.CENTER)

        val guardPanel = JPanel(BorderLayout())
        guardPanel.border = javax.swing.BorderFactory.createTitledBorder("Guard: The conditions where the pattern should apply")
        guardPanel.add(guardPane, BorderLayout.CENTER)

        renamingScopePanel.layout = BoxLayout(renamingScopePanel, BoxLayout.Y_AXIS)
        renamingScopePanel.border = javax.swing.BorderFactory.createTitledBorder("Renaming Scope")
        renamingScopePanel.add(patternPanel)
        renamingScopePanel.add(Box.createVerticalStrut(8))
        renamingScopePanel.add(guardPanel)

        val southPanel = JPanel()
        southPanel.layout = BoxLayout(southPanel, BoxLayout.Y_AXIS)
        // add the combined titled Renaming Scope panel instead of raw panes


        southPanel.add(renamingScopePanel)
        confirmScopeButton.addActionListener { _: ActionEvent? -> confirmScope() }
        val buttonRowScope = JPanel()
        buttonRowScope.add(confirmScopeButton)
        southPanel.add(buttonRowScope)
        val progressPanel = setupProgressPanel()
        southPanel.add(progressPanel)
        //        southPanel.add(Box.createVerticalStrut(8))

        // Button row
        val buttonRow = JPanel()
        val stopButton = JButton("Stop Agent")
        stopButton.addActionListener { _: ActionEvent? -> stopAgent() }
        buttonRow.add(stopButton)

        southPanel.add(buttonRow)
        return southPanel
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
            Runtime.getRuntime().exec("docker kill $containerId")
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

    fun resetProgress() {
        completedRenames = 0
        totalRenames = 0
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
            robotSpinnerLabel.icon = spinnerIcon
            actionPanel.revalidate()
            actionPanel.repaint()
        }
    }

    fun stopRobotAnimation() {
        invokeLater {
            robotSpinnerLabel.icon = idleIcon
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
}
