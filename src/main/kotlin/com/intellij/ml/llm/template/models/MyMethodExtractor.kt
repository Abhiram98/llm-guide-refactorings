// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.ml.llm.template.models

import com.intellij.codeInsight.Nullability
import com.intellij.codeInsight.highlighting.HighlightManager
import com.intellij.ide.util.PropertiesComponent
import com.intellij.java.refactoring.JavaRefactoringBundle
import com.intellij.ml.llm.template.refactoringobjects.extractfunction.customextractors.MyInplaceMethodExtractor
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.wm.WindowManager
import com.intellij.psi.*
import com.intellij.psi.util.PsiEditorUtil
import com.intellij.refactoring.HelpID
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.extractMethod.ExtractMethodDialog
import com.intellij.refactoring.extractMethod.ExtractMethodHandler
import com.intellij.refactoring.extractMethod.newImpl.*
import com.intellij.refactoring.extractMethod.newImpl.ExtractMethodHelper.addSiblingAfter
import com.intellij.refactoring.extractMethod.newImpl.ExtractMethodHelper.findEditorSelection
import com.intellij.refactoring.extractMethod.newImpl.ExtractMethodHelper.guessMethodName
import com.intellij.refactoring.extractMethod.newImpl.ExtractMethodHelper.replacePsiRange
import com.intellij.refactoring.extractMethod.newImpl.ExtractMethodPipeline.findAllOptionsToExtract
import com.intellij.refactoring.extractMethod.newImpl.ExtractMethodPipeline.selectOptionWithTargetClass
import com.intellij.refactoring.extractMethod.newImpl.ExtractMethodPipeline.withFilteredAnnotations
import com.intellij.refactoring.extractMethod.newImpl.inplace.ExtractMethodPopupProvider
import com.intellij.refactoring.extractMethod.newImpl.inplace.extractInDialog
import com.intellij.refactoring.extractMethod.newImpl.structures.ExtractOptions
import com.intellij.refactoring.listeners.RefactoringEventData
import com.intellij.refactoring.listeners.RefactoringEventListener
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.refactoring.util.ConflictsUtil
import com.intellij.util.containers.MultiMap
import org.jetbrains.annotations.NonNls
import java.util.concurrent.CompletableFuture

data class ExtractedElements(val callElements: List<PsiElement>, val method: PsiMethod)
data class FunctionName (val name: String)
class FunctionNameProvider(private val functionName: String) {
    fun getFunctionName() : FunctionName {
        return FunctionName(functionName)
    }
}

class MyMethodExtractor (private val functionNameProvider: FunctionNameProvider? = null)  {
    fun doExtract(file: PsiFile, range: TextRange) {
        val editor = PsiEditorUtil.findEditor(file) ?: return
        if (EditorSettingsExternalizable.getInstance().isVariableInplaceRenameEnabled) {
            val activeExtractor = MyInplaceMethodExtractor.getActiveExtractor(editor)
            if (activeExtractor != null) {
                activeExtractor.restartInDialog()
            } else {
                findAndSelectExtractOption(editor, file, range)?.thenApply { options ->
                    runInplaceExtract(editor, range, options)
                }
            }
        } else {
            findAndSelectExtractOption(editor, file, range)?.thenApply { options ->
                extractInDialog(options.targetClass, options.elements, "", options.isStatic)
            }
        }
    }

    fun findAndSelectExtractOption(editor: Editor, file: PsiFile, range: TextRange): CompletableFuture<ExtractOptions>? {
        try {
            if (!CommonRefactoringUtil.checkReadOnlyStatus(file.project, file)) return null
            val elements = ExtractSelector().suggestElementsToExtract(file, range)
            if (elements.isEmpty()) {
                throw ExtractException(RefactoringBundle.message("selected.block.should.represent.a.set.of.statements.or.an.expression"), file)
            }
            val allOptionsToExtract: List<ExtractOptions> = computeWithAnalyzeProgress<List<ExtractOptions>, ExtractException>(file.project) {
                findAllOptionsToExtract(elements)
            }
            return selectOptionWithTargetClass(editor, allOptionsToExtract)
        }
        catch (e: ExtractException) {
            val message = JavaRefactoringBundle.message("extract.method.error.prefix") + " " + (e.message ?: "")
            CommonRefactoringUtil.showErrorHint(file.project, editor, message, ExtractMethodHandler.getRefactoringName(), HelpID.EXTRACT_METHOD)
            showError(editor, e.problems)
            return null
        }
    }

    private fun <T, E: Exception> computeWithAnalyzeProgress(project: Project, throwableComputable: ThrowableComputable<T, E>): T {
        return ProgressManager.getInstance().run(object : Task.WithResult<T, E>(project,
            JavaRefactoringBundle.message("dialog.title.analyze.code.fragment.to.extract"), true) {
            override fun compute(indicator: ProgressIndicator): T {
                return ReadAction.compute(throwableComputable)
            }
        })
    }

    private fun runInplaceExtract(editor: Editor, range: TextRange, options: ExtractOptions){
        val project = options.project
        val popupSettings = createInplaceSettingsPopup(options)
        val guessedNames = suggestSafeMethodNames(options)
        val methodName = guessedNames.first()
        val suggestedNames = guessedNames.takeIf { it.size > 1 }.orEmpty()
        executeRefactoringCommand(project) {
            val inplaceExtractor = MyInplaceMethodExtractor(editor, range, options.targetClass, popupSettings, methodName)
            inplaceExtractor.extractAndRunTemplate(LinkedHashSet(suggestedNames))
        }
    }

    val ExtractOptions.targetClass: PsiClass
        get() = anchor.containingClass ?: throw IllegalStateException()

    private fun suggestSafeMethodNames(options: ExtractOptions): List<String> {
        var unsafeNames = guessMethodName(options)
        if (functionNameProvider != null) {
            unsafeNames = listOf(functionNameProvider.getFunctionName().name) + unsafeNames
        }
        val safeNames = unsafeNames.filterNot { name -> hasConflicts(options.copy(methodName = name)) }
        if (safeNames.isNotEmpty()) return safeNames

        val baseName = unsafeNames.firstOrNull() ?: "extracted"
        val generatedNames = sequenceOf(baseName) + generateSequence(1) { seed -> seed + 1 }.map { number -> "$baseName$number" }
        return generatedNames.filterNot { name -> hasConflicts(options.copy(methodName = name)) }.take(1).toList()
    }

    private fun hasConflicts(options: ExtractOptions): Boolean {
        val (_, method) = prepareRefactoringElements(options)
        val conflicts = MultiMap<PsiElement, String>()
        ConflictsUtil.checkMethodConflicts(options.anchor.containingClass, null, method, conflicts)
        return ! conflicts.isEmpty
    }

    private fun createInplaceSettingsPopup(options: ExtractOptions): ExtractMethodPopupProvider {
        val isStatic = options.isStatic
        val analyzer = CodeFragmentAnalyzer(options.elements)
        val optionsWithStatic = ExtractMethodPipeline.withForcedStatic(analyzer, options)
        val makeStaticAndPassFields = optionsWithStatic?.inputParameters?.size != options.inputParameters.size
        val showStatic = ! isStatic && optionsWithStatic != null
        val hasAnnotation = options.dataOutput.nullability != Nullability.UNKNOWN && options.dataOutput.type !is PsiPrimitiveType
        val annotationAvailable = ExtractMethodHelper.isNullabilityAvailable(options)
        return ExtractMethodPopupProvider(
            annotateDefault = if (hasAnnotation && annotationAvailable) needsNullabilityAnnotations(options.project) else null,
            makeStaticDefault = if (showStatic) !makeStaticAndPassFields else null,
            staticPassFields = makeStaticAndPassFields
        )
    }

    fun executeRefactoringCommand(project: Project, command: () -> Unit) {
        CommandProcessor.getInstance().executeCommand(project, command, ExtractMethodHandler.getRefactoringName(), null)
    }

    fun replaceElements(sourceElements: List<PsiElement>, callElements: List<PsiElement>, anchor: PsiMember, method: PsiMethod): ExtractedElements {
        return WriteAction.compute<ExtractedElements, Throwable> {
            val addedMethod = anchor.addSiblingAfter(method) as PsiMethod
            val replacedCallElements = replacePsiRange(sourceElements, callElements)
            ExtractedElements(replacedCallElements, addedMethod)
        }
    }

    fun extractMethod(extractOptions: ExtractOptions): ExtractedElements {
        val elementsToExtract = prepareRefactoringElements(extractOptions)
        return replaceElements(extractOptions.elements, elementsToExtract.callElements, extractOptions.anchor, elementsToExtract.method)
    }

    fun doTestExtract(
        doRefactor: Boolean,
        editor: Editor,
        isConstructor: Boolean?,
        isStatic: Boolean?,
        returnType: PsiType?,
        newNameOfFirstParam: String?,
        targetClass: PsiClass?,
        @PsiModifier.ModifierConstant visibility: String?,
        vararg disabledParameters: Int
    ): Boolean {
        val project = editor.project ?: return false
        val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return false
        val range = ExtractMethodHelper.findEditorSelection(editor) ?: return false
        val elements = ExtractSelector().suggestElementsToExtract(file, range)
        if (elements.isEmpty()) throw ExtractException("Nothing to extract", file)
        val analyzer = CodeFragmentAnalyzer(elements)
        val allOptionsToExtract = findAllOptionsToExtract(elements)
        var options = allOptionsToExtract.takeIf { targetClass != null }?.find { option -> option.anchor.containingClass == targetClass }
            ?: allOptionsToExtract.find { option -> option.anchor.containingClass !is PsiAnonymousClass }
            ?: allOptionsToExtract.first()
        options = options.copy(methodName = "newMethod")
        if (isConstructor != options.isConstructor){
            options = ExtractMethodPipeline.asConstructor(analyzer, options) ?: throw ExtractException("Fail", elements.first())
        }
        if (! options.isStatic && isStatic == true) {
            options = ExtractMethodPipeline.withForcedStatic(analyzer, options) ?: throw ExtractException("Fail", elements.first())
        }
        if (newNameOfFirstParam != null) {
            options = options.copy(
                inputParameters = listOf(options.inputParameters.first().copy(name = newNameOfFirstParam)) + options.inputParameters.drop(1)
            )
        }
        if (returnType != null) {
            options = options.copy(dataOutput = options.dataOutput.withType(returnType))
        }
        if (disabledParameters.isNotEmpty()) {
            options = options.copy(
                disabledParameters = options.inputParameters.filterIndexed { index, _ -> index in disabledParameters },
                inputParameters = options.inputParameters.filterIndexed { index, _ -> index !in disabledParameters }
            )
        }
        if (visibility != null) {
            options = options.copy(visibility = visibility)
        }
        if (options.anchor.containingClass?.isInterface == true) {
            options = ExtractMethodPipeline.adjustModifiersForInterface(options.copy(visibility = PsiModifier.PRIVATE))
        }
        if (doRefactor) {
            extractMethod(options)
        }
        return true
    }

    fun showError(editor: Editor, ranges: List<TextRange>) {
        val project = editor.project ?: return
        if (ranges.isEmpty()) return
        val highlightManager = HighlightManager.getInstance(project)
        ranges.forEach { textRange ->
            highlightManager.addRangeHighlight(editor, textRange.startOffset, textRange.endOffset,
                EditorColors.SEARCH_RESULT_ATTRIBUTES, true, null)
        }
        WindowManager.getInstance().getStatusBar(project).info = RefactoringBundle.message("press.escape.to.remove.the.highlighting")
    }

    fun prepareRefactoringElements(extractOptions: ExtractOptions): ExtractedElements {
        val dependencies = withFilteredAnnotations(extractOptions)
        val factory = PsiElementFactory.getInstance(dependencies.project)
        val codeBlock = BodyBuilder(factory)
            .build(
                dependencies.elements,
                dependencies.flowOutput,
                dependencies.dataOutput,
                dependencies.inputParameters,
                dependencies.disabledParameters,
                dependencies.requiredVariablesInside
            )
        val method = SignatureBuilder(dependencies.project)
            .build(
                dependencies.anchor.context,
                dependencies.elements,
                dependencies.isStatic,
                dependencies.visibility,
                dependencies.typeParameters,
                dependencies.dataOutput.type.takeIf { !dependencies.isConstructor },
                dependencies.methodName,
                dependencies.inputParameters,
                dependencies.dataOutput.annotations,
                dependencies.thrownExceptions,
                dependencies.anchor
            )
        method.body?.replace(codeBlock)

        if (needsNullabilityAnnotations(dependencies.project) && ExtractMethodHelper.isNullabilityAvailable(dependencies)) {
//            updateMethodAnnotations(method, dependencies.inputParameters)
        }

        val callElements = CallBuilder(dependencies.elements.first()).createCall(method, dependencies)
        return ExtractedElements(callElements, method)
    }

    private fun needsNullabilityAnnotations(project: Project): Boolean {
        return PropertiesComponent.getInstance(project).getBoolean(ExtractMethodDialog.EXTRACT_METHOD_GENERATE_ANNOTATIONS, true)
    }

    companion object {
        private val LOG = Logger.getInstance(MethodExtractor::class.java)

        @NonNls const val refactoringId: String = "refactoring.extract.method"

        internal fun sendRefactoringDoneEvent(extractedMethod: PsiMethod) {
            val data = RefactoringEventData()
            data.addElement(extractedMethod)
            extractedMethod.project.messageBus.syncPublisher(RefactoringEventListener.REFACTORING_EVENT_TOPIC)
                .refactoringDone(refactoringId, data)
        }

        internal fun sendRefactoringStartedEvent(elements: Array<PsiElement>) {
            val project = elements.firstOrNull()?.project ?: return
            val data = RefactoringEventData()
            data.addElements(elements)
            val publisher = project.messageBus.syncPublisher(RefactoringEventListener.REFACTORING_EVENT_TOPIC)
            publisher.refactoringStarted(refactoringId, data)
        }

        fun invokeOnElements(project: Project, editor: Editor?, file: PsiFile?, elements: Array<PsiElement>, functionNameProvider: FunctionNameProvider?) {
            var selection = findEditorSelection(editor!!)
            if (selection == null && elements.size == 1) selection = elements[0].textRange
            if (selection != null) MyMethodExtractor(functionNameProvider).doExtract(file!!, selection)
        }
    }
}