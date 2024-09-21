package com.intellij.ml.llm.template.intentions

import com.google.gson.JsonParser
import com.intellij.ml.llm.template.utils.openFile
import com.intellij.ml.llm.template.utils.openFileFromQualifiedName
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiFile
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.withLock
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File

class ApplyMMOnProjectAndCommitIntention: ApplyMoveMethodOnProjectIntention() {
    override fun runPluginOnSpecificFiles(project: Project) {

        val fileText = ApplyMMOnProjectAndCommitIntention::class.java
            .getResource("/plugin_input_files/classes_and_commits.json")?.readText()?:return
        val fileAndCommits = JsonParser.parseString(fileText).asJsonArray
        val repo = FileRepositoryBuilder()
            .setGitDir(File("${project.basePath}/.git"))
            .build()
        val gitRepo = Git(repo)


        for (classAndCommit in fileAndCommits) {
            val filePath = classAndCommit.asJsonObject.get("file_path").asString
            val commitHash = classAndCommit.asJsonObject.get("commit_hash").asString
            runBlocking{
                mutex.withLock {
                    invokeLaterFinished = false
                    gitRepo.checkout().setName(commitHash).setForced(true).call()
                    project.getBaseDir().refresh(false, true)
                    VfsUtil.markDirtyAndRefresh(false, true, true, project.baseDir)
                    Thread.sleep(5000)
                    var newFile: PsiFile? = null
                    var newEditor: Editor? = null
                    DumbService.getInstance(project).smartInvokeLater {
                        val editorFilePair = openFile(filePath, project)
                        newEditor = editorFilePair.first
                        newFile = editorFilePair.second
                    }
                    Thread.sleep(5000)
                    DumbService.getInstance(project).smartInvokeLater {
                        super.invokePlugin(project, newEditor, newFile)
                        invokeLaterFinished = true
                    }
                    runBlocking{
                        waitForBackgroundFinish(30 * 60 * 1000, 1000)
                        invokeLaterFinished = false
                        invokeLater {
                            if (newFile != null)
                                FileEditorManager.getInstance(project).closeFile(newFile!!.virtualFile)
                            invokeLaterFinished = true
                        }
                        waitForBackgroundFinish(5 * 60 * 1000, 1000)
                    }
                }
            }
        }


    }
}