package org.boulderse.ijserver.refactoringobjects.conditionals

import com.intellij.codeInsight.daemon.impl.quickfix.ConvertSwitchToIfIntention
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiSwitchStatement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase
import org.boulderse.ijserver.refactoringobjects.AbstractRefactoring
import org.boulderse.ijserver.refactoringobjects.MyRefactoringFactory
import org.boulderse.ijserver.utils.PsiUtils
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class Switch2IfFactory {
    companion object : MyRefactoringFactory {
        override fun createObjectsFromFuncCall(
            funcCall: String,
            project: Project,
            editor: Editor,
            file: PsiFile,
        ): List<AbstractRefactoring> {
            val params = getParamsFromFuncCall(funcCall)
            val startLine = params[0].toInt()

            val refObj =
                this.fromStartLoc(
                    startLine,
                    project,
                    editor,
                    file,
                )
            if (refObj != null) {
                return listOf(refObj)
            }
            return listOf()
        }

        private fun fromStartLoc(
            startLine: Int,
            project: Project,
            editor: Editor,
            file: PsiFile,
        ): AbstractRefactoring? {
            return runReadAction {
                val elementAtStartLine =
                    PsiUtilBase.getElementAtOffset(
                        file,
                        editor.document.getLineStartOffset(startLine - 1),
                    )

                val startOffset = editor.document.getLineStartOffset(startLine - 1)
                val endOffset = editor.document.getLineStartOffset(startLine)
                val foundElements =
                    PsiTreeUtil
                        .findChildrenOfType(elementAtStartLine.parent, PsiSwitchStatement::class.java)
                        .filter { it.startOffset in (startOffset..endOffset) }
                if (foundElements.isEmpty()) {
                    return@runReadAction null
                }
                if (foundElements[0] != null) {
                    return@runReadAction Switch2IfRefactoring(startLine, startLine, foundElements[0])
                }
                return@runReadAction null
            }
        }

        override val logicalName: String
            get() = "Convert Switch Statement to If Statement"
        override val apiFunctionName: String
            get() = "convert_switch2if"
        override val APIDocumentation: String
            get() =
                """
                def convert_switch2if(line_start):
                ""${'"'}
                Converts switch-case statements to if-else statements where applicable.

                This function refactors code by replacing switch-case statements with equivalent if-else statements,
                starting from the specified line number `line_start`. It assumes that the necessary updates to the source code
                are handled externally.

                Parameters:
                - line_start (int): The line number from which to start searching for switch-case statements to convert. Must be a positive integer.
                ""${'"'}
                """.trimIndent()
    }

    private class Switch2IfRefactoring(
        override val startLoc: Int,
        override val endLoc: Int,
        var switchStatement: PsiSwitchStatement,
    ) : AbstractRefactoring() {
        override fun performRefactoring(
            project: Project,
            editor: Editor,
            file: PsiFile,
        ) {
            super.performRefactoring(project, editor, file)
            WriteCommandAction.runWriteCommandAction(
                project,
                Runnable { ConvertSwitchToIfIntention.doProcessIntention(switchStatement) },
            )
            reverseRefactoring = getReverseRefactoringObject(project, editor, file)
        }

        override fun isValid(
            project: Project,
            editor: Editor,
            file: PsiFile,
        ): Boolean {
            isValid = switchStatement.isPhysical
            return isValid!!
        }

        override fun getRefactoringPreview(): String = "Convert Switch to If"

        override fun getStartOffset(): Int = switchStatement.startOffset

        override fun getEndOffset(): Int = switchStatement.endOffset

        override fun getReverseRefactoringObject(
            project: Project,
            editor: Editor,
            file: PsiFile,
        ): AbstractRefactoring? {
            val factory = If2Switch.factory
            val createdObjectsFromFuncCall =
                factory.createObjectsFromFuncCall(
                    "${factory.apiFunctionName}($startLoc)",
                    project,
                    editor,
                    file,
                )
            if (createdObjectsFromFuncCall.isNotEmpty()) {
                return createdObjectsFromFuncCall[0]
            }
            return null
        }

        override fun recalibrateRefactoring(
            project: Project,
            editor: Editor,
            file: PsiFile,
        ): AbstractRefactoring? {
            if (isValid == true) {
                return this
            }
            val recalibratedByLineNumber =
                Switch2IfFactory.fromStartLoc(
                    startLoc,
                    project,
                    editor,
                    file,
                )
            if (recalibratedByLineNumber != null) {
                return recalibratedByLineNumber
            } else {
                val foundPsiElement = PsiUtils.searchForPsiElement(file, switchStatement)
                if (foundPsiElement != null && foundPsiElement is PsiSwitchStatement) {
                    switchStatement = foundPsiElement
                    return this
                }
            }
            return null
        }
    }
}
