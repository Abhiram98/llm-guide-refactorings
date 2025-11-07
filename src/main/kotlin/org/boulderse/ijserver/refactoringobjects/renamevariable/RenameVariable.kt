package org.boulderse.ijserver.refactoringobjects.renamevariable

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
import org.boulderse.ijserver.refactoringobjects.AbstractRefactoring
import org.boulderse.ijserver.utils.PsiUtils
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.idea.base.codeInsight.handlers.fixers.startLine
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
    val searchComments: Boolean,
) : AbstractRefactoring(oldVarPsi) {
    val relatedRenames: MutableList<PsiElement> = mutableListOf()

    init {
        runReadAction { relatedRenames.addAll(findRelatedElements(oldVarPsi)) }
    }

    fun startLineWithComments(editor: Editor): Int = oldVarPsi.startLine(editor.document)

    fun getResolvedElement(): PsiElement? {
        if ((oldVarPsi as? PsiReferenceExpression) != null) {
            val refElement = (oldVarPsi as PsiReferenceExpression).resolve()?.namedUnwrappedElement
            return refElement
        }
        if ((oldVarPsi as? PsiJavaCodeReferenceElementImpl != null)) {
            val refElement = (oldVarPsi as PsiJavaCodeReferenceElementImpl).resolve()?.namedUnwrappedElement
            return refElement
        }
        return null
    }

    fun getResolvedStartLine(): Int? {
        if ((oldVarPsi as? PsiReferenceExpression) != null) {
            print("found reference.")
            val refElement = (oldVarPsi as PsiReferenceExpression).resolve()?.namedUnwrappedElement
            return refElement?.containingFile?.fileDocument?.let { refElement.startLine(it) }
        }
        if ((oldVarPsi as? PsiJavaCodeReferenceElementImpl != null)) {
            val refElement = (oldVarPsi as PsiJavaCodeReferenceElementImpl).resolve()?.namedUnwrappedElement
            return refElement?.containingFile?.fileDocument?.let { refElement.startLine(it) }
        }
        return null
    }

    fun getResolvedFilePath(): String? {
        if ((oldVarPsi as? PsiReferenceExpression) != null) {
            print("found reference.")
            val refElement = (oldVarPsi as PsiReferenceExpression).resolve()?.namedUnwrappedElement
            refElement?.containingFile?.fileDocument?.let { refElement.startLine(it) }
            return refElement?.containingFile?.virtualFile?.path
        }
        if ((oldVarPsi as? PsiJavaCodeReferenceElementImpl != null)) {
            val refElement = (oldVarPsi as PsiJavaCodeReferenceElementImpl).resolve()?.namedUnwrappedElement
            return refElement?.containingFile?.virtualFile?.path
        }

        return null
    }

    override fun performRefactoring(
        project: Project,
        editor: Editor,
        file: PsiFile,
    ) {
        super.performRefactoring(project, editor, file)
//        val varPsi = PsiUtils.getVariableFromPsi(file, oldName)
        relatedRenames.forEach {
            try {
                doRename(it, project)
            } catch (e: Exception) {
                print("failed to do one of the related renames : $it")
            }
        }

        try {
            doRename(oldVarPsi, project)
        } catch (e: Exception) {
            print("Failed to rename inner element")
            if (relatedRenames.isEmpty()) {
                throw Exception("Failed to rename element")
            }
        }
        reverseRefactoring = getReverseRefactoringObject(project, editor, file)
    }

    private fun doRename(
        psiElement: PsiElement,
        project: Project,
    ) {
        val refactoringFactory = runReadAction { RefactoringFactory.getInstance(project) }
        val rename = runReadAction { refactoringFactory.createRename(psiElement, newName, searchComments, false) }
        val usages =
            ProgressManager.getInstance().run {
                runReadAction { rename?.findUsages() }
            }
        WriteCommandAction.runWriteCommandAction(project) {
            rename?.doRefactoring(usages)
        }

        //        val processor = RenameProcessor(project, oldVarPsi, newName, searchComments, false)
        //        processor.run()
    }

    override fun isValid(
        project: Project,
        editor: Editor,
        file: PsiFile,
    ): Boolean {
        // Valid if oldName exists and newName doesn't
        isValid = PsiUtils.getVariableFromPsi(outerPsiElement, oldName) != null &&
            PsiUtils.getVariableFromPsi(outerPsiElement, newName) == null &&
            oldVarPsi.isPhysical
        return isValid!!
    }

    override fun getRefactoringPreview(): String = "Rename ${PsiUtils.getElementTypeStr(getResolvedElement()?:oldVarPsi)} to $newName"

    override fun getStartOffset(): Int = oldVarPsi.startOffset

    override fun getEndOffset(): Int = oldVarPsi.endOffset

    override fun fetchRootPsi(): PsiElement? = getResolvedElement() ?: oldVarPsi

    override fun getReverseRefactoringObject(
        project: Project,
        editor: Editor,
        file: PsiFile,
    ): AbstractRefactoring? =
        RenameVariableFactory.fromOldNewName(
            project,
            outerPsiElement,
            newName,
            oldName,
        )

    override fun recalibrateRefactoring(
        project: Project,
        editor: Editor,
        file: PsiFile,
    ): AbstractRefactoring? {
        if (isValid == true) {
            return this
        }

        val variablePsi = PsiUtils.getVariableFromPsi(outerPsiElement, oldName)
        if (variablePsi != null) {
            oldVarPsi = variablePsi
            return this
        } else {
            val variablePsiInFile = PsiUtils.getVariableFromPsi(file, oldName)
            if (variablePsiInFile != null) {
                oldVarPsi = variablePsiInFile
                return this
            }
        }
        return null
    }

    companion object {
        fun findRelatedElements(psiElement: PsiElement): List<PsiElement> {
            if (psiElement is PsiMethod) {
                val superMethods = psiElement.findSuperMethods().toList()
                if (superMethods.isNotEmpty()) {
                    return superMethods
                        .map { it2 ->
                            PsiUtils.findAllOverridingMethods(it2)
                        }.flatten()
                        .toSet()
                        .toList()
                } else {
                    return PsiUtils.findAllOverridingMethods(psiElement)
                }
            } else if ((psiElement as? PsiReferenceExpression) != null) {
                print("found reference.")
                val elements = mutableListOf<PsiElement>()
//                val nameUnwrapped = psiElement.namedUnwrappedElement
//                if (nameUnwrapped!=null && nameUnwrapped !is PsiCompiledElement)
//                    elements.add(nameUnwrapped)

                val resolvedElement = psiElement.resolve()
                if (resolvedElement != null && resolvedElement !is PsiCompiledElement) {
                    elements.add(resolvedElement)
                }
                return elements
            } else if ((psiElement as? PsiJavaCodeReferenceElementImpl != null)) {
                val resolvedElement = psiElement.resolve()
                if (resolvedElement != null && resolvedElement !is PsiCompiledElement) {
                    return listOf(resolvedElement)
                }
                return emptyList()
            } else if (psiElement is PsiParameter) {
                // find super method's param and rename those.
                val containingMethod = psiElement.getParentOfType<PsiMethod>(true)
                if (containingMethod != null) {
                    return containingMethod
                        .findSuperMethods()
                        .map {
                            PsiUtils.findAllOverridingMethods(it)
                        }.flatten()
                        .toSet()
                        .map {
                            it.parameterList.parameters.filter { param -> param.name == psiElement.name }
                        }.flatten()
                        .filter { it !is PsiCompiledElement }
                        .reversed()
                }
                return emptyList()
            } else {
                return emptyList()
            }
        }
    }
}
