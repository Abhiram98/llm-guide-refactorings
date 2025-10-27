package org.boulderse.ijserver.refactoringobjects.extractfunction

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.refactoring.inline.InlineMethodProcessor
import org.boulderse.ijserver.refactoringobjects.AbstractRefactoring
import org.boulderse.ijserver.refactoringobjects.MyRefactoringFactory
import org.boulderse.ijserver.utils.MethodSignature
import org.boulderse.ijserver.utils.PsiUtils
import org.jetbrains.kotlin.idea.base.codeInsight.handlers.fixers.endLine
import org.jetbrains.kotlin.idea.base.codeInsight.handlers.fixers.startLine
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class InlineMethodFactory {
    companion object : MyRefactoringFactory {
        override fun createObjectsFromFuncCall(
            funcCall: String,
            project: Project,
            editor: Editor,
            file: PsiFile,
        ): List<AbstractRefactoring> {
            val params = getParamsFromFuncCall(funcCall)
            val methodName = getStringFromParam(params[0])

            return fromMethodName(file, editor, methodName)
        }

        fun fromMethodName(
            file: PsiFile,
            editor: Editor,
            methodName: String,
        ): List<AbstractRefactoring> {
            val classPsi = runReadAction { file.getChildOfType<PsiClass>() }
            val methodPsi = runReadAction { PsiUtils.getMethodNameFromClass(classPsi, methodName) }
            if (methodPsi != null) {
                return runReadAction {
                    return@runReadAction listOf(
                        InlineMethodRefactoring(
                            methodPsi.startLine(editor.document),
                            methodPsi.endLine(editor.document),
                            methodPsi,
                        ),
                    )
                }
            }
            return emptyList()
        }

        fun fromMethodSignature(
            file: PsiFile,
            editor: Editor,
            methodSignature: MethodSignature,
        ): List<AbstractRefactoring> {
            val classPsi = runReadAction { file.getChildOfType<PsiClass>() }
            val methodPsi =
                runReadAction {
                    PsiUtils.getMethodWithSignatureFromClass(classPsi, methodSignature)
                }
            if (methodPsi != null) {
                return runReadAction {
                    return@runReadAction listOf(
                        InlineMethodRefactoring(
                            methodPsi.startLine(editor.document),
                            methodPsi.endLine(editor.document),
                            methodPsi,
                        ),
                    )
                }
            }
            return emptyList()
        }

        override val logicalName: String
            get() = "Inline Method"
        override val apiFunctionName: String
            get() = "inline_method"
        override val APIDocumentation: String
            get() =
                "def inline_method(method_name):\n" +
                    "    \"\"\"\n" +
                    "    Inlines a method by replacing its calls with the method's body.\n" +
                    "\n" +
                    "    This function refactors code by replacing all calls to the specified method with the body of the method itself,\n" +
                    "    effectively eliminating the method. It assumes that the necessary updates to the source code are handled externally.\n" +
                    "\n" +
                    "    Parameters:\n" +
                    "    - method_name (str): The name of the method to be inlined.\"\"\"\n"
    }
}

class InlineMethodRefactoring(
    override val startLoc: Int,
    override val endLoc: Int,
    var psiMethod: PsiMethod,
) : AbstractRefactoring() {
    override fun performRefactoring(
        project: Project,
        editor: Editor,
        file: PsiFile,
    ) {
        InlineMethodProcessor(project, psiMethod, null, editor, false).run()
        reverseRefactoring = getReverseRefactoringObject(project, editor, file)
    }

    override fun isValid(
        project: Project,
        editor: Editor,
        file: PsiFile,
    ): Boolean {
        isValid = psiMethod.isPhysical
        return isValid!!
    }

    override fun getRefactoringPreview(): String = "Inline method ${psiMethod.name}"

    override fun getStartOffset(): Int = psiMethod.startOffset

    override fun getEndOffset(): Int = psiMethod.endOffset

    override fun getReverseRefactoringObject(
        project: Project,
        editor: Editor,
        file: PsiFile,
    ): AbstractRefactoring? {
        return null // TODO: call extract method here.
    }

    override fun recalibrateRefactoring(
        project: Project,
        editor: Editor,
        file: PsiFile,
    ): AbstractRefactoring? {
        if (isValid == true) {
            return this
        }

        val foundMethodPsi = PsiUtils.searchForPsiElement(file, psiMethod)
        if (foundMethodPsi != null && foundMethodPsi is PsiMethod) {
            psiMethod = foundMethodPsi
            return this
        }
        return null
    }
}
