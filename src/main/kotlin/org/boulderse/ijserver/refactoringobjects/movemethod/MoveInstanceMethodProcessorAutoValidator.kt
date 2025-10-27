package org.boulderse.ijserver.refactoringobjects.movemethod

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiVariable
import com.intellij.refactoring.move.MoveCallback
import com.intellij.refactoring.move.moveInstanceMethod.MoveInstanceMethodProcessor
import com.intellij.usageView.UsageInfo
import com.intellij.util.containers.MultiMap

class MoveInstanceMethodProcessorAutoValidator(
    project: Project,
    method: PsiMethod,
    targetVariable: PsiVariable,
    newVisibility: String,
    isOpenInEditor: Boolean,
    oldClassParameterNames: Map<PsiClass, String>,
) : MoveInstanceMethodProcessor(project, method, targetVariable, newVisibility, isOpenInEditor, oldClassParameterNames) {
    override fun showConflicts(
        conflicts: MultiMap<PsiElement, String>,
        usages: Array<out UsageInfo>?,
    ): Boolean {
        if (conflicts.isEmpty) return true
        return conflicts
            .toHashMap()
            .map { it ->
                it.value.filter { message -> !(message.contains("is already defined in the class") && message.contains("Method")) }
            }.reduce { acc, strings -> acc + strings }
            .isEmpty()
//        return conflicts.isEmpty
    }

    override fun findUsages(): Array<UsageInfo> { // To make reflection work
        return super.findUsages()
    }

    override fun preprocessUsages(refUsages: Ref<Array<UsageInfo>>): Boolean { // to make reflection work.
        return super.preprocessUsages(refUsages)
    }

    fun delegateFindUsages(): Array<UsageInfo> = findUsages()

    fun delegatePreprocessUsages(refUsages: Ref<Array<UsageInfo>>): Boolean = preprocessUsages(refUsages)
}
