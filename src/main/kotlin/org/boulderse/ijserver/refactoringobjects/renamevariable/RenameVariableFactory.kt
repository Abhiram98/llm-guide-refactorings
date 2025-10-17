package org.boulderse.ijserver.refactoringobjects.renamevariable

import org.boulderse.ijserver.refactoringobjects.AbstractRefactoring
import org.boulderse.ijserver.refactoringobjects.MyRefactoringFactory
import org.boulderse.ijserver.utils.MethodSignature
import org.boulderse.ijserver.utils.Parameter
import org.boulderse.ijserver.utils.PsiUtils
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.idea.base.codeInsight.handlers.fixers.endLine
import org.jetbrains.kotlin.idea.base.codeInsight.handlers.fixers.startLine
import org.jetbrains.kotlin.idea.base.psi.getLineNumber
import org.jetbrains.kotlin.idea.hierarchy.overrides.isOverrideHierarchyElement
import org.jetbrains.kotlin.idea.search.declarationsSearch.forEachOverridingMethod
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments

class RenameVariableFactory {
    companion object: MyRefactoringFactory {
        override fun createObjectsFromFuncCall(
            funcCall: String,
            project: Project,
            editor: Editor,
            file: PsiFile
        ): List<AbstractRefactoring> {

            val params = getParamsFromFuncCall(funcCall)
            return fromOldNewName(project, editor, file, getStringFromParam(params[1]), getStringFromParam(params[0]))
        }

        fun fromOldNewName(
            project: Project,
            editor: Editor,
            file: PsiFile,
            oldName: String,
            newName: String,
        ): List<AbstractRefactoring> {
            val functionPsi: PsiElement =
                runReadAction {
                    PsiUtils.getParentFunctionOrNull(editor, language = file.language)
                        ?: file.getChildOfType<PsiClass>()
                }!!

            val renameObj = fromOldNewName(project, functionPsi, oldName, newName)
            if (renameObj != null)
                return listOf(renameObj)
            return listOf()
        }

        fun fromOldNewNameAll(
            project: Project,
            editor: Editor,
            file: PsiFile,
            oldName: String,
            newName: String,
        ): List<AbstractRefactoring> {
            val functionPsi: PsiElement = file.getChildOfType<PsiClass>()!!

            return fromOldNewNameAll(project, editor, functionPsi, oldName, newName)
        }

        override val logicalName: String
            get() = "Rename Variable"
        override val apiFunctionName: String
            get() = "rename_variable"
        // rename_variable("x", "count")
        override val APIDocumentation: String
            get() = """def rename_variable(old_variable_name, new_variable_name):
    ""${'"'}
    Renames occurrences of a variable within the scope of a function or method.

    This function is intended to refactor code by replacing all occurrences of the variable named `old_variable_name`
    with the new variable name `new_variable_name` within the scope of the function or method where it is called.

    Parameters:
    - old_variable_name (str): The name of the variable to be renamed.
    - new_variable_name (str): The new name for the variable.
    ""${'"'}
                    """.trimIndent()

        fun fromOldNewName(project: Project,
                           outerPsiElement: PsiElement,
                           oldName:String,
                           newName: String): AbstractRefactoring?{
            val varPsi = runReadAction { PsiUtils.getVariableFromPsi(outerPsiElement, oldName) }
            if (varPsi!=null)
                return RenameVariable(
                    runReadAction{ varPsi.getLineNumber() },
                    runReadAction{ varPsi.getLineNumber() },
                    oldName, newName, varPsi,
                    outerPsiElement,
                    false
                )
            return null
        }

        fun fromOldNewNameAll(project: Project,
                              editor: Editor,
                           outerPsiElement: PsiElement,
                           oldName:String,
                           newName: String): List<AbstractRefactoring>{
            val varPsi = runReadAction { PsiUtils.getAllElementsOfName(outerPsiElement, oldName) }

            val newElements = runReadAction{
                varPsi
                    .filter{
                        it !is PsiCompiledElement
                    }
            }
            return newElements
                    .map {
                        RenameVariable(
                                runReadAction{ PsiUtils.getStartLine(it) },
                                runReadAction{ PsiUtils.getEndLine(it) },
                                oldName, newName, it, outerPsiElement, false
                        )
                    }
        }


        fun fromMethodOldNewName(
            project: Project,
            outerClass: PsiClass,
            methodName: String,
            oldName: String,
            newName: String
        ): AbstractRefactoring?{
            val outerPsiElement: PsiMethod = PsiUtils.getMethodNameFromClass(outerClass, methodName) ?: return null
            val varPsi = runReadAction { PsiUtils.getVariableFromPsi(outerPsiElement, oldName) }
            if (varPsi!=null)
                return RenameVariable(
                    runReadAction{ varPsi.getLineNumber() },
                    runReadAction{ varPsi.getLineNumber() },
                    oldName, newName, varPsi,
                    outerPsiElement,
                    false
                )
            return null
        }

        fun renameMethod(methodName: String, outerClass: PsiClass,
                         newName: String, oldMethodSignature: MethodSignature): AbstractRefactoring?{
            val methodPsi = PsiUtils.getMethodWithSignatureFromClass(outerClass, oldMethodSignature)
            if (methodPsi!=null){
                return RenameVariable(
                    runReadAction{ methodPsi.getLineNumber() },
                    runReadAction{ methodPsi.getLineNumber() },
                    methodName, newName, methodPsi, outerClass, false
                )
            }
            return null
        }

        fun renameParameter(methodSignature: MethodSignature, outerClass: PsiClass, oldParameter: Parameter, newParameter: Parameter): AbstractRefactoring?{
            val methodPsi = PsiUtils.getMethodWithSignatureFromClass(outerClass, methodSignature)
            if (methodPsi!=null){
                val oldParamPsi = PsiUtils.getMethodParameter(methodPsi, oldParameter)
                if (oldParamPsi!=null)
                    return RenameVariable(
                        runReadAction{ methodPsi.getLineNumber() },
                        runReadAction{ methodPsi.getLineNumber() },
                        oldParameter.name, newParameter.name, oldParamPsi, outerClass, false
                    )
            }
            return null
        }

        fun fromOldNewNameAllMatching(
            project: Project,
            editor: Editor,
            file: PsiFile,
            oldName: String,
            newName: String
        ): List<RenameVariable> {
            val outerPsiElement: PsiElement = file.getChildOfType<PsiClass>()!!
            val varPsi = runReadAction { PsiUtils.getAllElementsWithNameMatching(outerPsiElement, oldName) }

            val newElements = runReadAction{
                varPsi
                    .filter{
                        it !is PsiCompiledElement
                    }
            }
            return newElements
                .map {
                    RenameVariable(
                        runReadAction{ PsiUtils.getStartLine(it) },
                        runReadAction{ PsiUtils.getEndLine(it) },
                        oldName, newName, it, outerPsiElement, false
                    )
                }
        }

    }


}