package org.boulderse.ijserver.refactoringobjects.movemethod

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiVariable
import com.intellij.refactoring.move.MoveCallback
import com.intellij.refactoring.move.moveInstanceMethod.MoveInstanceMethodProcessor
import com.intellij.usageView.UsageInfo

class MoveInstanceMethodProcessWithCallBack(
    project: Project,
    method: PsiMethod,
    targetVariable: PsiVariable,
    newVisibility: String,
    isOpenInEditor: Boolean,
    oldClassParameterNames: Map<PsiClass, String>,
    val moveCallBack: MoveCallback,
) : MoveInstanceMethodProcessor(project, method, targetVariable, newVisibility, isOpenInEditor, oldClassParameterNames) {
    override fun performRefactoring(usages: Array<out UsageInfo>) {
        super.performRefactoring(usages)
        moveCallBack?.refactoringCompleted()
    }
}
