package org.boulderse.ijserver.refactoringobjects.movemethod

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.psi.*
import com.intellij.refactoring.move.moveMembers.MoveMembersOptions
import com.intellij.refactoring.move.moveMembers.MoveMembersProcessor
import com.intellij.usageView.UsageInfo
import com.intellij.util.containers.MultiMap

class MyMockMoveMembersOptions(
    private val myTargetClassName: String,
    private val mySelectedMembers: Array<PsiMember>,
) : MoveMembersOptions {
    private var myMemberVisibility: String? = PsiModifier.PUBLIC

    override fun getMemberVisibility(): String? = myMemberVisibility

    override fun makeEnumConstant(): Boolean = true

    fun setMemberVisibility(visibility: String?) {
        myMemberVisibility = visibility
    }

    override fun getSelectedMembers(): Array<PsiMember> = mySelectedMembers

    override fun getTargetClassName(): String = myTargetClassName
}

class MoveStaticMethodValidator(
    project: Project,
    sourceClass: PsiClass,
    targetClass: PsiClass,
    methodToMove: PsiMethod,
) : MoveMembersProcessor(
        project,
        MyMockMoveMembersOptions(targetClass.qualifiedName ?: "", arrayOf(methodToMove)),
    ) {
    override fun preprocessUsages(refUsages: Ref<Array<UsageInfo>>): Boolean = super.preprocessUsages(refUsages)

    override fun findUsages(): Array<UsageInfo> = super.findUsages()

    fun delegateFindUsages(): Array<UsageInfo> = findUsages()

    fun delegatePreprocessUsages(refUsages: Ref<Array<UsageInfo>>): Boolean = preprocessUsages(refUsages)

    override fun showConflicts(
        conflicts: MultiMap<PsiElement, String>,
        usages: Array<out UsageInfo>?,
    ): Boolean = conflicts.isEmpty
}
