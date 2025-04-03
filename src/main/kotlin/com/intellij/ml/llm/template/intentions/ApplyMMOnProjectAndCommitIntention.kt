package com.intellij.ml.llm.template.intentions

import com.google.gson.JsonParser
import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.ProjectUtil
import com.intellij.ml.llm.template.utils.openFile
import com.intellij.openapi.application.invokeLater
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
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path

class ApplyMMOnProjectAndCommitIntention: ApplyMoveMethodOnProjectIntention() {
    override fun runPluginOnSpecificFiles(project: Project, classNamesFile: Path) {
//        val data_dir = System.getenv("DATA_DIR")
//        runOnCommits(project, Path.of(data_dir).resolve("plugin_input_files/classes_and_commits.json"))
        runRealWorldEvaluation()
    }

    private fun runRealWorldEvaluation(){
        val projectPaths = listOf(

            SyntheticParams("", "dbeaver", Path("/Users/abhiram/Documents/TBE/evaluation_projects/dbeaver"), Path("/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/plugin_input_files/classes_and_commits-dbeaver.json")),

            SyntheticParams("", "elasticsearch", Path("/Users/abhiram/Documents/TBE/evaluation_projects/elasticsearch"), Path("/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/plugin_input_files/classes_and_commits-elasticsearch.json")),
//
            SyntheticParams("", "flink", Path("/Users/abhiram/Documents/TBE/evaluation_projects/flink"), Path("/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/plugin_input_files/classes_and_commits-flink.json")),

            SyntheticParams("", "ghidra", Path("/Users/abhiram/Documents/TBE/evaluation_projects/ghidra"), Path("/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/plugin_input_files/classes_and_commits-ghidra.json")),

            SyntheticParams("", "graal", Path("/Users/abhiram/Documents/TBE/evaluation_projects/graal"), Path("/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/plugin_input_files/classes_and_commits-graal.json")),
//
            SyntheticParams("", "kafka", Path("/Users/abhiram/Documents/TBE/evaluation_projects/kafka"), Path("/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/plugin_input_files/classes_and_commits-kafka.json")),

            SyntheticParams("", "redisson", Path("/Users/abhiram/Documents/TBE/evaluation_projects/redisson"), Path("/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/plugin_input_files/classes_and_commits-redisson.json")),

            SyntheticParams("", "selenium", Path("/Users/abhiram/Documents/TBE/evaluation_projects/selenium"), Path("/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/plugin_input_files/classes_and_commits-selenium.json")),

            SyntheticParams("", "spring-framework", Path("/Users/abhiram/Documents/TBE/evaluation_projects/spring-framework"), Path("/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/plugin_input_files/classes_and_commits-spring-framework.json")),

            SyntheticParams("", "spring-boot", Path("/Users/abhiram/Documents/TBE/evaluation_projects/spring-boot"), Path("/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/plugin_input_files/classes_and_commits-spring-boot.json")),
//
            SyntheticParams("", "ruoyi-vue-pro", Path("/Users/abhiram/Documents/TBE/evaluation_projects/ruoyi-vue-pro"), Path("/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/plugin_input_files/classes_and_commits-ruoyi-vue-pro.json"))

            )

        for (params in projectPaths){
            val project = runBlocking { ProjectUtil
                .openOrImportAsync(
                    params.projectPath,
                    options = OpenProjectTask(forceOpenInNewFrame = true)
                ) }
            Thread.sleep(10000)
            runOnCommits(project!!, params.classesToRunPath)
            saveOutput(params)
        }
    }
    private fun runOnCommits(project: Project, path: Path) {
        val fileText =
            Files.readString(path) ?: return

        val fileAndCommits = JsonParser.parseString(fileText).asJsonArray
        val repo = FileRepositoryBuilder()
            .setGitDir(File("${project.basePath}/.git"))
            .build()
        val gitRepo = Git(repo)


        for (classAndCommit in fileAndCommits) {
            val filePath = classAndCommit.asJsonObject.get("file_path").asString
            val commitHash = classAndCommit.asJsonObject.get("commit_hash").asString
            val className = classAndCommit.asJsonObject.get("class_name").asString
            runBlocking {
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
                        val psiFile = newFile as? PsiJavaFileImpl
                        if (psiFile != null) {
                            val targetClass = findTargetClass(psiFile, className)
                            if (targetClass != null) {
                                super.invokePlugin(project, newEditor, newFile, targetClass)
                            }
                        }
                        invokeLaterFinished = true
                    }
                    runBlocking {
                        waitForBackgroundFinish(30 * 60 * 1000, 1000)
                        invokeLaterFinished = false
                        invokeLater {
                            if (newFile != null)
                                FileEditorManager.getInstance(project).closeFile(newFile!!.virtualFile)
                            invokeLaterFinished = true
                        }
                        waitForBackgroundFinish(30 * 60 * 1000, 1000)
                    }
                }
            }
        }
    }

    private fun findTargetClass(psiFile: PsiJavaFileImpl, className: String): PsiClass? {
        return psiFile.classes.find { it.name == className } ?: psiFile.classes.flatMap { it.allInnerClasses.toList() }.find { it.name == className }
    }

    override fun saveOutput(params: SyntheticParams) {
        val processBuilder = ProcessBuilder(
            "/Users/abhiram/Documents/TBE/tbe/bin/python",
            "/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/src/main/python/mm_analyser/refactoring_miner_processing/automation_helpers/read_from_telemetry.py",
            params.projectName,
            "mm_run3"
        )
        try {
            val process = processBuilder.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                println(line)
            }
            val exitCode = process.waitFor()
            println("\nExited with code : $exitCode")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}