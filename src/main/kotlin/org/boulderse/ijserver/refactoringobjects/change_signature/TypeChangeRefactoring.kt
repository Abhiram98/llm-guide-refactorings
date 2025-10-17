package org.boulderse.ijserver.refactoringobjects.change_signature

import org.boulderse.ijserver.server.TypeChangeParams
import org.boulderse.ijserver.utils.PsiUtils
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.impl.scopes.ModulesScope
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.refactoring.typeMigration.TypeMigrationProcessor
import com.intellij.refactoring.typeMigration.TypeMigrationRules
import com.intellij.usageView.UsageInfo
import com.intellij.util.Functions
import org.jetbrains.kotlin.idea.base.codeInsight.handlers.fixers.startLine
import org.jetbrains.kotlin.idea.base.util.module
import kotlin.math.abs

class TypeChangeRefactoring(
    val project: Project,
    val elementToChange: PsiElement,
    val newType: PsiType,
    val rules: TypeMigrationRules
    ) : TypeMigrationProcessor(
        project, arrayOf(elementToChange), Functions.constant(newType), rules, true
    ) {

    fun doChange(){
        this.run()
    }

    override fun preprocessUsages(refUsages: Ref<Array<UsageInfo>>): Boolean {
        // return true regardless, because we want the change to be carried out.
        prepareSuccessful()
        return true
    }


    companion object{
        fun createFromParams(params: TypeChangeParams, file: PsiFile, editor: Editor, project: Project): TypeChangeRefactoring{

            val varNames = PsiUtils
                .getAllVariableFromPsi(file, params.variableName)
                .filter { it !is PsiMethod}


            val varNamesSorted = if (params.lineNum!=null){
                varNames.sortedBy { abs(it.startLine(editor.document) - params.lineNum) }
            }else{
                varNames
            }

            val newType = PsiType.getTypeByName(
                params.newType, project, GlobalSearchScope.projectScope(project))
            val myRules = TypeMigrationRules(project)
            myRules.setBoundScope(
                ModulesScope(setOf(file.module!!), project)
            )
            return TypeChangeRefactoring(project, varNamesSorted[0], newType, myRules)
        }
    }

}