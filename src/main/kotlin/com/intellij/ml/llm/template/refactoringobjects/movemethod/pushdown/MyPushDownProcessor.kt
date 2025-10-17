package com.intellij.ml.llm.template.refactoringobjects.movemethod.pushdown

import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.refactoring.classMembers.MemberInfoBase
import com.intellij.refactoring.memberPushDown.PushDownProcessor
import com.intellij.refactoring.util.DocCommentPolicy
import com.intellij.usageView.UsageInfo


class MyPushDownProcessor <MemberInfo : MemberInfoBase<Member>, Member : PsiElement, Klass : PsiElement>(
            val kClass: Klass,
            val memberInfo: List<MemberInfo>,
            val docCommentPolicy: DocCommentPolicy
        ) : PushDownProcessor<MemberInfo, Member, Klass>(kClass, memberInfo, docCommentPolicy) {

            fun delegatePerformRefactoring(): Boolean{
                val usages = findUsages()
                val usages_ref = Ref<Array<UsageInfo>> (usages)

                if (preprocessUsages(usages_ref)){
                    performRefactoring(usages)
                    return true
                }
                return false
            }


}