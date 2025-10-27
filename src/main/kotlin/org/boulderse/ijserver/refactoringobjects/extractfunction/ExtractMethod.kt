package org.boulderse.ijserver.refactoringobjects.extractfunction

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.keymap.impl.IdeKeyEventDispatcher
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.extractMethod.newImpl.MethodExtractor
import org.boulderse.ijserver.refactoringobjects.AbstractRefactoring
import org.boulderse.ijserver.refactoringobjects.extractfunction.customextractors.MyInplaceExtractionHelper
import org.boulderse.ijserver.utils.PsiUtils
import org.boulderse.ijserver.utils.isCandidateExtractable
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.refactoring.introduce.extractFunction.ExtractKotlinFunctionHandler
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import javax.swing.SwingUtilities.invokeAndWait

class ExtractMethod(
    override val startLoc: Int,
    override val endLoc: Int,
    val newFuncName: String,
    val leftPsi: PsiElement,
    val rightPsi: PsiElement,
    val candidateType: EfCandidateType,
) : AbstractRefactoring() {
    //    var efCandidate: EFCandidate? =null

    companion object {
        const val REFACTORING_NAME = "Extract Method"
    }

    override fun performRefactoring(
        project: Project,
        editor: Editor,
        file: PsiFile,
    ) {
        super.performRefactoring(project, editor, file)
        invokeAndWait { editor.selectionModel.setSelection(this.getStartOffset(), this.getEndOffset()) }
        invokeExtractFunction(newFuncName, project, editor, file)
        reverseRefactoring = runReadAction { getReverseRefactoringObject(project, editor, file) }
    }

    override fun getStartOffset(): Int = leftPsi.startOffset

    override fun getEndOffset(): Int = rightPsi.endOffset

    override fun getReverseRefactoringObject(
        project: Project,
        editor: Editor,
        file: PsiFile,
    ): AbstractRefactoring? {
        val objects = InlineMethodFactory.fromMethodName(file, editor, newFuncName)
        if (objects.isNotEmpty()) {
            return objects[0]
        }
        return null
    }

    override fun recalibrateRefactoring(
        project: Project,
        editor: Editor,
        file: PsiFile,
    ): AbstractRefactoring? {
        if (isValid == true) {
            return this
        }

        val newLeft =
            if (!leftPsi.isPhysical) {
                PsiUtils.searchForPsiElement(file, leftPsi)
            } else {
                leftPsi
            }

        val newRight =
            if (!rightPsi.isPhysical) {
                PsiUtils.searchForPsiElement(file, rightPsi)
            } else {
                rightPsi
            }
        if (newLeft != null && newRight != null) {
            return ExtractMethod(startLoc, endLoc, newFuncName, newLeft, newRight, candidateType)
        }
        return null
    }

    override fun isValid(
        project: Project,
        editor: Editor,
        file: PsiFile,
    ): Boolean {
        val candidate = getEFCandidate()
        isValid = runReadAction {
            isCandidateExtractable(
                candidate,
                editor,
                file,
                allowWholeBody = true,
            )
        } && leftPsi.isPhysical && rightPsi.isPhysical
        return isValid!!
    }

    fun getEFCandidate(): EFCandidate {
        val candidate =
            EFCandidate(
                functionName = this.newFuncName,
                offsetStart = this.getStartOffset(),
                offsetEnd = this.getEndOffset(),
                lineStart = this.startLoc,
                lineEnd = this.endLoc,
            ).also {
                it.efSuggestion = EFSuggestion(this.newFuncName, this.startLoc, this.endLoc)
                it.type = candidateType
            }
        return candidate
    }

    override fun getRefactoringPreview(): String = "${REFACTORING_NAME} lines($startLoc, $endLoc): $newFuncName"

    private fun invokeExtractFunction(
        newFunctionName: String,
        project: Project,
        editor: Editor?,
        file: PsiFile?,
    ) {
        when (file?.language) {
            JavaLanguage.INSTANCE -> {
                MethodExtractor().doExtract(
                    file,
                    TextRange(getStartOffset(), getEndOffset()),
                )
            }

            KotlinLanguage.INSTANCE -> {
                val dataContext = (editor as EditorEx).dataContext
                val allContainersEnabled = false
                val inplaceExtractionHelper = MyInplaceExtractionHelper(allContainersEnabled, newFuncName)
                ExtractKotlinFunctionHandler(allContainersEnabled, inplaceExtractionHelper).invoke(
                    project,
                    editor,
                    file,
                    dataContext,
                )
            }
        }
    }

    private fun findSelectedPsiElements(
        editor: Editor?,
        file: PsiFile?,
    ): Array<PsiElement> {
        if (editor == null) {
            return emptyArray()
        }
        val selectionModel = editor.selectionModel
        val startOffset = selectionModel.selectionStart
        val endOffset = selectionModel.selectionEnd

        val startElement = file?.findElementAt(startOffset)
        val endElement = file?.findElementAt(if (endOffset > 0) endOffset - 1 else endOffset)

        if (startElement == null || endElement == null) {
            return emptyArray()
        }

        val commonParent = PsiTreeUtil.findCommonParent(startElement, endElement) ?: return emptyArray()

        val selectedElements = PsiTreeUtil.findChildrenOfType(commonParent, PsiElement::class.java)
        val result =
            selectedElements
                .filter {
                    it.textRange.startOffset >= startOffset && it.textRange.endOffset <= endOffset
                }.toTypedArray()
        return result
    }
}
