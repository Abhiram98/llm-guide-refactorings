package com.intellij.ml.llm.template.refactoringobjects.movemethod

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.psi.*
import com.intellij.refactoring.move.moveMembers.MoveMembersOptions
import com.intellij.refactoring.move.moveMembers.MoveMembersProcessor
import com.intellij.usageView.UsageInfo
import com.intellij.util.containers.MultiMap


class MyMockMoveMembersOptions(private val myTargetClassName: String, private val mySelectedMembers: Array<PsiMember>) :
    MoveMembersOptions {
    private var myMemberVisibility: String? = PsiModifier.PUBLIC

    override fun getMemberVisibility(): String? {
        return myMemberVisibility
    }

    override fun makeEnumConstant(): Boolean {
        return true
    }

    fun setMemberVisibility(visibility: String?) {
        myMemberVisibility = visibility
    }

    override fun getSelectedMembers(): Array<PsiMember> {
        return mySelectedMembers
    }

    override fun getTargetClassName(): String {
        return myTargetClassName
    }
}

class MoveStaticMethodValidator(
    project: Project,
    sourceClass: PsiClass,
    targetClass: PsiClass,
    methodToMove: PsiMethod
): MoveMembersProcessor(
    project,
    MyMockMoveMembersOptions(targetClass.qualifiedName?:"", arrayOf(methodToMove))
) {
    override fun preprocessUsages(refUsages: Ref<Array<UsageInfo>>): Boolean {
        return super.preprocessUsages(refUsages)
    }

    override fun findUsages(): Array<UsageInfo> {
        return super.findUsages()
    }

    fun delegateFindUsages(): Array<UsageInfo>{
        return findUsages()
    }

    fun delegatePreprocessUsages(refUsages: Ref<Array<UsageInfo>>): Boolean{
        return preprocessUsages(refUsages)
    }

    override fun showConflicts(conflicts: MultiMap<PsiElement, String>, usages: Array<out UsageInfo>?): Boolean {
        return conflicts.isEmpty
    }
}