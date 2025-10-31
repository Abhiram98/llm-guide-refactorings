package org.boulderse.ijserver.toolwindow

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import javax.swing.Box
import javax.swing.BoxLayout
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
    private val confirmScopeButton = JButton("Confirm Scope")
    private var scopeConfirmed = CompletableDeferred<Boolean>()
    val renamingScopePanel = JPanel()
    val renameSuggestions = JPanel()
    val progressPanel = JPanel()
    private val progressBar = JProgressBar(0, 100)
    private val progressLabel = JLabel("0 / 0 (0%)")
    private var currentNumerator: Int = 0
    private var currentDenominator: Int = 0

    init {
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
        patternPanel.border = javax.swing.BorderFactory.createTitledBorder("Pattern")
        patternPanel.add(patternPane, BorderLayout.CENTER)

        val guardPanel = JPanel(BorderLayout())
        guardPanel.border = javax.swing.BorderFactory.createTitledBorder("Guard")
        guardPanel.add(guardPane, BorderLayout.CENTER)


        renamingScopePanel.layout = BoxLayout(renamingScopePanel, BoxLayout.Y_AXIS)
        renamingScopePanel.border = javax.swing.BorderFactory.createTitledBorder("Renaming Scope")
        renamingScopePanel.add(patternPanel)
        renamingScopePanel.add(Box.createVerticalStrut(8))
        renamingScopePanel.add(guardPanel)

        // Create a vertical container for pattern, guard and buttons
        val southPanel = JPanel()
        southPanel.layout = BoxLayout(southPanel, BoxLayout.Y_AXIS)
        // add the combined titled Renaming Scope panel instead of raw panes
        southPanel.add(renamingScopePanel)
        southPanel.add(Box.createVerticalStrut(8))

        // Button row
        val buttonRow = JPanel()
        val clearButton = JButton("Clear Logs")
        clearButton.addActionListener { _: ActionEvent? -> clear() }
        buttonRow.add(confirmScopeButton)
        confirmScopeButton.addActionListener { _: ActionEvent? -> confirmScope() }
        buttonRow.add(clearButton)

        southPanel.add(buttonRow)

        // Configure progress panel (titled) and add progress bar + label
        progressPanel.layout = BoxLayout(progressPanel, BoxLayout.Y_AXIS)
        progressPanel.border = javax.swing.BorderFactory.createTitledBorder("Progress")
        progressPanel.add(Box.createVerticalStrut(4))

        // Configure determinate progress bar
        progressBar.isStringPainted = false // we use our own label
        progressBar.minimum = 0
        progressBar.maximum = 100
        progressBar.value = 0

        // Add components to the progress panel
        val progressRow = JPanel()
        progressRow.layout = BoxLayout(progressRow, BoxLayout.X_AXIS)
        progressRow.add(progressBar)
        progressRow.add(Box.createHorizontalStrut(8))
        progressRow.add(progressLabel)

        progressPanel.add(progressRow)
        add(progressPanel, BorderLayout.NORTH)

        // Add the southPanel to the existing layout set by LogViewer
        add(southPanel, BorderLayout.SOUTH)

        renameSuggestions.layout = BoxLayout(renameSuggestions, BoxLayout.Y_AXIS)
        renameSuggestions.border = javax.swing.BorderFactory.createTitledBorder("Rename Suggestions")
        add(renameSuggestions, BorderLayout.CENTER)
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

    fun setProgress(numerator: Int, denominator: Int) {
        currentNumerator = numerator.coerceAtLeast(0)
        currentDenominator = denominator.coerceAtLeast(0)

        val percent = if (currentDenominator <= 0) 0 else ((currentNumerator.toDouble() / currentDenominator.toDouble()) * 100).toInt().coerceIn(0, 100)
        progressBar.value = percent
        progressLabel.text = "${currentNumerator} / ${currentDenominator} (${percent}%)"
        // ensure UI updates on EDT
        javax.swing.SwingUtilities.invokeLater {
            progressBar.repaint()
            progressLabel.repaint()
        }
    }

    // Reset progress to initial empty state
    fun resetProgress() {
        currentNumerator = 0
        currentDenominator = 0
        progressBar.value = 0
        progressLabel.text = "0 / 0 (0%)"
        javax.swing.SwingUtilities.invokeLater {
            progressBar.repaint()
            progressLabel.repaint()
        }
    }
}
