package com.intellij.ml.llm.template.intentions

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.ml.llm.template.LLMBundle
import com.intellij.ml.llm.template.benchmark.CreateBenchmarkForFile
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class CreateBenchmarkIntention : IntentionAction {
    val newCommitMap: MutableMap<String, Pair<String, String>> = mutableMapOf()
    override fun startInWriteAction(): Boolean {
        return false
    }

    override fun getText() = LLMBundle.message("intentions.create.benchmark.family.name")

    override fun getFamilyName() = LLMBundle.message("intentions.create.benchmark.family.name")

    override fun isAvailable(p0: Project, p1: Editor?, p2: PsiFile?): Boolean {
        return p0.isInitialized
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        // read RMiner output.
        // For each "interesting file", that is: >=5 refactorings, in a single commit
        // Go to commit
        // Create inverse refactoring objects
        // Execute them. Remember which ones were executed successfully.
        // create branch, create commit and save this information somewhere.
        val projectDir = "/Users/abhiram/Documents/TBE/evaluation_projects/cassandra"
        val refminerOut = "/Users/abhiram/Documents/TBE/evaluation_projects/cassandra-interesting-two-files.json"
        val jsonContent = Files.readString(Path.of(refminerOut))
        val json = JsonParser.parseString(jsonContent)

        val repo = FileRepositoryBuilder()
            .setGitDir(File("${project.basePath}/.git"))
            .build()
        val gitRepo = Git(repo)

        val task = object : Task.Backgroundable(
            project, LLMBundle.message("intentions.create.benchmark.progress.title")
        ) {
            override fun run(indicator: ProgressIndicator) {
                    for (filename in json.asJsonObject.keySet()) {
                        createBenchmark(json, filename, gitRepo, project)
                    }
                    DumbService.getInstance(project).smartInvokeLater {
                        Files.write(
                            Path.of("/Users/abhiram/Documents/TBE/evaluation_projects/cassandra-interesting-two-files-new-commits.json"),
                            Gson().toJson(newCommitMap).toString().toByteArray()
                        )
                    }

            }
        }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
            // Make commit and checkout branch/tag

//        val task = object : Task.Backgroundable(
//            project, LLMBundle.message("intentions.create.benchmark.progress.title")
//        ) {
//            override fun run(indicator: ProgressIndicator) {
//
//            }
//        }
//        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    private fun createBenchmark(
        json: JsonElement,
        filename: String,
        gitRepo: Git,
        project: Project
    ) {
        val commitInfo = json.asJsonObject[filename].asJsonArray[0]
        val commitHash = commitInfo.asJsonObject["sha1"].asString
        DumbService.getInstance(project).smartInvokeLater {

            // Checkout commit
            gitRepo.checkout().setName(commitHash).setForced(true).call()
            project.getBaseDir().refresh(false, true);
        }
        // allow re-index after updating git repo head.
        DumbService.getInstance(project).smartInvokeLater {

            // Open file
            val editorFilePair = try {
                openFile(filename, project)
            } catch (e: Exception) {
                print("file not found")
                return@smartInvokeLater
            }
            val newEditor = editorFilePair.first
            val newFile = editorFilePair.second

            val refactorings = commitInfo.asJsonObject["refactorings"].asJsonArray
            val fileBenchmark =
                CreateBenchmarkForFile(filename, project, newEditor, newFile, refactorings)
            fileBenchmark.create()

            FileDocumentManager.getInstance().saveAllDocuments(); // save changes to local filesystem
            project.baseDir.refresh(false, true);
            gitRepo.add().addFilepattern(".").call()
            val newCommitHash = gitRepo.commit().setMessage("undo refactorings in $commitHash").call()

            newCommitMap.put(filename, Pair(commitHash, newCommitHash.name))
        }


    }

    private fun openFile(filePath: String, project: Project): Pair<Editor, PsiFile> {
//        runWriteAction {  }
        var ret : Pair<Editor, PsiFile>? = null
        val vfile = LocalFileSystem.getInstance().refreshAndFindFileByPath(project.basePath + "/" + filePath)
            ?: throw Exception("file not found")
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
}