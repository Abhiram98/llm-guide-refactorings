package com.intellij.ml.llm.template.refactoringobjects.introduce

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.refactoring.introduceField.IntroduceFieldHandler

class MyIntroduceFieldHandler(
    val project: Project,
    val editor: Editor
): IntroduceFieldHandler() {


    fun expressionToField(expression: PsiExpression){
        invokeImpl(project, expression, editor)
    }

    fun variableToField(localVariable: PsiLocalVariable){
        invokeImpl(project, localVariable, editor)
    }

//    override fun showRefactoringDialog(
//        project: Project?,
//        editor: Editor?,
//        parentClass: PsiClass?,
//        expr: PsiExpression?,
//        type: PsiType?,
//        occurrences: Array<out PsiExpression>?,
//        anchorElement: PsiElement?,
//        anchorElementIfAll: PsiElement?
//    ): Settings {
//        val settings = super.showRefactoringDialog(
//            project,
//            editor,
//            parentClass,
//            expr,
//            type,
//            occurrences,
//            anchorElement,
//            anchorElementIfAll
//        )
//        if (settings==null)
//            return settings
//
//        return Settings(
//
//
//        )
//    }
}