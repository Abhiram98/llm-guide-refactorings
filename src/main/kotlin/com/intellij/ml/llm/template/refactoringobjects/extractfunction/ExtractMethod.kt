package com.intellij.ml.llm.template.refactoringobjects.extractfunction

import com.intellij.lang.java.JavaLanguage
import com.intellij.ml.llm.template.models.FunctionNameProvider
import com.intellij.ml.llm.template.models.MyMethodExtractor
import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.refactoringobjects.extractfunction.customextractors.MyInplaceExtractionHelper
import com.intellij.ml.llm.template.utils.PsiUtils
import com.intellij.ml.llm.template.utils.isCandidateExtractable
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.keymap.impl.IdeKeyEventDispatcher
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.extractMethod.newImpl.inplace.InplaceMethodExtractor
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.refactoring.introduce.extractFunction.ExtractKotlinFunctionHandler
import org.jetbrains.kotlin.idea.util.executeEnterHandler
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class ExtractMethod(
    override val startLoc: Int,
    override val endLoc: Int,
    val newFuncName: String,
    val leftPsi: PsiElement,
    val rightPsi: PsiElement,
    val candidateType: EfCandidateType
) : AbstractRefactoring() {

//    var efCandidate: EFCandidate? =null

    companion object{
        const val REFACTORING_NAME = "Extract Method"

    }

    override fun performRefactoring(project: Project, editor: Editor, file: PsiFile) {
        super.performRefactoring(project, editor, file)
        editor.selectionModel.setSelection(this.getStartOffset(), this.getEndOffset())
        invokeExtractFunction(newFuncName, project, editor, file)
        reverseRefactoring = getReverseRefactoringObject(project, editor, file)
    }

    override fun getStartOffset(): Int {
        return leftPsi.startOffset
    }

    override fun getEndOffset(): Int {
        return rightPsi.endOffset
    }

    override fun getReverseRefactoringObject(project: Project, editor: Editor, file: PsiFile): AbstractRefactoring? {
        val objects = InlineMethodFactory.fromMethodName(file, editor, newFuncName)
        if (objects.isNotEmpty())
            return objects[0]
        return null
    }

    override fun recalibrateRefactoring(project: Project, editor: Editor, file: PsiFile): AbstractRefactoring? {
        if (isValid==true)
            return this

        val newLeft = if (!leftPsi.isPhysical) {
            PsiUtils.searchForPsiElement(file, leftPsi)
        } else {
            leftPsi
        }

        val newRight = if (!rightPsi.isPhysical) {
            PsiUtils.searchForPsiElement(file, rightPsi)
        } else {
            rightPsi
        }
        if (newLeft!=null && newRight!=null){
            return ExtractMethod(startLoc, endLoc,  newFuncName, newLeft, newRight, candidateType)
        }
        return null
    }


    override fun isValid(project: Project, editor: Editor, file: PsiFile): Boolean {
        val candidate = getEFCandidate()
        isValid =  runReadAction {
             isCandidateExtractable(
                candidate, editor, file, allowWholeBody=true
            )
        } && leftPsi.isPhysical && rightPsi.isPhysical
        return isValid!!
    }

    fun getEFCandidate(): EFCandidate {
        val candidate = EFCandidate(
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

    override fun getRefactoringPreview(): String {
        return "${REFACTORING_NAME} lines($startLoc, $endLoc): $newFuncName"
    }


    private fun invokeExtractFunction(newFunctionName: String, project: Project, editor: Editor?, file: PsiFile?) {
        val functionNameProvider = FunctionNameProvider(newFunctionName)
        when (file?.language) {
            JavaLanguage.INSTANCE -> {
                MyMethodExtractor.invokeOnElements(
                    project, editor, file, findSelectedPsiElements(editor, file), FunctionNameProvider(newFunctionName)
                )
            }

            KotlinLanguage.INSTANCE -> {
                val dataContext = (editor as EditorEx).dataContext
                val allContainersEnabled = false
                val inplaceExtractionHelper = MyInplaceExtractionHelper(allContainersEnabled, functionNameProvider)
                ExtractKotlinFunctionHandler(allContainersEnabled, inplaceExtractionHelper).invoke(
                    project, editor, file, dataContext
                )
            }
        }
    }

    private fun findSelectedPsiElements(editor: Editor?, file: PsiFile?): Array<PsiElement> {
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
        val result = selectedElements.filter {
            it.textRange.startOffset >= startOffset && it.textRange.endOffset <= endOffset
        }.toTypedArray()
        return result
    }


}