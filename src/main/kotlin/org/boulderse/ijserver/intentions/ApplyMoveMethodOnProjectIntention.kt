package org.boulderse.ijserver.intentions

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
import org.boulderse.ijserver.LLMBundle
import org.boulderse.ijserver.utils.openFileFromQualifiedName
import java.io.File

open class ApplyMoveMethodOnProjectIntention : ApplyMoveMethodInteractiveIntention() {
    protected val mutex = Mutex()

    init {
        showSuggestions = false
    }

    companion object {
        const val FILE_LIMIT = 5
    }

    protected var invokeLaterFinished = true

    override fun invokeLLM(
        project: Project,
        promptIterator: Iterator<MutableList<ChatMessage>>,
        editor: Editor,
        file: PsiFile,
    ) {
        super.invokeLLM(project, promptIterator, editor, file)
    }

    override fun invoke(
        project: Project,
        editor: Editor?,
        file: PsiFile?,
    ) {
        val task =
            object : Task.Backgroundable(
                project,
                LLMBundle.message("intentions.request.extract.function.background.process.title"),
            ) {
                override fun run(indicator: ProgressIndicator) {
                    runPluginOnSpecificFiles(project)
                }
            }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    protected open fun runPluginOnSpecificFiles(project: Project) {
        val allClasses =
            File(
                "/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/qualified_classes.txt",
            ).readLines()
        val basePath = project.basePath!!
        for (filePath in allClasses) {
            runBlocking {
                mutex.withLock {
                    invokeLaterFinished = false
                    invokeLater {
                        val editorFilePair = openFileFromQualifiedName(filePath, project, false)
                        val newEditor = editorFilePair.first
                        val newFile = editorFilePair.second
//                       val innerClass = (newFile as PsiJavaFileImpl).classes[0]
//                       val className = innerClass.getChildOfType<PsiIdentifier>()!!
//                       newEditor.selectionModel.setSelection(className.startOffset, className.startOffset + 1)
                        super.invoke(project, newEditor, newFile)
                        invokeLaterFinished = true
                    }
                    runBlocking { waitForBackgroundFinish(5 * 60 * 1000, 1000) }
                }
            }
        }
    }

    tailrec suspend fun waitForBackgroundFinish(
        maxDelay: Long,
        checkPeriod: Long,
    ): Boolean {
        if (maxDelay < 0) return false
        if (invokeLaterFinished && finishedBackgroundTask == true) return true
        delay(checkPeriod)
        return waitForBackgroundFinish(maxDelay - checkPeriod, checkPeriod)
    }

    fun waitForImportFinish(project: Project) {
//        val tasks: List<Task?> = BackgroundTaskUtil.getRunningBackgroundTasks(ProgressManager.getInstance())
        BackgroundTaskUtil()
    }
}
