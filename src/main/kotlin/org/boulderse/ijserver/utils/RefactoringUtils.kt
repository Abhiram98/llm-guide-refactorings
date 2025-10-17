package org.boulderse.ijserver.utils

import org.boulderse.ijserver.refactoringobjects.AbstractRefactoring
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager

fun refactoringOrderComparator() = Comparator<AbstractRefactoring>{ a, b ->
    when {
        (a.getStartOffset() > b.getStartOffset() && a.getEndOffset() < b.getEndOffset()) -> -1
        (b.getStartOffset() > a.getStartOffset() && b.getEndOffset() < a.getEndOffset()) -> 1
        else -> 0
    }
}
fun getExecutionOrder(refactoringCandidates: List<AbstractRefactoring>): List<AbstractRefactoring> {
    return refactoringCandidates.sortedWith(refactoringOrderComparator())
}

fun isEquivalent(psiA: PsiElement, psiB: PsiElement, project: Project){
    PsiManager.getInstance(project).areElementsEquivalent(psiA, psiB)

}
