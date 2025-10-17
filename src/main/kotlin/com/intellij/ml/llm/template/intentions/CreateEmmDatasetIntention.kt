package com.intellij.ml.llm.template.intentions

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.ml.llm.template.LLMBundle
import com.intellij.ml.llm.template.benchmark.CreateBenchmarkForFile
import com.intellij.ml.llm.template.benchmark.EmmBenchmark
import com.intellij.ml.llm.template.benchmark.ExtractionRange
import com.intellij.ml.llm.template.utils.MethodSignature
import com.intellij.ml.llm.template.utils.openFile
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File
import java.nio.file.Files
import java.nio.file.Path


class CreateEmmDatasetIntention() : IntentionAction {
    data class Status(val success: Boolean, val newCommitHash: String?, val newBranchName: String?)

    val newCommitMap: MutableMap<String, Pair<String, String>> = mutableMapOf()
    val statusMap = mutableMapOf<Int, Status>()
    lateinit var myProject: Project
    private var openFileCompleted = false

    private val mutex = Mutex()

    override fun startInWriteAction(): Boolean {
        return false
    }

    override fun getText() = LLMBundle.message("intentions.create.benchmark.family.name")

    override fun getFamilyName() = LLMBundle.message("intentions.create.benchmark.family.name")

    override fun isAvailable(p0: Project, p1: Editor?, p2: PsiFile?): Boolean {
        return p0.isInitialized
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        myProject = project
        val task = object : Task.Backgroundable(
            project, LLMBundle.message("intentions.create.benchmark.progress.title")
        ) {
            override fun run(indicator: ProgressIndicator) {
                createDataset()
            }
        }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    private fun createDataset() {
        val data_dir = System.getenv("DATA_DIR")
        val extractionFilePath = Path.of(data_dir).resolve("plugin_input_files/extraction_files_and_ranges.json")
        val fileText = Files.readString(extractionFilePath)?:return
        val json = JsonParser.parseString(fileText)
        val repo = FileRepositoryBuilder()
            .setGitDir(File("${myProject.basePath}/.git"))
            .build()
        val gitRepo = Git(repo)


        for (jsonElement in json.asJsonArray) {
            runBlocking{ processExtract(jsonElement, gitRepo) }
        }
        writeResults()

    }

    private fun writeResults() {
        print("results---")
        print(Gson().toJson(statusMap).toString())
        print("end of results---")

        val data_dir = System.getenv("DATA_DIR")
        val outfilePath = Path.of(data_dir).resolve("plugin_output/extraction_results.json")
        Files.write(outfilePath, Gson().toJson(statusMap).toString().toByteArray())
        println("Wrote to $outfilePath")
    }

    private suspend fun processExtract(jsonElement: JsonElement, gitRepo: Git) {
        mutex.withLock {
            val prevCommit = jsonElement.asJsonObject.get("prev_commit").asString
            val refId = jsonElement.asJsonObject.get("ref_id").asInt
            val filePath = jsonElement.asJsonObject.get("file_path").asString
            val newMethodName = jsonElement.asJsonObject.get("extracted_method_name").asString

            try {
                gitRepo.checkout().setName(prevCommit).setForced(true).call()
                reloadProjectFiles()
                Thread.sleep(5000) // sleep to allow refresh.
                var editorFilePair: Pair<Editor, PsiFile>? = null
                openFileCompleted = false
                DumbService.getInstance(myProject).smartInvokeLater {
                    editorFilePair = openFile(filePath, myProject)
                    openFileCompleted = true
                }
                waitForFileOpenFinish(10 * 60 * 1000, 1000)
                val newEditor = editorFilePair!!.first
                val newFile = editorFilePair!!.second

                val methodExtractedFrom = MethodSignature.getMethodSignatureParts(
                    jsonElement.asJsonObject.get("method_extracted_from").asString
                )
                val extractedStartLine = jsonElement.asJsonObject.get("extracted_start_line").asInt
                val extractedStartColumn = jsonElement.asJsonObject.get("extracted_start_column").asInt
                val extractedEndLine = jsonElement.asJsonObject.get("extracted_end_line").asInt
                val extractedEndColumn = jsonElement.asJsonObject.get("extracted_end_column").asInt
                val benchmark = EmmBenchmark(
                    prevCommit,
                    refId,
                    filePath,
                    methodExtractedFrom!!,
                    ExtractionRange(extractedStartLine, extractedStartColumn, extractedEndLine, extractedEndColumn),
                    newEditor,
                    newFile,
                    newMethodName
                )
                benchmark.extractMethod(myProject, newEditor, newFile)
                waitForBenchmarkFinish(30 * 60 * 1000, 1000, benchmark)
                openFileCompleted = false
                invokeLater{
                    FileDocumentManager.getInstance().saveDocumentAsIs(newEditor.document)
                    openFileCompleted = true
                }
                waitForFileOpenFinish(10 * 60 * 1000, 1000)
                myProject.save()
                Thread.sleep(1000)
                openFileCompleted = false
                invokeLater{
                    FileEditorManager.getInstance(myProject).closeFile(newFile.virtualFile)
                    openFileCompleted = true
                }
                waitForFileOpenFinish(10 * 60 * 1000, 1000)
                benchmark.createNewCommit(gitRepo)
                statusMap.put(refId, Status(true, benchmark.newCommitHash, benchmark.newBranchName))
            } catch (e: Exception) {
                e.printStackTrace()
                statusMap.put(refId, Status(false, null, null))
            }
        }
    }


    private fun reloadProjectFiles() {
        myProject.getBaseDir().refresh(false, true)
        VfsUtil.markDirtyAndRefresh(false, true, true, myProject.baseDir)
    }

    private fun createBenchmark(
        refJson: JsonElement,
        filename: String,
        gitRepo: Git,
        project: Project
    ) {
//        val commitInfo = refJson.asJsonObject[filename].asJsonArray[0]
        val commitHash = refJson.asJsonObject["v2_hash"].asString
        DumbService.getInstance(project).smartInvokeLater {
            Thread.sleep(500)
            // Checkout commit
            gitRepo.checkout().setName(commitHash).setForced(true).call()
            project.getBaseDir().refresh(false, true)
            VfsUtil.markDirtyAndRefresh(false, true, true, project.baseDir)
        }
        // allow re-index after updating git repo head.
        DumbService.getInstance(project).smartInvokeLater {
            Thread.sleep(500)
            // Open file
            val editorFilePair = try {
                openFile(filename, project)
            } catch (e: Exception) {
                print("file not found: $filename")
                newCommitMap.put(filename, Pair(commitHash, "file not found"))
                return@smartInvokeLater
            }
            val newEditor = editorFilePair.first
            val newFile = editorFilePair.second

            val refactorings = refJson.asJsonObject["refactorings"].asJsonArray
            val fileBenchmark =
                CreateBenchmarkForFile(filename, project, newEditor, newFile, refactorings)
            fileBenchmark.create()
//            statusMap.putAll(fileBenchmark.statusMap)
            val apiDir: VirtualFile = project.getBaseDir()
            VfsUtil.markDirtyAndRefresh(false, true, true, apiDir)
//            VirtualFileManager.getInstance().syncRefresh()
            FileDocumentManager.getInstance().saveAllDocuments() // save changes to local filesystem
            project.baseDir.refresh(false, true);
            Thread.sleep(1000)
            project.save()

            Thread.sleep(500)
            val newCommitHash = createNewCommit(gitRepo, commitHash, filename)

            newCommitMap.put(filename, Pair(commitHash, newCommitHash!!.name))
        }


    }

    private fun createNewCommit(
        gitRepo: Git,
        commitHash: String,
        filename: String
    ): RevCommit? {
        gitRepo.add().addFilepattern(".").call()
        gitRepo.checkout().setName(commitHash).call() // this was resolving an issue with being unable to checkout the right branch.
        val newCommitHash = gitRepo.commit().setMessage("undo refactorings in $commitHash").call()
        val baseClassName = filename.split("/").last().split(".java").first()
        val branchName = "undo-$baseClassName-${commitHash.substring(0, 7)}"

        try {
            gitRepo.branchDelete().setForce(true).setBranchNames(branchName).call()
        } catch (e: Exception) {
            print("failed to delete. must not exist")
            e.printStackTrace()
    //                return@smartInvokeLater
        }
        gitRepo.checkout()
            .setCreateBranch(true)
            .setForced(true)
            .setName(branchName).call();
        return newCommitHash
    }

    private tailrec suspend fun waitForBenchmarkFinish(maxDelay: Long, checkPeriod: Long, benchmark: EmmBenchmark) : Boolean{
        if(maxDelay < 0) return false
        if(benchmark.completed) return true
        delay(checkPeriod)
        return waitForBenchmarkFinish(maxDelay - checkPeriod, checkPeriod, benchmark)
    }

    private tailrec suspend fun waitForFileOpenFinish(maxDelay: Long, checkPeriod: Long) : Boolean{
        if(maxDelay < 0) return false
        if(openFileCompleted) return true
        delay(checkPeriod)
        return waitForFileOpenFinish(maxDelay - checkPeriod, checkPeriod)
    }

}