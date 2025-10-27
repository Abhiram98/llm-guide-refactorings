package org.boulderse.ijserver.toolwindow

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.BoxLayout
import javax.swing.Box
import kotlin.time.Duration.Companion.minutes

class RenameLogViewer: LogViewer("Rename agent logs") {

    private val patternText = JTextArea()
    private val guardText = JTextArea()
    private val confirmScopeButton = JButton("Confirm Scope")
    private var scopeConfirmed = CompletableDeferred<Boolean>()

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

            val renamingScopePanel = JPanel()
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

        // Add the southPanel to the existing layout set by LogViewer
        add(southPanel, BorderLayout.SOUTH)
    }

    fun getPatternText(): String {
        return patternText.text
    }

    fun getGuardText(): String {
        return guardText.text
    }

    fun setPattern(pattern: String) {
        patternText.text = pattern
    }

    fun setGuard(guard: String) {
        guardText.text = guard
    }

    suspend fun waitForConfirmation(): Boolean {
        return withTimeoutOrNull(5.minutes)
        {scopeConfirmed.await()} ?: throw Exception("User did not review the scope within 5 minutes")
    }

    fun confirmScope(){
        scopeConfirmed.complete(true)
    }

    fun resetConfirmationWait(){
        scopeConfirmed = CompletableDeferred<Boolean>()
    }

    fun setLogMessage(message: String){
        this.clear()
        this.appendLog(message)
    }

}