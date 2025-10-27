package org.boulderse.ijserver.ui

import com.intellij.codeInsight.unwrap.ScopeHighlighter
import com.intellij.icons.AllIcons
import org.boulderse.ijserver.LLMBundle
import org.boulderse.ijserver.refactoringobjects.AbstractRefactoring
import org.boulderse.ijserver.telemetry.EFTelemetryDataElapsedTimeNotificationPayload
import org.boulderse.ijserver.telemetry.EFTelemetryDataManager
import org.boulderse.ijserver.telemetry.EFTelemetryDataUtils
import org.boulderse.ijserver.telemetry.TelemetryDataAction
import org.boulderse.ijserver.utils.EFNotification
import org.boulderse.ijserver.utils.Observable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.popup.IconButton
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import org.boulderse.ijserver.telemetry.TelemetryElapsedTimeObserver
import org.boulderse.ijserver.telemetry.sendTelemetryData
import java.awt.Dimension
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JComponent
import javax.swing.KeyStroke
import javax.swing.ListSelectionModel
import javax.swing.table.DefaultTableModel
import kotlin.time.Duration.Companion.minutes


open class RefactoringSuggestionsPanel(
    project: Project,
    editor: Editor,
    file: PsiFile,
    candidates: List<AbstractRefactoring>,
    efTelemetryDataManager: EFTelemetryDataManager? = null,
    val button_name: String
) : Observable() {
    lateinit var myRefactoringCandidateTable: JBTable
    lateinit var myRefactoringScrollPane: JBScrollPane
    val myProject: Project = project
    lateinit var refactoringDescriptionBox: JBTextArea
    val myCandidates = candidates
    val myEditor = editor
    var myPopup: JBPopup? = null
    val myFile = file
    val myHighlighter = AtomicReference(ScopeHighlighter(editor))
    val myEFTelemetryDataManager = efTelemetryDataManager
    private val logger = Logger.getInstance("#com.intellij.ml.llm")
    var prevSelectedCandidateIndex = 0
    var completedIndices = mutableListOf<Int>()
    val ratingOptions = arrayOf(
        "No Rating",
        "Very Unhelpful",
        "Unhelpful",
        "Somewhat Unhelpful",
        "Somewhat Helpful",
        "Helpful",
        "Very Helpful"
    )
    val closed = CompletableDeferred<Boolean>()
    val ratingsBox = ComboBox(ratingOptions)
    private var resetRating: Boolean = false
    private lateinit var refactoringDescriptionPane: JBScrollPane

    fun initTable(){
        val tableModel = buildTableModel(myCandidates)
        val refactoringDescriptionMap = buildRefactoringDescriptionMap(myCandidates)
        refactoringDescriptionBox = buildRefactoringDescriptionBox()
        refactoringDescriptionPane = JBScrollPane(refactoringDescriptionBox).apply {
            verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_ALWAYS
            horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            this.preferredSize = Dimension(500, 150)
        }
        myRefactoringCandidateTable = buildRefactoringCandidatesTable(tableModel, refactoringDescriptionMap)
        myRefactoringScrollPane = buildScrollPane()

    }


    private fun buildRefactoringDescriptionMap(candidates: List<AbstractRefactoring>): Map<AbstractRefactoring, String> {
        val candidateSignatureMap: MutableMap<AbstractRefactoring, String> = mutableMapOf()

        candidates.forEach { candidate ->
            candidateSignatureMap[candidate] = candidate.description
        }

        return candidateSignatureMap
    }

    open fun buildRefactoringCandidatesTable(
        tableModel: DefaultTableModel,
        candidateSignatureMap: Map<AbstractRefactoring, String>
    ): JBTable {
        val refFunctionCandidateTable = object : JBTable(tableModel) {
            override fun processKeyBinding(ks: KeyStroke, e: KeyEvent, condition: Int, pressed: Boolean): Boolean {
                if (e.keyCode == KeyEvent.VK_ENTER) {
                    if (e.id == KeyEvent.KEY_PRESSED) {
                        if (!isEditing && e.modifiersEx == 0) {
                            performAction(selectedRow)
                        }
                    }
                    e.consume()
                    return true
                }
                if (e.keyCode == KeyEvent.VK_ESCAPE) {
                    if (e.id == KeyEvent.KEY_PRESSED) {
                        myPopup?.cancel()
                    }
                }
                return super.processKeyBinding(ks, e, condition, pressed)
            }

            override fun processMouseEvent(e: MouseEvent?) {
                if (e != null && e.clickCount == 2) {
                    performAction(selectedRow)
                }
                super.processMouseEvent(e)
            }
        }
        refFunctionCandidateTable.minimumSize = Dimension(-1, 500)
        refFunctionCandidateTable.tableHeader = null

        refFunctionCandidateTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        refFunctionCandidateTable.selectionModel.addListSelectionListener {
            highlightElement(refFunctionCandidateTable, candidateSignatureMap)
        }
        refFunctionCandidateTable.selectionModel.setSelectionInterval(0, 0)
        refFunctionCandidateTable.cellEditor = null

        refFunctionCandidateTable.columnModel.getColumn(0).maxWidth = 50
        refFunctionCandidateTable.columnModel.getColumn(1).cellRenderer = FunctionNameTableCellRenderer()
        refFunctionCandidateTable.setShowGrid(false)
        return refFunctionCandidateTable
    }

    private fun buildScrollPane(): JBScrollPane {
        val refFunctionsScrollPane = JBScrollPane(myRefactoringCandidateTable)

        refFunctionsScrollPane.border = JBUI.Borders.empty()
        refFunctionsScrollPane.maximumSize = Dimension(500, 500)
        refFunctionsScrollPane.minimumSize = Dimension(250, 250)

        return refFunctionsScrollPane
    }

    private fun buildTableModel(candidates: List<AbstractRefactoring>): DefaultTableModel {
        val columnNames = arrayOf("Function Length", "Function Name")
        val model = object : DefaultTableModel() {
            override fun getColumnClass(column: Int): Class<*> {
                // Return the class that corresponds to the specified column. Can return multiple types for multiple columns
                return String::class.java
            }

            override fun isCellEditable(row: Int, column: Int): Boolean {
                // Makes the cells in the table non-editable
                return false
            }
        }
        model.setColumnIdentifiers(columnNames)
        candidates.forEach { refCandidate ->
            val refName = refCandidate.getRefactoringPreview()
            model.addRow(arrayOf("", refName))
        }
        return model
    }

    private fun buildRefactoringDescriptionBox(): JBTextArea {
        val refactoringDescription =
            JBTextArea()

        refactoringDescription.isFocusable = true
        refactoringDescription.isEditable = false
        refactoringDescription.lineWrap = true
        refactoringDescription.wrapStyleWord = true

        return refactoringDescription
    }

    fun createPanel(): JComponent {
        val popupPanel = panel {
            row {
                cell(myRefactoringScrollPane).align(AlignX.FILL)
                    .applyToComponent { minimumSize = JBDimension(100, 500) }
            }

            row {
                cell(refactoringDescriptionPane)
                    .align(AlignX.FILL)
//                    .applyToComponent { minimumSize = JBDimension(100, 500) }
            }

            row {
                button(button_name, actionListener = {
                    performAction(myRefactoringCandidateTable.selectedRow)
                }).comment(
                    LLMBundle.message(
                        "ef.candidates.popup.invoke.extract.function",
                        KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance().getAction("ExtractMethod"))
                    )
                ).align(AlignX.LEFT)
                button("Reject Suggestion", actionListener = {onReject(myRefactoringCandidateTable.selectedRow)}).align(AlignX.RIGHT)
            }
            row {
                cell(ratingsBox).comment("Rate the suggestion!")
                    .onChanged { registerRating(myRefactoringCandidateTable.selectedRow, ratingsBox.selectedItem as String) }
                    .align(AlignX.LEFT)
            }
        }

//        popupPanel.preferredFocusedComponent = myRefactoringCandidateTable
        return popupPanel
    }

    private fun registerRating(selectedRow: Int, rating: String) {
        println("Rating for $selectedRow = $rating")
        if (!resetRating)
            myCandidates[selectedRow].userRating = rating
    }

    fun setDelegatePopup(jbPopup: JBPopup) {
        myPopup = jbPopup
    }

    fun onReject(index: Int){
        myPopup?.cancel()
    }

    private fun generateFunctionSignature(psiMethod: PsiMethod): String {
        val builder = StringBuilder()

        // Add the method name
        builder.append(psiMethod.name)

        // Add the parameters
        builder.append("(")
        psiMethod.parameterList.parameters.joinTo(
            buffer = builder,
            separator = ", \n\t"
        ) { "${it.type.presentableText} ${it.name}" }
        builder.append(")")

        // Add the return type if it's not a constructor
        if (!psiMethod.isConstructor) {
            builder.append(": ${psiMethod.returnType?.presentableText ?: "Unit"}")
        }

        // Add function body
        builder.append(" {\n\t...\n}")

        return builder.toString()
    }


    open fun performAction(index: Int): Boolean {
        if (index !in completedIndices){
            notifyObservers(
                EFNotification(
                    EFTelemetryDataElapsedTimeNotificationPayload(
                        TelemetryDataAction.STOP,
                        prevSelectedCandidateIndex
                    )
                )
            )
            addSelectionToTelemetryData(index)
            val refObj = myCandidates[index]
            ProgressManager.getInstance().runProcessWithProgressSynchronously(
                { refObj.performRefactoring(myProject, myEditor, myFile) },
                "Performing Refactoring",
                true,
                myProject
                )
            myHighlighter.get().dropHighlight()
            refreshCandidates(index, "COMPLETED")
            myPopup?.cancel()
            return true
        }
        return false
    }

    fun addSelectionToTelemetryData(index: Int) {
        val refCandidate = myCandidates[index]
        val hostFunctionTelemetryData = myEFTelemetryDataManager?.getData()?.hostFunctionTelemetryData
        myEFTelemetryDataManager?.addUserSelectionTelemetryData(
            EFTelemetryDataUtils.buildUserSelectionTelemetryData(
                refCandidate,
                index,
                hostFunctionTelemetryData,
                myFile
            )
        )
    }

    fun refreshCandidates(index: Int, tag: String){
        completedIndices.add(index)
//        ExtractFunctionPanel.showPopup(this, myEditor)
        val selectedRow = myRefactoringCandidateTable.selectedRow
        val refName = myRefactoringCandidateTable.getValueAt(selectedRow, 1)!! as String
        myRefactoringCandidateTable.setValueAt("$tag: $refName", selectedRow, 1)
    }

    open fun highlightElement(extractFuncationCandidateJBTable: JBTable, candidateSignatureMap: Map<AbstractRefactoring, String>){
        val candidate = getSelectedRefactoringObject(extractFuncationCandidateJBTable) ?: return
        val startOffset = getStartOffset(extractFuncationCandidateJBTable.selectedRow)
        val endOffset = getEndOffset(extractFuncationCandidateJBTable.selectedRow)
        myEditor.selectionModel.setSelection(startOffset, endOffset)

        refactoringDescriptionBox.text = candidateSignatureMap[candidate]
        val scopeHighlighter: ScopeHighlighter = myHighlighter.get()
        scopeHighlighter.dropHighlight()
        val range = TextRange(startOffset, endOffset)
        scopeHighlighter.highlight(com.intellij.openapi.util.Pair(range, listOf(range)))
        val startLoc = getStartLoc(extractFuncationCandidateJBTable.selectedRow)
        myEditor.scrollingModel.scrollTo(LogicalPosition(startLoc, 0), ScrollType.CENTER)

        // set rating value
        if (::myRefactoringCandidateTable.isInitialized) {
            resetRating = true
            val indexOf = ratingOptions.indexOf(myCandidates[myRefactoringCandidateTable.selectedRow].userRating)
            ratingsBox.selectedIndex = if(indexOf==-1) 0 else indexOf
            resetRating = false
        }
        // compute elapsed time
        notifyObservers(EFNotification(EFTelemetryDataElapsedTimeNotificationPayload(TelemetryDataAction.STOP, prevSelectedCandidateIndex)))
        notifyObservers(EFNotification(EFTelemetryDataElapsedTimeNotificationPayload(TelemetryDataAction.START, extractFuncationCandidateJBTable.selectedRow)))
        prevSelectedCandidateIndex = extractFuncationCandidateJBTable.selectedRow
    }

    open fun getStartLoc(index: Int) = myCandidates[index].startLoc

    private fun getSelectedRefactoringObject(extractFuncationCandidateJBTable: JBTable): AbstractRefactoring? {
        val candidate = myCandidates[extractFuncationCandidateJBTable.selectedRow]
        return candidate
    }

    open fun getStartOffset(index: Int): Int{
        return myCandidates[index].getStartOffset()
    }

    open fun getEndOffset(index: Int): Int{
        return myCandidates[index].getEndOffset()
    }

    suspend fun waitAndClose(): Boolean{
        return withTimeoutOrNull(5.minutes)
            {closed.await()} ?:
            throw Exception("User did not review the suggestion within 5 minutes")
    }

    fun createAndShowPopup() {
        this.initTable()
        val elapsedTimeTelemetryDataObserver = TelemetryElapsedTimeObserver()
        this.addObserver(elapsedTimeTelemetryDataObserver)
        val panel = this.createPanel()
        myEFTelemetryDataManager?.newSession()
        myEFTelemetryDataManager?.setRefactoringObjects(myCandidates)

        val efPopup =
            JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, myRefactoringCandidateTable)
                .setRequestFocus(true)
                .setTitle(LLMBundle.message("ef.candidates.popup.title"))
                .setResizable(true)
                .setMovable(true)
                .setCancelOnClickOutside(false)
                .setCancelButton(IconButton("Close", AllIcons.Actions.Close))
                .setCancelOnOtherWindowOpen(false)
                .setCancelOnWindowDeactivation(false)
                .createPopup()
        // Create the popup

        // Add onClosed listener
        efPopup.addListener(object : JBPopupListener {
            override fun onClosed(event: LightweightWindowEvent
            ) {
                elapsedTimeTelemetryDataObserver.update(
                    EFNotification(
                        EFTelemetryDataElapsedTimeNotificationPayload(TelemetryDataAction.STOP, 0)
                    )
                )
                myEFTelemetryDataManager?.let { elapsedTimeTelemetryDataObserver.buildElapsedTimeTelemetryData(it) }
                myHighlighter.get().dropHighlight()
                myEFTelemetryDataManager?.let {  sendTelemetryData(it) }
                closed.complete(true)
            }

            override fun beforeShown(event: LightweightWindowEvent) {
                super.beforeShown(event)
                elapsedTimeTelemetryDataObserver.update(
                    EFNotification(
                        EFTelemetryDataElapsedTimeNotificationPayload(TelemetryDataAction.START, 0)
                    )
                )
            }
        })

        // set the popup as delegate to the Extract Function panel
        setDelegatePopup(efPopup)

        // Show the popup at the top right corner of the current editor
        val contentComponent = myEditor.contentComponent
        efPopup.show(RelativePoint.getSouthWestOf(contentComponent))
    }
}
