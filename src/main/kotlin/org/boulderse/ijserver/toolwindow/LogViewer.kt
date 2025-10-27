package org.boulderse.ijserver.toolwindow

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import javax.swing.*


open class LogViewer(val initalText: String) : JPanel() {
    // Set up the JTextArea
    private val logArea = JTextArea()

    init {
        logArea.isEditable = false // Make the text area read-only
        logArea.lineWrap = true // Wrap lines if they are too long
        logArea.wrapStyleWord = true // Wrap at word boundaries

        // Set up the JScrollPane
        val scrollPane = JScrollPane(logArea)
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
        scrollPane.preferredSize = Dimension(500, 400) // Adjust the size as needed
        val clearButton = JButton("Clear Logs")
        clearButton.addActionListener { e: ActionEvent? -> clear() }

        // Add the JScrollPane to this JPanel
        layout = BorderLayout()
        add(scrollPane, BorderLayout.CENTER)
        add(clearButton, BorderLayout.SOUTH);
    }

    // Method to add logs
    fun appendLog(log: String) {
        logArea.append(log + "\n")

        // Auto-scroll to the bottom
        logArea.caretPosition = logArea.document.length
    }

    fun clear(){
        logArea.text = initalText + "\n"
    }
}
