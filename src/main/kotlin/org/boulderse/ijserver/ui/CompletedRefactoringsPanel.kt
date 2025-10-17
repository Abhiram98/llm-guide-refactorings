package org.boulderse.ijserver.ui

import com.intellij.codeInsight.unwrap.ScopeHighlighter
import org.boulderse.ijserver.LLMBundle
import org.boulderse.ijserver.refactoringobjects.AbstractRefactoring
import org.boulderse.ijserver.showEFNotification
import org.boulderse.ijserver.telemetry.EFTelemetryDataElapsedTimeNotificationPayload
import org.boulderse.ijserver.telemetry.EFTelemetryDataManager
import org.boulderse.ijserver.telemetry.TelemetryDataAction
import org.boulderse.ijserver.utils.CodeTransformer
import org.boulderse.ijserver.utils.EFNotification
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
    efTelemetryDataManager: EFTelemetryDataManager,
    var reverseRefactorings: List<AbstractRefactoring?> = getReverseObjects(candidates, project, editor, file)
) : RefactoringSuggestionsPanel(
    project, editor,
    file,
    candidates, codeTransformer, highlighter, efTelemetryDataManager, LLMBundle.message("ef.candidates.completed.popup.extract.function.button.title")
) {

    companion object{
        fun getReverseObjects(
            refactoringObjects: List<AbstractRefactoring>,
            project: Project, editor: Editor, file: PsiFile
        ): List<AbstractRefactoring?>{
            return refactoringObjects.map { it.reverseRefactoring }
        }
    }
    init {
        reverseRefactorings = recalibrateRefactorings(reverseRefactorings)
        // Recalibrate at the start to avoid issue with other ref objects being executed later.
    }

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

            // Undo refactorings after index.
            // Undo Refactoring.
            // Redo refactoring.

            val refCandidate = myCandidates[index]
//            for (i in (index until myCandidates.size).reversed()){
//                val refCandidate = myCandidates[i]
//                refCandidate.getReverseRefactoringObject(myProject, myEditor, myFile)?.performRefactoring(myProject, myEditor, myFile)
//            }
//            for (i in index+1 until myCandidates.size){
//                val refCandidate = myCandidates[i]
//                refCandidate.performRefactoring(myProject, myEditor, myFile)
//            }
//

//            val reverseRefactoring = refCandidate.getReverseRefactoringObject(myProject, myEditor, myFile)
            val reverseRefactoring = reverseRefactorings[index]
            if (reverseRefactoring!=null && reverseRefactoring.isValid(myProject, myEditor, myFile)) {
                val runnable = Runnable {
                    reverseRefactoring.performRefactoring(myProject, myEditor, myFile)
                }
                runnable.run()
                //        myPopup!!.cancel()
                refCandidate.undone = true
                refreshCandidates(index, "UNDID")
                reverseRefactorings = recalibrateRefactorings(reverseRefactorings)
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

    override fun getEndOffset(index: Int): Int {
        if(reverseRefactorings[index]!=null)
            return reverseRefactorings[index]!!.getEndOffset()
        return 0
    }

    override fun getStartOffset(index: Int): Int {
        if(reverseRefactorings[index]!=null)
            return reverseRefactorings[index]!!.getStartOffset()
        return 0
    }

    override fun getStartLoc(index: Int): Int {
        if(reverseRefactorings[index]!=null)
            return myEditor.document.getLineNumber(reverseRefactorings[index]!!.getStartOffset())
        return 0
    }

    private fun recalibrateRefactorings(refactoringObjects: List<AbstractRefactoring?>): MutableList<AbstractRefactoring?> {
        // recalibrate refactorings in case any got invalidated.
        val recalibratedRefactorings = mutableListOf<AbstractRefactoring?>()
        refactoringObjects.forEach { ref ->
            if (ref==null)
                recalibratedRefactorings.add(ref)
            else if (ref.isValid(myProject, myEditor, myFile))
                recalibratedRefactorings.add(ref)
            else
                recalibratedRefactorings.add(ref.recalibrateRefactoring(myProject, myEditor, myFile))
        }
        return recalibratedRefactorings

    }


}