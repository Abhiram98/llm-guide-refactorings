package com.intellij.ml.llm.template.utils

import com.google.gson.Gson
import com.intellij.lang.java.JavaLanguage
import com.intellij.ml.llm.template.LLMBundle
import com.intellij.ml.llm.template.refactoringobjects.extractfunction.EFCandidate
import com.intellij.ml.llm.template.refactoringobjects.extractfunction.EFSuggestion
import com.intellij.ml.llm.template.refactoringobjects.extractfunction.EFSuggestionList
import com.intellij.ml.llm.template.suggestrefactoring.RefactoringSuggestion
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.*
import com.intellij.refactoring.extractMethod.newImpl.ExtractMethodPipeline.findAllOptionsToExtract
import com.intellij.refactoring.extractMethod.newImpl.ExtractSelector
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.base.psi.unifier.toRange
import org.jetbrains.kotlin.idea.refactoring.introduce.extractFunction.ExtractKotlinFunctionHandler
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.elementsInRange
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


fun addLineNumbersToCodeSnippet(codeSnippet: String, startIndex: Int): String {
    val lines = codeSnippet.lines()
    val numberedLines = lines.mapIndexed { index, line -> "${startIndex + index}. $line" }
    return numberedLines.joinToString("\n")
}


fun replaceGithubUrlLineRange(githubUrl: String, lineStart: Int, lineEnd: Int): String {
    val newLineRange = String.format("L$lineStart-L$lineEnd")
    return String.format("${githubUrl.substringBefore("#")}#$newLineRange")
}


/**
 * This function will extract the suggestions from a ChatGPT output.
 * ChatGPT output may contain a mix of text (i.e. sentences) and code and Json.
 * The output is not always clean and this function is meant to identify the extract function suggestions
 *
 * Example of ChatGPT reply can be:
 *            I would suggest the following extract method refactorings:
 *
 *             1. Extract method for creating `GroupRebalanceConfig` object from lines 2650-2656.
 *             2. Extract method for creating `ConsumerCoordinator` object from lines 2649-2670.
 *             3. Extract method for creating `FetchConfig` object from lines 2674-2684.
 *             4. Extract method for creating `Fetcher` object from lines 2685-2692.
 *             5. Extract method for creating `OffsetFetcher` object from lines 2693-2701.
 *             6. Extract method for creating `TopicMetadataFetcher` object from lines 2702-2703.
 *
 *             The JSON object for these extract method refactorings would be:
 *
 *             ```
 *             [
 *                 {"function_name": "createRebalanceConfig", "line_start": 2650, "line_end": 2656},
 *                 {"function_name": "createConsumerCoordinator", "line_start": 2649, "line_end": 2670},
 *                 {"function_name": "createFetchConfig", "line_start": 2674, "line_end": 2684},
 *                 {"function_name": "createFetcher", "line_start": 2685, "line_end": 2692},
 *                 {"function_name": "createOffsetFetcher", "line_start": 2693, "line_end": 2701},
 *                 {"function_name": "createTopicMetadataFetcher", "line_start": 2702, "line_end": 2703}
 *             ]
 *             ```
 *
 * Another example may be:
 *             "The above function does not have any extract method opportunities."
 *
 */
fun identifyExtractFunctionSuggestions(input: String): EFSuggestionList {
    var refactoringSuggestion: RefactoringSuggestion;
    try {
        refactoringSuggestion= Gson().fromJson(input, RefactoringSuggestion::class.java)
    } catch (e: Exception){
        refactoringSuggestion = Gson().fromJson("{$input}", RefactoringSuggestion::class.java)
    }

    val efSuggestions = mutableListOf<EFSuggestion>()
    for (suggestion in refactoringSuggestion.improvements) {
        efSuggestions.add(
            EFSuggestion(
                functionName = "newMethod",
                lineStart = suggestion.start,
                lineEnd = suggestion.end,
            )
        )
    }

    return EFSuggestionList(efSuggestions)
}


fun isCandidateExtractable(
    efCandidate: EFCandidate,
    editor: Editor,
    file: PsiFile,
    observerList: List<Observer> = emptyList(),
    allowWholeBody: Boolean=false
): Boolean {
    when (file.language) {
        JavaLanguage.INSTANCE -> return isFunctionExtractableJava(efCandidate, editor, file, observerList, allowWholeBody)
        KotlinLanguage.INSTANCE -> return isSelectionExtractableKotlin(efCandidate, editor, file, observerList)
    }
    return false
}

private fun isFunctionExtractableJava(
    efCandidate: EFCandidate,
    editor: Editor,
    file: PsiFile,
    observerList: List<Observer>,
    allowWholeBody: Boolean
): Boolean {
    var result = true
    var reason = ""
    var applicationResult = EFApplicationResult.OK

    if (!efCandidate.isValid()) {
        result = false
        reason = LLMBundle.message("extract.function.invalid.candidate")
        applicationResult = EFApplicationResult.FAIL
    } else if (!allowWholeBody && selectionIsEntireBodyFunctionJava(efCandidate, file)) {
        result = false
        reason = LLMBundle.message("extract.function.entire.function.selection.message")
        applicationResult = EFApplicationResult.FAIL
    } else {
//        editor.selectionModel.setSelection(efCandidate.offsetStart, efCandidate.offsetEnd)
        val range = TextRange(efCandidate.offsetStart, efCandidate.offsetEnd)
//        val range = ExtractMethodHelper.findEditorSelection(editor)
        val elements = ExtractSelector().suggestElementsToExtract(file, range!!)
        if (elements.isEmpty()) {
            result = false
            applicationResult = EFApplicationResult.FAIL
            reason = LLMBundle.message("extract.function.code.not.extractable.message")
        } else {
            try {
                val allOptionsToExtract = findAllOptionsToExtract(elements)
                if (allOptionsToExtract.isEmpty()) {
                    result = false
                    reason = LLMBundle.message("extract.function.code.not.extractable.message")
                    applicationResult = EFApplicationResult.FAIL
                }
            } catch (e: Exception) {
//                logException(e)
                result = false
                reason = e.message ?: reason
                applicationResult = EFApplicationResult.FAIL
            }
        }
    }
//    editor.selectionModel.removeSelection()
    buildEFNotificationAndNotifyObservers(efCandidate, applicationResult, reason, observerList)
    return result
}

private fun isSelectionExtractableKotlin(
    efCandidate: EFCandidate,
    editor: Editor,
    file: PsiFile,
    observerList: List<Observer>
): Boolean {
    var result = true
    var reason = ""
    var applicationResult = EFApplicationResult.OK

    if (file !is KtFile) return false
    if (!efCandidate.isValid()) {
        result = false
        reason = LLMBundle.message("extract.function.invalid.candidate")
        applicationResult = EFApplicationResult.FAIL
    } else if (selectionIsEntireBodyFunctionKotlin(efCandidate, file)) {
        result = false
        reason = LLMBundle.message("extract.function.entire.function.selection.message")
        applicationResult = EFApplicationResult.FAIL
    } else if (!canSelectElementsForExtractionKotlin(efCandidate, editor, file)) {
        result = false
        reason = LLMBundle.message("extract.function.code.not.extractable.message")
        applicationResult = EFApplicationResult.FAIL
    } else {
        try {
            val elements = file.elementsInRange(TextRange(efCandidate.offsetStart, efCandidate.offsetEnd))
            val targetSibling = PsiUtils.getParentFunctionOrNull(elements[0], file.language)
            val extractionData = ExtractionData(file, elements.toRange(false), targetSibling!!)
            val analysisResult = extractionData.performAnalysis()
            if (analysisResult.status != AnalysisResult.Status.SUCCESS) {
                result = false
                reason = LLMBundle.message("extract.function.code.not.extractable.message")
                applicationResult = EFApplicationResult.FAIL
            } else {
                throw NotImplementedError("Not yet implemented")
                var iDescriptor = analysisResult.descriptor!!
//                ExtractionGeneratorConfiguration(
//                    ExtractableCodeDescriptor(
//                        extractionData = iDescriptor.extractionData,
//                        suggestedNames = iDescriptor.suggestedNames,
//                        visibility = iDescriptor.visibility,
//                        parameters = iDescriptor.parameters,
//                        receiverParameter = iDescriptor.receiverParameter,
//                        typeParameters = iDescriptor.typeParameters,
//                        replacementMap = iDescriptor.replacementMap,
//                        controlFlow = iDescriptor.controlFlow,
//                        returnType = iDescriptor.returnType,
//                        originalContext = null
//                        ),
//                    ExtractionGeneratorOptions(
//                        inTempFile = true,
//                        target = ExtractionTarget.FUNCTION,
//                    )
//                ).generateDeclaration()
            }

        } catch (t: Throwable) {
            result = false
            reason = LLMBundle.message("extract.function.code.not.extractable.message")
            applicationResult = EFApplicationResult.FAIL
        }
    }

    buildEFNotificationAndNotifyObservers(efCandidate, applicationResult, reason, observerList)
    editor.selectionModel.removeSelection()
    return result
}

fun canSelectElementsForExtractionKotlin(efCandidate: EFCandidate, editor: Editor, file: KtFile): Boolean {
    var result = false
    editor.selectionModel.setSelection(efCandidate.offsetStart, efCandidate.offsetEnd)
    try {
        ExtractKotlinFunctionHandler().selectElements(editor, file) { elements, _ ->
            result = elements.isNotEmpty()
        }
    }
    catch (t: Throwable) {
        return result
    }
    editor.selectionModel.removeSelection()
    return result
}

private fun buildEFNotificationAndNotifyObservers(
    efCandidate: EFCandidate,
    result: EFApplicationResult,
    reason: String,
    observers: List<Observer>
) {
//    observers.forEach {
//        it.update(
//            EFNotification(
//                EFCandidateApplicationPayload(
//                    result = result,
//                    candidate = efCandidate,
//                    reason = reason
//                )
//            )
//        )
//    }
    // TODO: Add logic to update observers
}

private fun logException(e: Exception) {
    val logger = Logger.getInstance("#com.intellij.ml.llm")
    val lineNumber = e.stackTrace.firstOrNull()?.lineNumber
    logger.info("Utils.kt:$lineNumber", e)
}

private fun selectionIsEntireBodyFunctionKotlin(efCandidate: EFCandidate, file: PsiFile): Boolean {
    val start = file.findElementAt(efCandidate.offsetStart)
    val end = file.findElementAt(efCandidate.offsetEnd)

    val parentBlock =
        PsiUtils.getParentFunctionBlockOrNull(start, KotlinLanguage.INSTANCE) ?: PsiUtils.getParentFunctionBlockOrNull(
            end,
            KotlinLanguage.INSTANCE
        )
    val statements = (parentBlock as KtBlockExpression).statements
    if (statements.isEmpty()) return false
    val statementsOffsetRange = statements.first().startOffset to statements.last().endOffset

    return efCandidate.offsetStart <= statementsOffsetRange.first && efCandidate.offsetEnd >= statementsOffsetRange.second
}

private fun selectionIsEntireBodyFunctionJava(efCandidate: EFCandidate, file: PsiFile): Boolean {
    val start = file.findElementAt(efCandidate.offsetStart)
    val end = file.findElementAt(efCandidate.offsetEnd)

    val parentBlock = PsiUtils.getParentFunctionBlockOrNull(start, JavaLanguage.INSTANCE)
        ?: PsiUtils.getParentFunctionBlockOrNull(end, JavaLanguage.INSTANCE)
    val statements = (parentBlock as PsiCodeBlock).statements

    if (statements.isEmpty()) {
        return false
    }

    val statementsOffsetRange = statements.first().startOffset to statements.last().endOffset
    return efCandidate.offsetStart <= statementsOffsetRange.first && efCandidate.offsetEnd >= statementsOffsetRange.second
}

fun openFile(filePath: String, project: Project): Pair<Editor, PsiFile> {
    var ret : Pair<Editor, PsiFile>? = null
    val vfile = LocalFileSystem.getInstance().refreshAndFindFileByPath(project.basePath + "/" + filePath)
        ?: throw Exception("file not found - ${project.basePath+"/"+filePath}")
    val newEditor = FileEditorManager.getInstance(project).openTextEditor(
        OpenFileDescriptor(
            project,
            vfile
        ),
        false // request focus to editor
    )!!
    val psiFile = PsiManager.getInstance(project).findFile(vfile)!!

    ret = Pair(newEditor, psiFile)

    return ret!!
}

fun openFileFromQualifiedName(qualifiedName: String, project: Project, focus: Boolean): Pair<Editor, PsiFile> {
    var ret : Pair<Editor, PsiFile>? = null
    val clazz = PsiUtils.findClassFromQualifier(qualifiedName, project)

    if (clazz==null){
        throw Exception("no such class $qualifiedName")
    }
    val vfile = clazz.containingFile.originalFile.virtualFile
    val newEditor = FileEditorManager.getInstance(project).openTextEditor(
        OpenFileDescriptor(
            project,
            vfile
        ),
        focus // request focus to editor
    )!!
    val psiFile = PsiManager.getInstance(project).findFile(vfile)!!

    ret = Pair(newEditor, psiFile)

    return ret!!
}
fun findAllFilesWithExtension(basePath: String, extension: String): List<Path>{
    val projectDirAbsolutePath = Paths.get(basePath).toAbsolutePath()
    return Files.walk(projectDirAbsolutePath)
        .filter { item -> Files.isRegularFile(item) }
        .filter { item -> item.toString().endsWith(extension) }
        .toList()
}


