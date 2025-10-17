package com.intellij.ml.llm.template.refactoringobjects.stringbuilder

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.refactoringobjects.MyRefactoringFactory
import com.intellij.ml.llm.template.utils.PsiUtils
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.refactoring.suggested.startOffset
import com.siyeh.ig.performance.StringConcatenationInLoopsInspection
import com.siyeh.ipp.concatenation.ReplaceConcatenationWithStringBufferIntention
import org.jetbrains.kotlin.idea.base.codeInsight.handlers.fixers.endLine
import org.jetbrains.kotlin.idea.base.codeInsight.handlers.fixers.startLine
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

class StringBuilderRefactoringFactory {
    companion object: MyRefactoringFactory{
        val preview = fun(element: PsiElement): String{
            return "Use String Builder"
        }
        override fun createObjectsFromFuncCall(
            funcCall: String,
            project: Project,
            editor: Editor,
            file: PsiFile
        ): List<AbstractRefactoring> {
            val params = getParamsFromFuncCall(funcCall)
            val varName = getStringFromParam(params[0])

            val outerPsi: PsiElement? =
                runReadAction {
                    file.getChildOfType<PsiClass>()
//                    PsiUtils.getParentFunctionOrNull(editor, language = file.language)?:
//                    PsiUtils.getParentClassOrNull(editor, language = file.language)?:

                }
            val varPsiElements = PsiUtils.getVariableAndReferencesFromPsi(outerPsi, varName)

            val refactoringObjects: MutableList<AbstractRefactoring> = mutableListOf()

            for (psiElement in varPsiElements){
                if (psiElement is PsiLocalVariable){
                    if (psiElement.initializer is PsiPolyadicExpression){
                        val psiPolyadicExpression = psiElement.initializer as PsiPolyadicExpression
                        val startLoc = psiPolyadicExpression.startLine(editor.document)
                        val endLoc = psiPolyadicExpression.endLine(editor.document)
                        refactoringObjects.add(
                            StringBuilder4ConcatRefactoring(startLoc, endLoc, psiPolyadicExpression)
                        )
                    }
                }
                val parentElement = psiElement.parent
                if (psiElement is PsiReferenceExpression && parentElement is PsiAssignmentExpression){
                    val sbConcat = StringConcatenationInLoopsInspection()
                    val problemsHolder = ProblemsHolder(InspectionManager.getInstance(project), file, false)
                    val visitor = sbConcat.buildVisitor(problemsHolder, false)
                    runReadAction { parentElement.accept(visitor) }
                    if(problemsHolder.hasResults()){
                        refactoringObjects.add(
                            StringBuilder4AssignInLoop(
                                parentElement.startLine(editor.document),
                                parentElement.endLine(editor.document),
                                parentElement as PsiAssignmentExpression,
                                problemsHolder
                            )
                        )
                    }
                }
            }

            return refactoringObjects
        }

        override val logicalName: String
            get() = "Use String Builder"
        override val apiFunctionName: String
            get() = "use_string_builder"
        override val APIDocumentation: String
            get() = """def use_string_builder(variable_name):
            ""${'"'}
            Refactors code to use a string builder for string concatenation involving the specified variable.
        
            This function identifies instances where the specified string variable is concatenated multiple times
            and refactors the code to use a string builder (or an equivalent approach) for more efficient string 
            concatenation. It assumes that the necessary updates to the source code are handled externally.
        
            Parameters:
            - variable_name (str): The name of the string variable to refactor for efficient concatenation.
            ""${'"'}
            """.trimIndent()

        class StringBuilder4ConcatRefactoring(
            override val startLoc: Int,
            override val endLoc: Int,
            var psiPolyadicExpression: PsiPolyadicExpression
        ) : AbstractRefactoring() {
            override fun performRefactoring(project: Project, editor: Editor, file: PsiFile) {
                super.performRefactoring(project, editor, file)
                val sbConcat = ReplaceConcatenationWithStringBufferIntention()
                WriteCommandAction.runWriteCommandAction(project,
                    Runnable {
                        sbConcat.invoke(psiPolyadicExpression)
                    })
                reverseRefactoring = getReverseRefactoringObject(project, editor, file)
            }

            override fun isValid(project: Project, editor: Editor, file: PsiFile): Boolean {
                isValid = psiPolyadicExpression.isPhysical
                return isValid!!
            }

            override fun getRefactoringPreview(): String {
                return "Use String Builder"
            }

            override fun getStartOffset(): Int {
                return psiPolyadicExpression.startOffset
            }

            override fun getEndOffset(): Int {
                return psiPolyadicExpression.endOffset
            }

            override fun getReverseRefactoringObject(
                project: Project,
                editor: Editor,
                file: PsiFile
            ): AbstractRefactoring? {
                return null
            }

            override fun recalibrateRefactoring(project: Project, editor: Editor, file: PsiFile): AbstractRefactoring? {
                if (this.isValid(project, editor, file))
                    return this

                val foundPsiElement = PsiUtils.searchForPsiElement(file, psiPolyadicExpression)
                if (foundPsiElement!=null && foundPsiElement is PsiPolyadicExpression) {
                    psiPolyadicExpression = foundPsiElement
                    return this
                }
                return null
            }

        }

        class StringBuilder4AssignInLoop(
            override val startLoc: Int,
            override val endLoc: Int,
            var psiAssignmentExpression: PsiAssignmentExpression,
            var problemsHolder: ProblemsHolder
        ) : AbstractRefactoring() {
            override fun performRefactoring(project: Project, editor: Editor, file: PsiFile) {
                super.performRefactoring(project, editor, file)
                val problem = problemsHolder.results[0]!!
                val fix = problem.fixes!![0]

                WriteCommandAction.runWriteCommandAction(project,
                    Runnable { fix.applyFix(project, problem) })
                reverseRefactoring = getReverseRefactoringObject(project, editor, file)
            }

            override fun isValid(project: Project, editor: Editor, file: PsiFile): Boolean {
                isValid = psiAssignmentExpression.isPhysical
                return isValid!!
            }

            override fun getRefactoringPreview(): String {
                return "Use String Builder"
            }

            override fun getStartOffset(): Int {
                return psiAssignmentExpression.startOffset
            }

            override fun getEndOffset(): Int {
                return psiAssignmentExpression.endOffset
            }

            override fun getReverseRefactoringObject(
                project: Project,
                editor: Editor,
                file: PsiFile
            ): AbstractRefactoring? {
                return null
            }

            override fun recalibrateRefactoring(project: Project, editor: Editor, file: PsiFile): AbstractRefactoring? {
                if (this.isValid(project, editor, file))
                    return this

                val foundPsiElement = PsiUtils.searchForPsiElement(file, psiAssignmentExpression)
                if (foundPsiElement!=null && foundPsiElement is PsiAssignmentExpression) {
                    psiAssignmentExpression = foundPsiElement
                    val sbConcat = StringConcatenationInLoopsInspection()
                    problemsHolder = ProblemsHolder(
                        InspectionManager.getInstance(project), file, false
                    )
                    val visitor = sbConcat.buildVisitor(problemsHolder, false)
                    runReadAction { foundPsiElement.accept(visitor) }
                    if (problemsHolder.hasResults())
                        return this
                }
                return null
            }

        }
    }
}