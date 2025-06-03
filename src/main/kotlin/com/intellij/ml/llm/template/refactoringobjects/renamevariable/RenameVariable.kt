package com.intellij.ml.llm.template.refactoringobjects.renamevariable

import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.utils.PsiUtils
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.impl.source.PsiJavaCodeReferenceElementImpl
import com.intellij.refactoring.RefactoringFactory
import com.intellij.refactoring.rename.RenameHandler
import com.intellij.refactoring.rename.RenameProcessor
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.idea.debugger.getContainingMethod
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
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

    val relatedRenames: MutableList<PsiElement> = mutableListOf()
    init {
        runReadAction{ relatedRenames.addAll(findRelatedElements(oldVarPsi)) }
    }


    override fun performRefactoring(project: Project, editor: Editor, file: PsiFile) {
        super.performRefactoring(project, editor, file)
//        val varPsi = PsiUtils.getVariableFromPsi(file, oldName)
        relatedRenames.forEach { doRename(it, project) }

        try{ doRename(oldVarPsi, project) }
        catch (e: Exception){
            print("Failed to rename inner element")
            if (relatedRenames.isEmpty())
                throw Exception("Failed to rename element")
        }
        reverseRefactoring = getReverseRefactoringObject(project, editor, file)
    }

    private fun doRename(
        psiElement: PsiElement,
        project: Project,
    ) {
        val refactoringFactory = runReadAction { RefactoringFactory.getInstance(project) }
        val rename = runReadAction { refactoringFactory.createRename(psiElement, newName, searchComments, false) }
        val usages = ProgressManager.getInstance().run {
            runReadAction { rename?.findUsages() }
        }
        WriteCommandAction.runWriteCommandAction(project) {
            rename?.doRefactoring(usages)
        }

        //        val processor = RenameProcessor(project, oldVarPsi, newName, searchComments, false)
        //        processor.run()


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

    companion object{
        fun findRelatedElements(psiElement: PsiElement): List<PsiElement>{
            if (psiElement is PsiMethod) {

                return psiElement.findSuperMethods().toList()
//                if (superMethods.isNotEmpty()) {
////                    return superMethods.map { it2 ->
////                        PsiUtils.findAllOverridingMethods(it2)
////                    }.flatten()
//
//                } else {
//                    return emptyList()
//                }
            } else if ((psiElement as? PsiReferenceExpression)!=null) {
                print("found reference.")
                val elements = mutableListOf<PsiElement>()
//                val nameUnwrapped = psiElement.namedUnwrappedElement
//                if (nameUnwrapped!=null && nameUnwrapped !is PsiCompiledElement)
//                    elements.add(nameUnwrapped)

                val resolvedElement = psiElement.resolve()
                if (resolvedElement!=null && resolvedElement !is PsiCompiledElement) {
                    elements.add(resolvedElement)
                }
                return elements
            }
            else if ((psiElement as? PsiJavaCodeReferenceElementImpl !=null)) {
                val resolvedElement = psiElement.resolve()
                if (resolvedElement != null && resolvedElement !is PsiCompiledElement)
                    return listOf(resolvedElement)
                return emptyList()
            }
            else if (psiElement is PsiParameter){
                // find super method's param and rename those.
                val containingMethod = psiElement.getParentOfType<PsiMethod>(true)
                if (containingMethod!=null){
                    return containingMethod.findSuperMethods().map {
                        it.parameterList.parameters.filter { param -> param.name == psiElement.name }
                    }.flatten().reversed()
                }
                return emptyList()
            }
            else {
                return emptyList()
            }
        }
    }
}