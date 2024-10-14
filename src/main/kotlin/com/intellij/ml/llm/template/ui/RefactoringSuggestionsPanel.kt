package com.intellij.ml.llm.template.ui

import com.intellij.codeInsight.unwrap.ScopeHighlighter
import com.intellij.lang.java.JavaLanguage
import com.intellij.ml.llm.template.LLMBundle
import com.intellij.ml.llm.template.refactoringobjects.extractfunction.EFCandidate
import com.intellij.ml.llm.template.models.FunctionNameProvider
import com.intellij.ml.llm.template.models.MyMethodExtractor
import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.telemetry.EFTelemetryDataElapsedTimeNotificationPayload
import com.intellij.ml.llm.template.telemetry.EFTelemetryDataManager
import com.intellij.ml.llm.template.telemetry.EFTelemetryDataUtils
import com.intellij.ml.llm.template.telemetry.TelemetryDataAction
import com.intellij.ml.llm.template.utils.CodeTransformer
import com.intellij.ml.llm.template.utils.EFNotification
import com.intellij.ml.llm.template.utils.Observable
import com.intellij.ml.llm.template.utils.PsiUtils
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.refactoring.extractMethod.newImpl.MethodExtractor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.table.JBTable
import com.intellij.ui.util.preferredWidth
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import org.jetbrains.annotations.Nls
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.base.psi.unifier.toRange
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.*
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.elementsInRange
import java.awt.Dimension
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JComponent
import javax.swing.JTextArea
import javax.swing.KeyStroke
import javax.swing.ListSelectionModel
import javax.swing.table.DefaultTableModel


open class RefactoringSuggestionsPanel(
    project: Project,
    editor: Editor,
    file: PsiFile,
    candidates: List<AbstractRefactoring>,
    codeTransformer: CodeTransformer,
    highlighter: AtomicReference<ScopeHighlighter>,
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
    val myCodeTransformer = codeTransformer
    val myFile = file
    val myHighlighter = highlighter
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
        myRefactoringScrollPane = builScrollPane()

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

    private fun builScrollPane(): JBScrollPane {
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

    private fun generateFunctionSignature(efCandidate: EFCandidate): String {
        var signature = LLMBundle.message("ef.candidates.popup.cannot.compute.function.signature")
        when (myFile.language) {
            JavaLanguage.INSTANCE ->
                MyMethodExtractor(FunctionNameProvider(efCandidate.functionName)).findAndSelectExtractOption(
                    myEditor,
                    myFile,
                    TextRange(efCandidate.offsetStart, efCandidate.offsetEnd)
                )?.thenApply { options ->
                    val elementsToReplace = MethodExtractor().prepareRefactoringElements(options)
                    elementsToReplace.method.setName(efCandidate.functionName)
                    signature = generateFunctionSignature(elementsToReplace.method)
                }

            KotlinLanguage.INSTANCE -> {
                fun computeKotlinFunctionSignature(
                    functionName: String,
                    file: KtFile,
                    elements: List<PsiElement>,
                    targetSibling: PsiElement
                ): @Nls String {
                    var kotlinSignature = LLMBundle.message("ef.candidates.popup.cannot.compute.function.signature")
                    val extractionData = ExtractionData(file, elements.toRange(false), targetSibling)
                    try {
                        val analysisResult = extractionData.performAnalysis()
                        if (analysisResult.status == AnalysisResult.Status.SUCCESS) {
                            val config = ExtractionGeneratorConfiguration(
                                analysisResult.descriptor!!,
                                ExtractionGeneratorOptions(
                                    inTempFile = true,
                                    target = ExtractionTarget.FUNCTION,
                                    dummyName = functionName,
                                )
                            )
                            kotlinSignature = config.getDeclarationPattern().replace(Regex("[\\w.]+${efCandidate.functionName}"), efCandidate.functionName)
                            val regex = Regex("\\b(\\w+)(?:\\.\\w+)+\\b")
                            kotlinSignature = kotlinSignature.replace(regex) { matchResult ->
                                matchResult.value.substringAfterLast('.')
                            }
                            kotlinSignature = kotlinSignature.replace(",", ",\n\t")
                            kotlinSignature = kotlinSignature.replace("$0", "\t...")
                        }
                    } catch (t: Throwable) {
                        logger.error("Error computing signature for candidate:\n$efCandidate\n")
                        logger.error(t)
                    }

                    return kotlinSignature
                }

                val elements = myFile.elementsInRange(TextRange(efCandidate.offsetStart, efCandidate.offsetEnd))
                if (elements.isNotEmpty()) {
                    val targetSibling = PsiUtils.getParentFunctionOrNull(elements[0], myFile.language)
                    if (targetSibling != null) {
                        signature = computeKotlinFunctionSignature(
                            efCandidate.functionName,
                            myFile as KtFile,
                            elements,
                            targetSibling
                        )
                    }
                }
            }
        }
        return signature
    }

    open fun performAction(index: Int) {
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
            val efCandidate = myCandidates[index]

            val runnable = Runnable {
                myCodeTransformer.applyCandidate(efCandidate, myProject, myEditor, myFile)
            }
            runnable.run()
            refreshCandidates(index, "COMPLETED")
        }
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
}