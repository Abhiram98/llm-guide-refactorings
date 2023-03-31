package com.intellij.ml.llm.template.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.lang.LanguageRefactoringSupport
import com.intellij.lang.java.JavaLanguage
import com.intellij.ml.llm.template.LLMBundle
import com.intellij.ml.llm.template.models.*
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase
import com.intellij.refactoring.extractMethod.ExtractMethodHandler
import com.intellij.refactoring.extractMethod.ExtractMethodProcessor
import com.intellij.refactoring.extractMethod.PrepareFailedException
import kotlinx.serialization.json.Json


@Suppress("UnstableApiUsage")
abstract class ApplyExtractFunctionTransformationIntention(
    private val llmRequestProvider: LLMRequestProvider = CodexRequestProvider
) : IntentionAction {
    private val logger = Logger.getInstance("#com.intellij.ml.llm")

    override fun getFamilyName(): String = LLMBundle.message("intentions.apply.transformation.family.name")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return editor != null && file != null
    }

    private fun findSelectedPsiElements(editor: Editor, file: PsiFile?): Array<PsiElement> {
        val selectionModel = editor.selectionModel
        val startOffset = selectionModel.selectionStart
        val endOffset = selectionModel.selectionEnd

        val startElement = file?.findElementAt(startOffset)
        val endElement = file?.findElementAt(if (endOffset > 0) endOffset - 1 else endOffset)

        if (startElement == null || endElement == null) {
            return emptyArray()
        }

        val commonParent = PsiTreeUtil.findCommonParent(startElement, endElement)
        if (commonParent == null) {
            return emptyArray()
        }

        val selectedElements = PsiTreeUtil.findChildrenOfType(commonParent, PsiElement::class.java)
        return selectedElements.filter { it.textRange.startOffset >= startOffset && it.textRange.endOffset <= endOffset }.toTypedArray()
    }

    private fun invokeEf1(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return
        val document = editor.document
        val selectionModel = editor.selectionModel
        val selectedText = selectionModel.selectedText
        if (selectedText != null) {
            /* invoke ExtractMethodHandler */
            val selectedElements = findSelectedPsiElements(editor, file)
            val extractMethodProcessor = ExtractMethodProcessor(
                project,
                editor,
                selectedElements,
                null,
                "Extract Method",
                "newMethod",
                null
            )
            extractMethodProcessor.methodName = "anotherMethodName"

            try {
//                 Perform the extraction
                if (extractMethodProcessor.prepare()) {
//                    ExtractMethodHandler.extractMethod(project, extractMethodProcessor)
                    extractMethodProcessor.doRefactoring()
                    val x = 0
//                    CommandProcessor.getInstance().executeCommand(project, {
//                        ApplicationManager.getApplication().runWriteAction {
//                            extractMethodProcessor.doRefactoring()
//                        }
//                    }, "Extract Method", null)
                }
            } catch (e: PrepareFailedException) {
                // Handle the error if the extraction is not possible
                e.message?.let { showErrorHint(project, editor, it) }
            }
            catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }

    private fun performExtractFunction(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return
        val document = editor.document
        val selectionModel = editor.selectionModel
        val selectedText = selectionModel.selectedText
        if (selectedText != null) {
            /* invoke ExtractMethodHandler */
            val selectedElements = findSelectedPsiElements(editor, file)

            val provider = LanguageRefactoringSupport.INSTANCE.forLanguage(JavaLanguage.INSTANCE)
            val refactoringActionHandler = provider.getExtractMethodHandler()
            val dataContext = (editor as EditorEx).dataContext
            refactoringActionHandler?.invoke(project, editor, file, dataContext)
        }
    }

    private fun selectLines(startLine: Int, endLine: Int, editor: Editor?) {
        if (editor == null || editor.document == null) return
        val selectionModel = editor.selectionModel
        selectionModel.setSelection(
            editor.document.getLineStartOffset(startLine),
            editor.document.getLineEndOffset(endLine)
        )
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return

        val document = editor.document
        val selectionModel = editor.selectionModel
        val selectedText = selectionModel.selectedText
        if (selectedText != null) {
            /*
             * The selected text should be the whole function. We have to follow the following steps:
             * 1. Take the selected text, and send it to ChatGPT using a well formatted prompt
             * 2. Process the reply
             *      - how to handle multiple suggestions? Take the largest?
             *      - options should be filtered based on some criteria such as how actionable they are,
             *        how many lines of code do they cover, etc
             * 3. Present suggestions to developer and let the developer choose
             * 4. Based on the developer's choice, automatic Extract Function should be performed
             */
            invokeLlm(selectedText, project, editor, file, selectionModel)
        } else {
            val namedElement = getParentNamedElement(editor)
            if (namedElement != null) {
                val queryText = namedElement.text
                val textRange = namedElement.textRange
                selectionModel.setSelection(textRange.startOffset, textRange.endOffset)
                invokeLlm(queryText, project, editor, file, selectionModel)
            } else {
                selectionModel.selectLineAtCaret()
                val textRange = getLineTextRange(document, editor)
//                transform(project, document.getText(textRange), editor, textRange)
            }
        }
    }

    private fun invokeLlm(text: String, project: Project, editor: Editor, file: PsiFile, selectionModel: SelectionModel) {
        logger.info("Invoking LLM with text: $text")
        val instruction = "Instruction for LLM goes here"
        val task = object : Task.Backgroundable(project, LLMBundle.message("intentions.request.extract.function.background.process.title")) {
                override fun run(indicator: ProgressIndicator) {
                    val response = sendEditRequest(
                        project,
                        text,
                        instruction,
                        llmRequestProvider = llmRequestProvider,
                    )
                    if (response != null) {
                        response.getSuggestions().firstOrNull()?.let {
                            invokeLater {
                                val efs: EFSuggestion = Json.decodeFromString(EFSuggestionSerializer, it.text)
                                applySuggestion(efs, project, editor, file )
                            }
                        }
                    }
                }
            }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    private fun applySuggestion(efSuggestion: EFSuggestion, project: Project, editor: Editor, file: PsiFile) {
        selectLines(efSuggestion.lineStart, efSuggestion.lineEnd, editor)
        performExtractFunction(project, editor, file)
    }

    private fun showErrorHint(project: Project, editor: Editor, message: String) {
        com.intellij.openapi.ui.Messages.showErrorDialog(project, message, "Extract Method Error")
    }

    private fun getLineTextRange(document: Document, editor: Editor): TextRange {
        val lineNumber = document.getLineNumber(editor.caretModel.offset)
        val startOffset = document.getLineStartOffset(lineNumber)
        val endOffset = document.getLineEndOffset(lineNumber)
        return TextRange.create(startOffset, endOffset)
    }

    private fun getParentNamedElement(editor: Editor): PsiNameIdentifierOwner? {
        val element = PsiUtilBase.getElementAtCaret(editor)
        return PsiTreeUtil.getParentOfType(element, PsiNameIdentifierOwner::class.java)
    }

    private fun transform(project: Project, text: String, editor: Editor, textRange: TextRange) {
        val instruction = getInstruction(project, editor) ?: return

        logger.info("Invoke transformation action with '$instruction' instruction for '$text'")
        val task =
            object : Task.Backgroundable(project, LLMBundle.message("intentions.request.extract.function.background.process.title")) {
                override fun run(indicator: ProgressIndicator) {
                    val response = sendEditRequest(
                        project,
                        text,
                        instruction,
                        llmRequestProvider = llmRequestProvider,
                    )
                    if (response != null) {
                        response.getSuggestions().firstOrNull()?.let {
                            logger.info("Suggested change: $it")
                            invokeLater {
                                WriteCommandAction.runWriteCommandAction(project) {
                                    updateDocument(project, it.text, editor.document, textRange)
                                }
                            }
                        }
                    }
                }
            }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    abstract fun getInstruction(project: Project, editor: Editor): String?

    private fun updateDocument(project: Project, suggestion: String, document: Document, textRange: TextRange) {
        document.replaceString(textRange.startOffset, textRange.endOffset, suggestion)
        PsiDocumentManager.getInstance(project).commitDocument(document)
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
        psiFile?.let {
            val reformatRange = TextRange(textRange.startOffset, textRange.startOffset + suggestion.length)
            CodeStyleManager.getInstance(project).reformatText(it, listOf(reformatRange))
        }
    }

    override fun startInWriteAction(): Boolean = false
}