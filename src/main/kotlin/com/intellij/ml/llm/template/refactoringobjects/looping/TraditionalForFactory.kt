package com.intellij.ml.llm.template.refactoringobjects.looping

import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.refactoringobjects.MyRefactoringFactory
import com.intellij.ml.llm.template.utils.PsiUtils
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiForStatement
import com.intellij.psi.PsiForeachStatement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase
import com.siyeh.ipp.forloop.ReplaceForEachLoopWithIndexedForLoopIntention
import org.jetbrains.kotlin.idea.editor.fixers.endLine
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class TraditionalForFactory {
    companion object: MyRefactoringFactory{
        override fun createObjectsFromFuncCall(
            funcCall: String,
            project: Project,
            editor: Editor,
            file: PsiFile
        ): List<AbstractRefactoring> {
            val params = getParamsFromFuncCall(funcCall)
            val startLine = params[0].toInt()

            val refObj = this.fromStartLoc(
                startLine, project, editor, file
            )
            if (refObj!=null)
                return listOf(refObj)
            return listOf()
        }

        fun fromStartLoc(startLoc: Int, project: Project, editor: Editor, file: PsiFile): UseTraditionalForLoop?{
            val elementAtStartLine =
                PsiUtilBase.getElementAtOffset(
                    file, editor.document.getLineStartOffset(startLoc - 1)
                )

            val startOffset = editor.document.getLineStartOffset(startLoc - 1)
            val endOffset = editor.document.getLineStartOffset(startLoc)
            val foundElements = PsiTreeUtil
                .findChildrenOfType(elementAtStartLine.parent, PsiForeachStatement::class.java)
                .filter { it.startOffset in (startOffset..endOffset) }

            if (foundElements.isEmpty() || foundElements[0]==null){
                return null
            }
            val psiForeachStatement = foundElements[0]!!
            return UseTraditionalForLoop(
                startLoc, psiForeachStatement.endLine(editor.document), psiForeachStatement
            )

        }

        override val logicalName: String
            get() = "Use Traditional For Loop"
        override val apiFunctionName: String
            get() = "use_traditional_forloop"
        override val APIDocumentation: String
            get() = """def use_traditional_forloop(line_start):
    ""${'"'}
    Converts a an enhanced for-loop to a traditional for-loop to where applicable.

    This function is intended to refactor code by replacing enhanced for-loops (also known as 
    "foreach" loops) with traditional for-loops where applicable, starting from the specified line number `line_start`. It assumes that the necessary 
    updates to the source code are handled externally.

    Parameters:
    - line_start (int): The line number from which to start searching for the enhanced for-loops to convert. Must be a positive integer.
    ""${'"'}""".trimIndent()

    }

    class UseTraditionalForLoop(
        override val startLoc: Int, override val endLoc: Int,
        var psiForeachStatement: PsiForeachStatement
    ) : AbstractRefactoring() {
        override fun isValid(project: Project, editor: Editor, file: PsiFile): Boolean {
            isValid = psiForeachStatement.isPhysical
            return isValid!!
        }

        override fun getRefactoringPreview(): String {
            return "Replace with traditional for loop"
        }

        override fun getStartOffset(): Int {
            return psiForeachStatement.startOffset
        }

        override fun getEndOffset(): Int {
            return psiForeachStatement.endOffset
        }

        override fun getReverseRefactoringObject(
            project: Project,
            editor: Editor,
            file: PsiFile
        ): AbstractRefactoring? {
            return EnhancedForFactory.factory.fromStartLoc(startLoc, project, editor, file)
        }

        override fun recalibrateRefactoring(project: Project, editor: Editor, file: PsiFile): AbstractRefactoring? {
            if (isValid==true)
                return this
            val foundForEach = PsiUtils.searchForPsiElement(file, psiForeachStatement)
            if (foundForEach!=null && foundForEach is PsiForeachStatement) {
                psiForeachStatement = foundForEach
                return this
            }
            return null

        }

        override fun performRefactoring(project: Project, editor: Editor, file: PsiFile) {
            super.performRefactoring(project, editor, file)
            runWriteAction { ReplaceForEachLoopWithIndexedForLoopIntention().processIntention(psiForeachStatement.children[0]) }
            reverseRefactoring = getReverseRefactoringObject(project, editor, file)
        }

    }

}