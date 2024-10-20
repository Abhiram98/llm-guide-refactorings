package com.intellij.ml.llm.template.intentions

import com.google.gson.JsonParser
import com.intellij.ml.llm.template.utils.openFile
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.PsiJavaFileImpl
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.withLock
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class ApplyMMOnProjectAndCommitIntention: ApplyMoveMethodOnProjectIntention() {
    override fun runPluginOnSpecificFiles(project: Project) {
        val data_dir = System.getenv("DATA_DIR")
        val fileText = Files.readString(Path.of(data_dir).resolve("plugin_input_files/classes_and_commits.json")) ?: return

        val fileAndCommits = JsonParser.parseString(fileText).asJsonArray
        val repo = FileRepositoryBuilder()
            .setGitDir(File("${project.basePath}/.git"))
            .build()
        val gitRepo = Git(repo)


        for (classAndCommit in fileAndCommits) {
            val filePath = classAndCommit.asJsonObject.get("file_path").asString
            val commitHash = classAndCommit.asJsonObject.get("commit_hash").asString
            val className = classAndCommit.asJsonObject.get("class_name").asString

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
                    val psiFile = newFile as? PsiJavaFileImpl
                    val targetClass = runReadAction { findTargetClass(psiFile, className) }
                    DumbService.getInstance(project).smartInvokeLater {
                        super.invokePlugin(project, newEditor, newFile, targetClass)
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
    private fun findTargetClass(psiFile: PsiJavaFileImpl?, className: String): PsiClass? {
        if (psiFile == null) return null
        return psiFile.classes.find { it.name == className } ?: psiFile.classes.flatMap { it.allInnerClasses.toList() }.find { it.name == className }
    }
}