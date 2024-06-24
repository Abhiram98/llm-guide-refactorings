package com.intellij.ml.llm.template.ui

import com.intellij.codeInsight.unwrap.ScopeHighlighter
import com.intellij.ml.llm.template.LLMBundle
import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.showEFNotification
import com.intellij.ml.llm.template.telemetry.EFTelemetryDataElapsedTimeNotificationPayload
import com.intellij.ml.llm.template.telemetry.EFTelemetryDataManager
import com.intellij.ml.llm.template.telemetry.TelemetryDataAction
import com.intellij.ml.llm.template.utils.CodeTransformer
import com.intellij.ml.llm.template.utils.EFNotification
import com.intellij.notification.NotificationType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import java.util.concurrent.atomic.AtomicReference

class CompletedRefactoringsPanel(
    project: Project,
    editor: Editor,
    file: PsiFile,
    candidates: List<AbstractRefactoring>,
    codeTransformer: CodeTransformer,
    highlighter: AtomicReference<ScopeHighlighter>,
    efTelemetryDataManager: EFTelemetryDataManager
) : RefactoringSuggestionsPanel(
    project, editor,
    file,
    candidates, codeTransformer, highlighter, efTelemetryDataManager, LLMBundle.message("ef.candidates.completed.popup.extract.function.button.title")
) {
//    override fun buildRefactoringCandidatesTable(
//        tableModel: DefaultTableModel,
//        candidateSignatureMap: Map<AbstractRefactoring, String>
//    ): JBTable {
//        val extractFunctionCandidateTable = object : JBTable(tableModel) {
//            override fun processKeyBinding(ks: KeyStroke, e: KeyEvent, condition: Int, pressed: Boolean): Boolean {
//                if (e.keyCode == KeyEvent.VK_ENTER) {
//                    if (e.id == KeyEvent.KEY_PRESSED) {
//                        if (!isEditing && e.modifiersEx == 0) {
//                            undoRefactoring(selectedRow)
//                        }
//                    }
//                    e.consume()
//                    return true
//                }
//                if (e.keyCode == KeyEvent.VK_ESCAPE) {
//                    if (e.id == KeyEvent.KEY_PRESSED) {
//                        myPopup?.cancel()
//                    }
//                }
//                return super.processKeyBinding(ks, e, condition, pressed)
//            }
//
//            override fun processMouseEvent(e: MouseEvent?) {
//                if (e != null && e.clickCount == 2) {
//                    undoRefactoring(selectedRow)
//                }
//                super.processMouseEvent(e)
//            }
//        }
//
//        extractFunctionCandidateTable.minimumSize = Dimension(-1, 100)
//        extractFunctionCandidateTable.tableHeader = null
//
//
//        extractFunctionCandidateTable.columnModel.getColumn(0).maxWidth = 50
//        extractFunctionCandidateTable.columnModel.getColumn(1).cellRenderer = FunctionNameTableCellRenderer()
//        extractFunctionCandidateTable.setShowGrid(false)
//
//        return extractFunctionCandidateTable
//    }

    override fun performAction(index: Int) {
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
            val refCandidate = myCandidates[index]
            val reverseRefactoring = refCandidate.getReverseRefactoringObject(myProject, myEditor, myFile)
            if (reverseRefactoring!=null) {
                val runnable = Runnable {
                    reverseRefactoring.performRefactoring(myProject, myEditor, myFile)
                }
                runnable.run()
                //        myPopup!!.cancel()
                refCandidate.undone = true
                refreshCandidates(index, "UNDID")
            }
            else {
                showEFNotification(
                    myProject,
                    LLMBundle.message("notification.extract.function.not.reversible"),
                    NotificationType.ERROR
                )
            }
        }
    }


}