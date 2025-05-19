package com.intellij.ml.llm.template.refactoringobjects.renamevariable

import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.utils.PsiUtils
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.RefactoringFactory
import com.intellij.refactoring.rename.RenameHandler
import com.intellij.refactoring.rename.RenameProcessor
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class RenameVariable(
    override val startLoc: Int,
    override val endLoc: Int,
    val oldName: String,
    val newName: String,
    var oldVarPsi: PsiElement,
    val outerPsiElement: PsiElement,
    val searchComments: Boolean

): AbstractRefactoring() {


    override fun performRefactoring(project: Project, editor: Editor, file: PsiFile) {
        super.performRefactoring(project, editor, file)
//        val varPsi = PsiUtils.getVariableFromPsi(file, oldName)
        val refactoringFactory = runReadAction{ RefactoringFactory.getInstance(project) }
        val rename = runReadAction{ refactoringFactory.createRename(oldVarPsi, newName, searchComments, false) }
        val usages = ProgressManager.getInstance().run {
            runReadAction { rename?.findUsages() }
        }
        WriteCommandAction.runWriteCommandAction(project) {
            rename?.doRefactoring(usages)
        }

//        val processor = RenameProcessor(project, oldVarPsi, newName, searchComments, false)
//        processor.run()

        reverseRefactoring = getReverseRefactoringObject(project, editor, file)
    }

    override fun isValid(project: Project, editor: Editor, file: PsiFile): Boolean {
        // Valid if oldName exists and newName doesn't
        isValid = PsiUtils.getVariableFromPsi(outerPsiElement, oldName)!=null
                && PsiUtils.getVariableFromPsi(outerPsiElement, newName)==null
                && oldVarPsi.isPhysical
        return isValid!!
    }

    override fun getRefactoringPreview(): String {
        return "${RenameVariableFactory.logicalName} $oldName -> $newName"
    }

    override fun getStartOffset(): Int {
        return oldVarPsi.startOffset
    }

    override fun getEndOffset(): Int {
        return oldVarPsi.endOffset
    }

    override fun getReverseRefactoringObject(project: Project, editor: Editor, file: PsiFile): AbstractRefactoring? {
        return RenameVariableFactory.fromOldNewName(
            project, outerPsiElement,
            newName, oldName
        )
    }

    override fun recalibrateRefactoring(project: Project, editor: Editor, file: PsiFile): AbstractRefactoring? {
        if (isValid==true)
            return this

        val variablePsi = PsiUtils.getVariableFromPsi(outerPsiElement, oldName)
        if (variablePsi!=null) {
            oldVarPsi = variablePsi
            return this
        }else{
            val variablePsiInFile = PsiUtils.getVariableFromPsi(file, oldName)
            if (variablePsiInFile!=null){
                oldVarPsi = variablePsiInFile
                return this
            }
        }
        return null
    }
}