package org.boulderse.ijserver.refactoringobjects

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase
import org.boulderse.ijserver.utils.PsiUtils
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class CodeInspectionFactory<T : PsiElement, T2 : MyRefactoringFactory>(
    override val logicalName: String,
    override val apiFunctionName: String,
    override val APIDocumentation: String, // first param to API call is the start line of element
    val psiClass: Class<out T>,
    val inspection: AbstractBaseJavaLocalInspectionTool, // inspection object, with the right options triggered.
    val refactoringPreview: (PsiElement) -> String,
    val isOnTheFly: Boolean = false,
    val reverseRefactoringFactory: T2?,
) : MyRefactoringFactory {
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

    fun fromStartLoc(
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
                    .findChildrenOfType(elementAtStartLine.parent, psiClass)
                    .filter { it.startOffset in (startOffset..endOffset) }
            if (foundElements.isEmpty()) {
                return@runReadAction null
            }
            if (foundElements[0] != null) {
                val problemsHolder =
                    ProblemsHolder(
                        InspectionManager.getInstance(project),
                        file,
                        isOnTheFly,
                    )
                val visitor = inspection.buildVisitor(problemsHolder, isOnTheFly)
                foundElements[0].accept(visitor)
                if (problemsHolder.hasResults()) {
                    return@runReadAction InspectionBasedRefactoring(
                        startLine,
                        startLine,
                        foundElements[0],
                        problemsHolder,
                        refactoringPreview,
                        reverseRefactoringFactory,
                        isOnTheFly,
                        inspection,
                    )
                } else {
                    return@runReadAction null // cannot be transformed to refactoring object
                }
            }
            return@runReadAction null
        }
    }

    private class InspectionBasedRefactoring(
        override val startLoc: Int,
        override val endLoc: Int,
        val psiElement: PsiElement,
        var problemsHolder: ProblemsHolder,
        val refactoringPreview: (PsiElement) -> String,
        val reverseRefactoringFactory: MyRefactoringFactory?,
        val isOnTheFly: Boolean,
        val inspection: AbstractBaseJavaLocalInspectionTool,
    ) : AbstractRefactoring() {
        //        private var reverseRefactoring:AbstractRefactoring?=null
        override fun performRefactoring(
            project: Project,
            editor: Editor,
            file: PsiFile,
        ) {
            super.performRefactoring(project, editor, file)
            val p0 = problemsHolder.results[0]!!
            WriteCommandAction.runWriteCommandAction(
                project,
                Runnable { p0.fixes?.get(0)?.applyFix(project, p0) },
            )
            reverseRefactoring = getReverseRefactoringObject(project, editor, file)
        }

        override fun isValid(
            project: Project,
            editor: Editor,
            file: PsiFile,
        ): Boolean {
            isValid = psiElement.isPhysical
            return isValid!!
        }

        override fun getRefactoringPreview(): String = refactoringPreview(psiElement)

        override fun getStartOffset(): Int = psiElement.startOffset

        override fun getEndOffset(): Int = psiElement.endOffset

        override fun getReverseRefactoringObject(
            project: Project,
            editor: Editor,
            file: PsiFile,
        ): AbstractRefactoring? {
            // Assumes that the creation function's signature
            // is api_call(start_line)
            if (reverseRefactoringFactory != null) {
                val createdObjectsFromFuncCall =
                    reverseRefactoringFactory.createObjectsFromFuncCall(
                        "${reverseRefactoringFactory.apiFunctionName}($startLoc)",
                        project,
                        editor,
                        file,
                    )
                if (createdObjectsFromFuncCall.isNotEmpty()) {
                    return createdObjectsFromFuncCall[0]
                }
            }
            return null
        }

        override fun recalibrateRefactoring(
            project: Project,
            editor: Editor,
            file: PsiFile,
        ): AbstractRefactoring? {
            if (this.isValid(project, editor, file)) {
                return this
            }

            val foundPsiElement = PsiUtils.searchForPsiElement(file, psiElement)
            if (foundPsiElement != null) {
                problemsHolder =
                    ProblemsHolder(
                        InspectionManager.getInstance(project),
                        file,
                        isOnTheFly,
                    )
                val visitor = inspection.buildVisitor(problemsHolder, isOnTheFly)
                foundPsiElement.accept(visitor)
                if (problemsHolder.hasResults()) {
                    return this
                }
            }
            return null
        }
    }
}
