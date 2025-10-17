package org.boulderse.ijserver.refactoringobjects.movemethod

import com.intellij.java.refactoring.JavaRefactoringBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.DialogTitle
import com.intellij.psi.*
import com.intellij.psi.util.PsiFormatUtil
import com.intellij.refactoring.HelpID
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.move.MoveCallback
import com.intellij.refactoring.move.moveMembers.MoveMembersDialog
import com.intellij.refactoring.move.moveMembers.MoveMembersImpl
import com.intellij.refactoring.util.CommonRefactoringUtil

object MoveMembersPreConditions {
    /**
     * element should be either not anonymous PsiClass whose members should be moved
     * or PsiMethod of a non-anonymous PsiClass
     * or PsiField of a non-anonymous PsiClass
     * or Inner PsiClass
     */
    fun checkPreconditions(
        project: Project?,
        elements: Array<PsiElement>,
        targetContainer: PsiElement?,
        moveCallback: MoveCallback?
    ): Boolean {
        if (elements.size == 0) {
            return false
        }

        val sourceClass: PsiClass?
        val first = elements[0]
        if (first is PsiMember && first.containingClass != null) {
            sourceClass = first.containingClass
        } else {
            return false
        }

        val preselectMembers: MutableSet<PsiMember> = HashSet()
        for (element in elements) {
            if (element is PsiMember && sourceClass != element.containingClass) {
                val message = RefactoringBundle.getCannotRefactorMessage(
                    RefactoringBundle.message("members.to.be.moved.should.belong.to.the.same.class")
                )
                return false
            }
            if (element is PsiField) {
                if (!element.hasModifierProperty(PsiModifier.STATIC)) {
                    val fieldName = PsiFormatUtil.formatVariable(
                        element,
                        PsiFormatUtil.SHOW_NAME or PsiFormatUtil.SHOW_TYPE or PsiFormatUtil.TYPE_AFTER,
                        PsiSubstitutor.EMPTY
                    )
                    val message = RefactoringBundle.message(
                        "field.0.is.not.static", fieldName,
                        MoveMembersImpl.getRefactoringName()
                    )
                    return false
                }
                preselectMembers.add(element)
            } else if (element is PsiMethod) {
                val methodName = PsiFormatUtil.formatMethod(
                    element,
                    PsiSubstitutor.EMPTY, PsiFormatUtil.SHOW_NAME or PsiFormatUtil.SHOW_PARAMETERS,
                    PsiFormatUtil.SHOW_TYPE
                )
                if (element.isConstructor) {
                    val message = RefactoringBundle.message(
                        "0.refactoring.cannot.be.applied.to.constructors",
                        MoveMembersImpl.getRefactoringName()
                    )
                    return false
                }
                if (!element.hasModifierProperty(PsiModifier.STATIC)) {
                    val message = RefactoringBundle.message(
                        "method.0.is.not.static", methodName,
                        MoveMembersImpl.getRefactoringName()
                    )
                    return false
                }
                preselectMembers.add(element)
            } else if (element is PsiClass) {
                if (!element.hasModifierProperty(PsiModifier.STATIC)) {
                    val message = JavaRefactoringBundle.message(
                        "inner.class.0.is.not.static", element.qualifiedName,
                        MoveMembersImpl.getRefactoringName()
                    )
                    return false
                }
                preselectMembers.add(element)
            }
        }

        if (!CommonRefactoringUtil.checkReadOnlyStatus(project!!, sourceClass!!)) return false

        val initialTargerClass = if (targetContainer is PsiClass) targetContainer else null

        return true
    }

}
