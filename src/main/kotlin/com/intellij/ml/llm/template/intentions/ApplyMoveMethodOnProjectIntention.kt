package com.intellij.ml.llm.template.intentions

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.ProjectUtil
import com.intellij.ml.llm.template.LLMBundle
import com.intellij.ml.llm.template.utils.openFileFromQualifiedName
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.progress.util.BackgroundTaskUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import dev.langchain4j.data.message.ChatMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Path
import kotlin.io.path.Path


open class ApplyMoveMethodOnProjectIntention: ApplyMoveMethodInteractiveIntention() {

    data class SyntheticParams(
        val size: String,
        val projectName: String,
        val projectPath: Path,
        val classesToRunPath: Path
    ){}

    protected val mutex = Mutex()
    init {
        showSuggestions=false
    }
    companion object{
        const val FILE_LIMIT = 5
    }
    protected var invokeLaterFinished = true

    override fun invokeLLM(project: Project, promptIterator: Iterator<MutableList<ChatMessage>>, editor: Editor, file: PsiFile) {
        super.invokeLLM(project, promptIterator, editor, file)
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val task = object : Task.Backgroundable(
            project, LLMBundle.message("intentions.request.extract.function.background.process.title")
        ) {
            override fun run(indicator: ProgressIndicator) {
//                val p = runBlocking { ProjectUtil
//                    .openOrImportAsync(
//                        Path("/Users/abhiram/Documents/TBE/jmove/dataset-tse/drjava/large/big/drjava-stable-20100913-r5387"),
//                        options = OpenProjectTask(forceOpenInNewFrame = true)
//                    ) }
//
                runPluginOnSpecificFiles(project, Path("classNamesFile"))
//                runSyntheticEvaluation()
            }
        }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    fun runSyntheticEvaluation(){
        val projectPaths = listOf(
            SyntheticParams("large", "ant", Path("/Users/abhiram/Documents/TBE/jmove/dataset-tse/ant/large/big/ant-1.8.2"), Path("/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/qualified_classes_ant_large.txt")),
            SyntheticParams("small", "ant", Path("/Users/abhiram/Documents/TBE/jmove/dataset-tse/ant/small/small/ant-1.8.2"), Path("/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/qualified_classes_ant_small.txt")),
            SyntheticParams("large", "mvnforum", Path("/Users/abhiram/Documents/TBE/jmove/dataset-tse/mvnforum/large/big/mvnforum-1.2.2-ga"), Path("/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/qualified_classes_mvnforum_large.txt")),
            SyntheticParams("small", "mvnforum", Path("/Users/abhiram/Documents/TBE/jmove/dataset-tse/mvnforum/small/small/mvnforum-1.2.2-ga"), Path("/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/qualified_classes_mvnforum_small.txt")),
            SyntheticParams("large", "lucene", Path("/Users/abhiram/Documents/TBE/jmove/dataset-tse/lucene/large/big/lucene-4.2.0"), Path("/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/qualified_classes_lucene_large.txt")),
            SyntheticParams("small", "lucene", Path("/Users/abhiram/Documents/TBE/jmove/dataset-tse/lucene/small/small/lucene-4.2.0"), Path("/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/qualified_classes_lucene_small.txt")),
            SyntheticParams("large", "junit", Path("/Users/abhiram/Documents/TBE/jmove/dataset-tse/junit/large/big/junit-4.10"), Path("/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/qualified_classes_junit_large.txt")),
            SyntheticParams("small", "junit", Path("/Users/abhiram/Documents/TBE/jmove/dataset-tse/junit/small/small/junit-4.10"), Path("/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/qualified_classes_junit_small.txt")),
            SyntheticParams("large", "jgroups", Path("/Users/abhiram/Documents/TBE/jmove/dataset-tse/jgroups/large/big/jgroups-2.10.0"), Path("/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/qualified_classes_jgroups_large.txt")),
            SyntheticParams("small", "jgroups", Path("/Users/abhiram/Documents/TBE/jmove/dataset-tse/jgroups/small/small/jgroups-2.10.0"), Path("/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/qualified_classes_jgroups_small.txt")),
            SyntheticParams("large", "jfreechart", Path("/Users/abhiram/Documents/TBE/jmove/dataset-tse/jfreechart/large/big/jfreechart-1.0.13"), Path("/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/qualified_classes_jfreechart_large.txt")),
            SyntheticParams("small", "jfreechart", Path("/Users/abhiram/Documents/TBE/jmove/dataset-tse/jfreechart/small/small/jfreechart-1.0.13"), Path("/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/qualified_classes_jfreechart_small.txt")),
            SyntheticParams("large", "derby", Path("/Users/abhiram/Documents/TBE/jmove/dataset-tse/derby/large/big/derby-10.9.1.0"), Path("/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/qualified_classes_derby_large.txt")),
            SyntheticParams("small", "derby", Path("/Users/abhiram/Documents/TBE/jmove/dataset-tse/derby/small/small/derby-10.9.1.0"), Path("/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/qualified_classes_derby_small.txt")),
            SyntheticParams("large", "jhotdraw", Path("/Users/abhiram/Documents/TBE/jmove/dataset-tse/jhotdraw/large/big/JHotDraw 7.6 original"), Path("/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/qualified_classes_jhotdraw_large.txt")),
            SyntheticParams("small", "jhotdraw", Path("/Users/abhiram/Documents/TBE/jmove/dataset-tse/jhotdraw/small/small/JHotDraw 7.6 original"), Path("/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/qualified_classes_jhotdraw_small.txt")),
            SyntheticParams("large", "drjava", Path("/Users/abhiram/Documents/TBE/jmove/dataset-tse/drjava/large/big/drjava-stable-20100913-r5387"), Path("/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/qualified_classes_drjava_large.txt")),
            SyntheticParams("small", "drjava", Path("/Users/abhiram/Documents/TBE/jmove/dataset-tse/drjava/small/small/drjava-stable-20100913-r5387"), Path("/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/qualified_classes_drjava_small.txt")),
            SyntheticParams("large", "tapestry", Path("/Users/abhiram/Documents/TBE/jmove/dataset-tse/tapestry/large/big/tapestry-5.1.0.5"), Path("/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/qualified_classes_tapestry_large.txt")),
            SyntheticParams("small", "tapestry", Path("/Users/abhiram/Documents/TBE/jmove/dataset-tse/tapestry/small/small/tapestry-5.1.0.5"), Path("/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/qualified_classes_tapestry_small.txt")),
            SyntheticParams("large", "jtopen", Path("/Users/abhiram/Documents/TBE/jmove/dataset-tse/jtopen/large/big/jtopen-7.8"), Path("/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/qualified_classes_jtopen_large.txt")),
            SyntheticParams("small", "jtopen", Path("/Users/abhiram/Documents/TBE/jmove/dataset-tse/jtopen/small/small/jtopen-7.8"), Path("/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/qualified_classes_jtopen_small.txt")),

            )

        for (params in projectPaths){
            val project = runBlocking { ProjectUtil
                .openOrImportAsync(
                    params.projectPath,
                    options = OpenProjectTask(forceOpenInNewFrame = true)
                ) }
            Thread.sleep(10000)
            runPluginOnInputFile(params.classesToRunPath, project!!)
            saveOutput(params)
        }

    }

    open fun saveOutput(params: SyntheticParams){

        val processBuilder = ProcessBuilder(
            "/Users/abhiram/Documents/TBE/tbe/bin/python",
            "/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/src/main/python/mm_analyser/jmove_dataset/get_qualifiednames_from_goldset.py",
            params.projectName,
            params.size,
            "mm_run4"
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

    open protected fun runPluginOnSpecificFiles(project: Project, classNamesFile: Path) {
        runSyntheticEvaluation()
    }

    private fun runPluginOnInputFile(classNamesFile: Path, project: Project) {
        val allClasses = File(classNamesFile.toString()).readLines()
        val basePath = project.basePath!!
        for (filePath in allClasses) {
            runBlocking {
                mutex.withLock {
                    invokeLaterFinished = false
                    invokeLater {
                        val editorFilePair = openFileFromQualifiedName(filePath, project)
                        val newEditor = editorFilePair.first
                        val newFile = editorFilePair.second
    //                       val innerClass = (newFile as PsiJavaFileImpl).classes[0]
    //                       val className = innerClass.getChildOfType<PsiIdentifier>()!!
    //                       newEditor.selectionModel.setSelection(className.startOffset, className.startOffset + 1)
                        super.invoke(project, newEditor, newFile)
                        invokeLaterFinished = true
                    }
                    runBlocking { waitForBackgroundFinish(100 * 60 * 1000, 1000) } // wait a maximum of 100 minutes.
                }
            }
        }
    }

    tailrec suspend fun waitForBackgroundFinish(maxDelay: Long, checkPeriod: Long) : Boolean{
        if(maxDelay < 0) return false
        if(invokeLaterFinished && finishedBackgroundTask==true) return true
        delay(checkPeriod)
        return waitForBackgroundFinish(maxDelay - checkPeriod, checkPeriod)
    }
    fun waitForImportFinish(project: Project){
//        val tasks: List<Task?> = BackgroundTaskUtil.getRunningBackgroundTasks(ProgressManager.getInstance())
        BackgroundTaskUtil()
    }
}