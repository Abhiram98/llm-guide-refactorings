package org.boulderse.ijserver.benchmark

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.boulderse.ijserver.refactoringobjects.extractfunction.EFCandidate
import org.boulderse.ijserver.refactoringobjects.extractfunction.ExtractMethodFactory
import org.boulderse.ijserver.utils.MethodSignature
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit

data class ExtractionRange(
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int,
    val endColumn: Int,
)

class EmmBenchmark(
    val prevCommit: String,
    val refId: Int,
    val filePath: String,
    val methodExtractedFrom: MethodSignature,
    val extractionRange: ExtractionRange,
    val newEditor: Editor,
    val newFile: PsiFile,
    val newMethodName: String,
) {
    var completed: Boolean = false
    var newCommitHash: String? = null
    var newBranchName: String? = null

    fun extractMethod(
        project: Project,
        editor: Editor,
        file: PsiFile,
    ) {
        val startLineOffset = editor.document.getLineStartOffset(extractionRange.startLine - 1)
        val startOffset = startLineOffset + extractionRange.startColumn - 1
        val endLineOffset = editor.document.getLineStartOffset(extractionRange.endLine - 1)
        val endOffset = endLineOffset + extractionRange.endColumn - 1

        val startElement = file.findElementAt(startOffset)
        val endElement = file.findElementAt(endOffset - 1)
        val ref =
            ExtractMethodFactory.fromEFCandidate(
                EFCandidate.fromExtractionRange(newMethodName, extractionRange.startLine, extractionRange.endLine, startOffset, endOffset),
                startElement,
                endElement,
            )
        invokeLater {
            ref.performRefactoring(project, editor, file)
            completed = true
        }
    }

    fun createNewCommit(gitRepo: Git): RevCommit? {
        gitRepo.add().addFilepattern(".").call()
        gitRepo.checkout().setName(prevCommit).call() // this was resolving an issue with being unable to checkout the right branch.
        val newCommitHash1 =
            gitRepo
                .commit()
                .setMessage(
                    "extract method $newMethodName from ${filePath.split('/').last()} in $prevCommit",
                ).call()
        val branchName = "extract-$newMethodName-${methodExtractedFrom.methodName}-${prevCommit.substring(0, 7)}"

        try {
            gitRepo
                .branchDelete()
                .setForce(true)
                .setBranchNames(branchName)
                .call()
        } catch (e: Exception) {
            print("failed to delete. must not exist")
            e.printStackTrace()
        }
        gitRepo
            .checkout()
            .setCreateBranch(true)
            .setForced(true)
            .setName(branchName)
            .call()
        newCommitHash = newCommitHash1.name
        newBranchName = branchName
        return newCommitHash1
    }
}
