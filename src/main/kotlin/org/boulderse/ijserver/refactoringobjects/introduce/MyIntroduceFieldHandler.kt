package org.boulderse.ijserver.refactoringobjects.introduce

import org.boulderse.ijserver.server.ExtractFieldParams
import org.boulderse.ijserver.utils.PsiUtils
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.refactoring.introduceField.IntroduceFieldHandler
import org.jetbrains.kotlin.idea.base.codeInsight.handlers.fixers.startLine
import kotlin.math.abs

class MyIntroduceFieldHandler(
    val project: Project,
    val editor: Editor,
    val localVariable: PsiLocalVariable,
    val containingClass: PsiClass,
    val newName: String,
    val makeStatic: Boolean
): IntroduceFieldHandler() {


    fun expressionToField(expression: PsiExpression){
        invokeImpl(project, expression, editor)
    }

    fun variableToField(){
        invokeImpl(project, localVariable, editor)
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
        return Settings(newName, expr, occurrences, true,
            makeStatic, makeStatic, // make is both static and final
            if (makeStatic) InitializationPlace.IN_FIELD_DECLARATION else InitializationPlace.IN_CURRENT_METHOD,
            "private",
            localVariable,
            type,
            false,
            containingClass
            , false, false);
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

    companion object{
        fun fromVariable(params: ExtractFieldParams, project: Project, editor: Editor, file: PsiFile): () -> Boolean {

            val containingClass = (file as PsiJavaFile).classes[0]

            val varNames = PsiUtils
                .getAllVariableFromPsi(file, params.variableName)
                .filter {
                    it is PsiLocalVariable
                }
            val varNamesSorted = if (params.lineNum!=null){
                varNames.sortedBy { abs(it.startLine(editor.document) - params.lineNum) }
            }else{
                varNames
            }

            if (varNames.isEmpty()){
                throw Exception("Could not find local variable with that name.")
            }

            return {
                MyIntroduceFieldHandler(project, editor,
                    containingClass = containingClass,
                    localVariable = varNamesSorted[0] as PsiLocalVariable,
                    newName = params.newFieldName,
                    makeStatic = params.makeStatic
                ).variableToField()
                true
            }
        }
    }
}