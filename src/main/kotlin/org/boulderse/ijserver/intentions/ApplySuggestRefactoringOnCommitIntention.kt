package org.boulderse.ijserver.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.PsiJavaFileImpl
import com.intellij.psi.search.FilenameIndex
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.model.chat.ChatLanguageModel
import org.boulderse.ijserver.LLMBundle
import org.boulderse.ijserver.settings.RefAgentSettingsManager
import org.boulderse.ijserver.telemetry.EFTelemetryDataUtils
import org.boulderse.ijserver.utils.GitUtils
import org.boulderse.ijserver.utils.PsiUtils
import org.boulderse.ijserver.utils.addLineNumbersToCodeSnippet
import org.eclipse.jgit.diff.DiffEntry
import org.jetbrains.kotlin.idea.base.util.projectScope
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

class ApplySuggestRefactoringOnCommitIntention(
    private var efLLMRequestProvider: ChatLanguageModel = RefAgentSettingsManager.getInstance().createAndGetAiModel()!!,
) : ApplySuggestRefactoringAgentIntention(efLLMRequestProvider) {
    init {
//        prompter = something
    }

//    override fun processLLMResponse(response: LLMBaseResponse, project: Project, editor: Editor, file: PsiFile) {
//        TODO("Not yet implemented")
//    }

    override fun getText(): String = LLMBundle.message("intentions.apply.suggest.refactoring.commit.agent.family.name")

    override fun getFamilyName(): String = LLMBundle.message("intentions.apply.suggest.refactoring.commit.agent.family.name")

    override fun isAvailable(
        project: Project,
        editor: Editor?,
        file: PsiFile?,
    ): Boolean = editor != null && file != null

    override fun invokeLLM(
        project: Project,
        promptIterator: Iterator<MutableList<ChatMessage>>,
        editor: Editor,
        file: PsiFile,
    ) {
//        super.invokeLLM(project, messageList, editor, file)
    }

    override fun invoke(
        project: Project,
        editor: Editor?,
        file: PsiFile?,
    ) {
        efLLMRequestProvider = RefAgentSettingsManager.getInstance().createAndGetAiModel()!!

        if (editor == null || file == null) return

        // TODO: get latest commit.
        // TODO: get all files in commit
        val editedJavaFiles =
            GitUtils
                .getDiffsInLatestCommit(project.basePath!!)
                .filter { it.changeType != DiffEntry.ChangeType.DELETE }
                .filter { it.newPath.endsWith(".java") }
//                .sortedBy { it.newPath }
                .map { it.newPath }

        // TODO: filter diffs to find the most interesting changes.
        //  get the top 1 (or top 3) changed files. Files added for examples.
        //  Or functions which have more than 5 lines (or 20%) of their code modified.

        val firstEditedJavaFile = editedJavaFiles.first()

        // TODO: open _any_ file and run agent on entire file.
        val files =
            FilenameIndex.getVirtualFilesByName(firstEditedJavaFile.split("/").last(), project.projectScope())
        if (files.size > 0) {
            val fileEdited = files.elementAt(0)

            val newEditor =
                FileEditorManager.getInstance(project).openTextEditor(
                    OpenFileDescriptor(
                        project,
                        fileEdited,
                    ),
                    true, // request focus to editor
                )!!
            val psiFile = PsiManager.getInstance(project).findFile(fileEdited)!!
            newEditor.selectionModel.setSelection(0, 0)
            val selectionModel = newEditor.selectionModel
            val namedElement =
                PsiUtils.getParentFunctionOrNull(newEditor, psiFile.language)
                    ?: PsiUtils.getParentClassOrNull(newEditor, psiFile.language)
                    ?: (psiFile as PsiJavaFileImpl).classes[0]
            if (namedElement != null) {
                telemetryDataManager.newSession()
                val codeSnippet = namedElement.text

                val textRange = namedElement.textRange
                selectionModel.setSelection(textRange.startOffset, textRange.endOffset)
                val startLineNumber = newEditor.document.getLineNumber(selectionModel.selectionStart) + 1
                val withLineNumbers = addLineNumbersToCodeSnippet(codeSnippet, startLineNumber)
                functionSrc = withLineNumbers
                functionPsiElement = namedElement

                val bodyLineStart =
                    when (namedElement) {
                        is PsiClass -> PsiUtils.getClassBodyStartLine(namedElement)
                        else -> PsiUtils.getFunctionBodyStartLine(namedElement)
                    }
                telemetryDataManager.addHostFunctionTelemetryData(
                    EFTelemetryDataUtils.buildHostFunctionTelemetryData(
                        codeSnippet = codeSnippet,
                        lineStart = startLineNumber,
                        bodyLineStart = bodyLineStart,
                        language = psiFile.language.id.toLowerCaseAsciiOnly(),
                        filePath = psiFile.virtualFile.path,
                        hostClassPsi = functionPsiElement as? PsiClass,
                    ),
                )

                // TODO: modify prompt to say that you can only make changes on hunk.
                //  This is a research problem. Should we modify only the hunk? Or a little bit extra here and there?

                getPromptAndRunBackgroundable(withLineNumbers, project, newEditor, psiFile)
            }
        }
    }
}
