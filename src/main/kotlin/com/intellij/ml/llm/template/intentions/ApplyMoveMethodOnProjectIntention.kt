package com.intellij.ml.llm.template.intentions

import com.intellij.ml.llm.template.LLMBundle
import com.intellij.ml.llm.template.utils.findAllFilesWithExtension
import com.intellij.ml.llm.template.utils.openFile
import com.intellij.ml.llm.template.utils.openFileFromQualifiedName
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.impl.source.PsiJavaFileImpl
import dev.langchain4j.data.message.ChatMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.math.min

class ApplyMoveMethodOnProjectIntention: ApplyMoveMethodInteractiveIntention() {

    private val mutex = Mutex()
    init {
        showSuggestions=false
    }
    companion object{
        const val FILE_LIMIT = 5
    }
    private var invokeLaterFinished = true

    override fun invokeLLM(project: Project, messageList: MutableList<ChatMessage>, editor: Editor, file: PsiFile) {
        super.invokeLLM(project, messageList, editor, file)
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val task = object : Task.Backgroundable(
            project, LLMBundle.message("intentions.request.extract.function.background.process.title")
        ) {
            override fun run(indicator: ProgressIndicator) {
                runPluginOnSpecificFiles(project)
            }
        }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    private fun runPluginOnSpecificFiles(project: Project) {
        val allClasses = File("/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/qualified_classes.txt").readLines()
        val basePath = project.basePath!!
        for (filePath in allClasses) {
           runBlocking{
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
                   runBlocking{ waitForBackgroundFinish(5 * 60 * 1000, 1000) }
               }
           }
        }
    }

    private fun filterOutThisFile(file: PsiFile): Boolean{
        val innerClasses = (file as PsiJavaFileImpl).classes
        if (innerClasses.isNotEmpty()){
            return innerClasses[0].isInterface
        }
        return true
    }
    tailrec suspend fun waitForBackgroundFinish(maxDelay: Long, checkPeriod: Long) : Boolean{
        if(maxDelay < 0) return false
        if(invokeLaterFinished && finishedBackgroundTask==true) return true
        delay(checkPeriod)
        return waitForBackgroundFinish(maxDelay - checkPeriod, checkPeriod)
    }
}