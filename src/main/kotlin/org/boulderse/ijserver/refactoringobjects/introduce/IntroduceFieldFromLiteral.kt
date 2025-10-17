package org.boulderse.ijserver.refactoringobjects.introduce

import org.boulderse.ijserver.server.ExtractFieldFromLiteralParams
import org.boulderse.ijserver.utils.PsiUtils
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.refactoring.introduceField.IntroduceFieldHandler
import org.jetbrains.kotlin.idea.base.codeInsight.handlers.fixers.startLine
import kotlin.math.abs

class IntroduceFieldFromLiteral(
    val project: Project,
    val editor: Editor,
    val expression: PsiExpression,
    val containingClass: PsiClass,
    val newName: String,
    val makeStatic: Boolean
): IntroduceFieldHandler() {


    fun expressionToField(){
        invokeImpl(project, expression, editor)
    }

    override fun getParentClass(initializerExpression: PsiExpression): PsiClass? {
        // override to get the top level class.
        return (initializerExpression.containingFile as? PsiJavaFile)?.classes?.get(0)
    }


    override fun showRefactoringDialog(
        project: Project?,
        editor: Editor?,
        parentClass: PsiClass?,
        expr: PsiExpression?,
        type: PsiType?,
        occurrences: Array<out PsiExpression>?,
        anchorElement: PsiElement?,
        anchorElementIfAll: PsiElement?
    ): Settings {

        val initPlace = if (makeStatic) InitializationPlace.IN_FIELD_DECLARATION else InitializationPlace.IN_CURRENT_METHOD

        return Settings(newName, expr, occurrences, true,
            makeStatic, makeStatic,
            initPlace, "private",
            null,
            type,
            false,
            containingClass
            , false, false);
    }


    companion object{
        fun fromVariable(params: ExtractFieldFromLiteralParams, project: Project, editor: Editor, file: PsiFile): () -> Boolean {

            val containingClass = (file as PsiJavaFile).classes[0]

            val matchingElements = PsiUtils
                .getElementMatchingText(file, params.literalValue)
                .filter {
                    it is PsiExpression
                }
            val varNamesSorted = if (params.lineNum!=null){
                matchingElements.sortedBy { abs(it.startLine(editor.document) - params.lineNum) }
            }else{
                matchingElements
            }

            if (matchingElements.isEmpty()){
                throw Exception("Could not find expression with the value ${params.literalValue}.")
            }

            return {
                IntroduceFieldFromLiteral(project, editor,
                    containingClass = containingClass,
                    expression = varNamesSorted[0] as PsiExpression,
                    newName = params.newFieldName,
                    makeStatic = params.makeStatic
                ).expressionToField()
                true
            }
        }
    }
}