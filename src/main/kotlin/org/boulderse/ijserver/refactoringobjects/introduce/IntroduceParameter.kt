package org.boulderse.ijserver.refactoringobjects.introduce

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLocalVariable
import com.intellij.psi.PsiMethod
import com.intellij.refactoring.introduceParameter.IntroduceParameterProcessor
import com.intellij.refactoring.introduceVariable.IntroduceVariableBase
import com.intellij.refactoring.suggested.startOffset
import it.unimi.dsi.fastutil.ints.IntList
import org.boulderse.ijserver.refactoringobjects.AbstractRefactoring
import org.jetbrains.kotlin.psi.psiUtil.endOffset

class IntroduceParameter(
    override val startLoc: Int,
    override val endLoc: Int,
    val paramName: String,
    val methosPsi: PsiMethod,
    val expression: PsiExpression,
    val localVariable: PsiLocalVariable?,
) : AbstractRefactoring(expression) {
    override fun isValid(
        project: Project,
        editor: Editor,
        file: PsiFile,
    ): Boolean = true

    override fun getRefactoringPreview(): String = "introduce parameter $paramName from ${expression.text}"

    override fun getStartOffset(): Int = expression.startOffset

    override fun getEndOffset(): Int = expression.endOffset

    override fun getReverseRefactoringObject(
        project: Project,
        editor: Editor,
        file: PsiFile,
    ): AbstractRefactoring? {
        TODO("Not yet implemented")
    }

    override fun recalibrateRefactoring(
        project: Project,
        editor: Editor,
        file: PsiFile,
    ): AbstractRefactoring? {
        TODO("Not yet implemented")
    }

    override fun performRefactoring(
        project: Project,
        editor: Editor,
        file: PsiFile,
    ) {
        super.performRefactoring(project, editor, file)
        IntroduceParameterProcessor(
            project,
            methosPsi,
            methosPsi,
            expression,
            expression,
            localVariable,
            true,
            paramName,
            IntroduceVariableBase.JavaReplaceChoice.ALL,
            1,
            false,
            false,
            false,
            expression.type,
            IntList.of(),
        ).run()
    }
}
