package org.boulderse.ijserver.refactoringobjects.extractfunction.customextractors

import com.intellij.codeInsight.template.impl.TemplateManagerImpl
import com.intellij.java.refactoring.JavaRefactoringBundle
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.command.impl.FinishMarkAction
import com.intellij.openapi.command.impl.StartMarkAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.extractMethod.newImpl.inplace.EditorState
import com.intellij.refactoring.extractMethod.newImpl.inplace.ExtractMethodTemplateBuilder
import com.intellij.refactoring.extractMethod.newImpl.inplace.InplaceExtractUtils
import com.intellij.refactoring.extractMethod.newImpl.inplace.TemplateField
import org.jetbrains.annotations.Nls
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.refactoring.KotlinNamesValidator
import org.jetbrains.kotlin.idea.refactoring.introduce.extractFunction.EXTRACT_FUNCTION
import org.jetbrains.kotlin.idea.refactoring.introduce.extractFunction.ExtractKotlinFunctionHandler
import org.jetbrains.kotlin.idea.refactoring.introduce.extractableSubstringInfo
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.*
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile

class MyInplace2(allContainersEnabled: Boolean): ExtractKotlinFunctionHandler.InplaceExtractionHelper(
    allContainersEnabled
) {
    override fun configureAndRun(
        project: Project,
        editor: Editor,
        descriptorWithConflicts: ExtractableCodeDescriptorWithConflicts,
        onFinish: (ExtractionResult) -> Unit
    ) {
        super.configureAndRun(project, editor, descriptorWithConflicts, onFinish)
    }

}


class MyInplaceExtractionHelper(private val myAllContainersEnabled: Boolean, private val functionName: String? = null) : ExtractKotlinFunctionHandler.InplaceExtractionHelper(myAllContainersEnabled) {
    override fun configureAndRun(
        project: Project,
        editor: Editor,
        descriptorWithConflicts: ExtractableCodeDescriptorWithConflicts,
        onFinish: (ExtractionResult) -> Unit
    ) {
        val activeTemplateState = TemplateManagerImpl.getTemplateState(editor)
        if (activeTemplateState != null) {
            activeTemplateState.gotoEnd(true)
            ExtractKotlinFunctionHandler(allContainersEnabled, ExtractKotlinFunctionHandler.InteractiveExtractionHelper)
                .invoke(project, editor, descriptorWithConflicts.descriptor.extractionData.originalFile, null)
        }
        var suggestedNames = descriptorWithConflicts.descriptor.suggestedNames.takeIf { it.isNotEmpty() } ?: listOf("extracted")
        if (functionName != null) {
            suggestedNames = listOf(functionName) + suggestedNames
        }
        val descriptor = descriptorWithConflicts.descriptor.copy(suggestedNames = suggestedNames)
        val elements = descriptor.extractionData.originalElements
        val file = descriptor.extractionData.originalFile
        val callTextRange = TextRange(rangeOf(elements.first()).startOffset, rangeOf(elements.last()).endOffset)

        val commonParent = descriptor.extractionData.commonParent
        val container = commonParent.takeIf { commonParent != elements.firstOrNull() } ?: commonParent.parent
        val callRangeProvider: () -> TextRange? = createSmartRangeProvider(container, callTextRange)
        val editorState = EditorState(project, editor)
        val disposable = Disposer.newDisposable()
        WriteCommandAction.writeCommandAction(project).run<Throwable> {
            val startMarkAction = StartMarkAction.start(editor, project, EXTRACT_FUNCTION)
            Disposer.register(disposable) { FinishMarkAction.finish(project, editor, startMarkAction) }
        }
        fun afterFinish(extraction: ExtractionResult){
            val callRange: TextRange = callRangeProvider.invoke() ?: throw IllegalStateException()
            val callIdentifier = findSingleCallExpression(file, callRange)?.calleeExpression ?: throw IllegalStateException()
            val methodIdentifier = extraction.declaration.nameIdentifier ?: throw IllegalStateException()
            val methodRange = extraction.declaration.textRange
            val methodOffset = extraction.declaration.navigationElement.textRange.endOffset
            val callOffset = callIdentifier.textRange.endOffset
            val preview = InplaceExtractUtils.createPreview(editor, methodRange, methodOffset, callRange, callOffset)
            Disposer.register(disposable, preview)
            val templateField = TemplateField(callIdentifier.textRange, listOf(methodIdentifier.textRange))
                .withCompletionNames(descriptor.suggestedNames)
                .withCompletionHint(getDialogAdvertisement())
                .withValidation { variableRange ->
                    val error = getIdentifierError(file, variableRange)
                    if (error != null) {
                        InplaceExtractUtils.showErrorHint(editor, variableRange.endOffset, error)
                    }
                    error == null
                }
            ExtractMethodTemplateBuilder(editor, EXTRACT_FUNCTION)
                .enableRestartForHandler(ExtractKotlinFunctionHandler::class.java)
                .onBroken {
                    editorState.revert()
                }
                .onSuccess {
                    processDuplicates(extraction.duplicateReplacers, file.project, editor)
                }
                .disposeWithTemplate(disposable)
                .createTemplate(file, listOf(templateField))
            onFinish(extraction)
        }
        try {
            val configuration = ExtractionGeneratorConfiguration(descriptor, ExtractionGeneratorOptions.DEFAULT)
            doRefactor(configuration, ::afterFinish)
        } catch (e: Throwable) {
            Disposer.dispose(disposable)
            throw e
        }
    }

    @Nls
    private fun getDialogAdvertisement():  String {
        val shortcut = KeymapUtil.getPrimaryShortcut("ExtractFunction") ?: throw IllegalStateException("Action is not found")
        return RefactoringBundle.message("inplace.refactoring.advertisement.text", KeymapUtil.getShortcutText(shortcut))
    }

    private fun findSingleCallExpression(file: KtFile, range: TextRange?): KtCallExpression? {
        if (range == null) return null
        val container = PsiTreeUtil.findCommonParent(file.findElementAt(range.startOffset), file.findElementAt(range.endOffset))
        val callExpressions = PsiTreeUtil.findChildrenOfType(container, KtCallExpression::class.java)
        return callExpressions.singleOrNull { it.textRange in range }
    }
}
