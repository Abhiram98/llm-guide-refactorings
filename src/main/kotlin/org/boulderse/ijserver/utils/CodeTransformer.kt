package org.boulderse.ijserver.utils

import org.boulderse.ijserver.refactoringobjects.extractfunction.EFCandidate
import org.boulderse.ijserver.refactoringobjects.AbstractRefactoring
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class CodeTransformer : Observable() {
    private val logger = Logger.getInstance("#com.intellij.ml.llm")

    fun applyCandidate(refCandidate: AbstractRefactoring, project: Project, editor: Editor, file: PsiFile): Boolean {
        var applicationResult = EFApplicationResult.OK
        var reason = ""

//        if (!isCandidateValid(efCandidate)) {
         if (!refCandidate.isValid(project, editor, file)){
            applicationResult = EFApplicationResult.FAIL
            reason = "invalid refactoring candidate"
        } else {
//            editor.selectionModel.setSelection(refCandidate.getStartOffset(), refCandidate.getEndOffset())
            try {
//                invokeExtractFunction(refCandidate.functionName, project, editor, file)
                refCandidate.performRefactoring(project, editor, file)
            } catch (e: Exception) {
                applicationResult = EFApplicationResult.FAIL
                reason = e.message ?: ""
            } catch (e: Error) {
                applicationResult = EFApplicationResult.FAIL
                reason = e.message ?: ""
            }
        }

        notifyObservers(
            EFNotification(
                EFCandidateApplicationPayload(
                    result = applicationResult,
                    reason = reason,
                    candidate = refCandidate
                )
            )
        )

        return applicationResult == EFApplicationResult.OK
    }


}