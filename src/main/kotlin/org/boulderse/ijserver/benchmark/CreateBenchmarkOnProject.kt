package org.boulderse.ijserver.benchmark

import com.google.gson.JsonParser
import com.intellij.openapi.application.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import kotlinx.coroutines.runBlocking
import okhttp3.internal.wait
import java.nio.file.Files
import java.nio.file.Path

class CreateBenchmarkOnProject(
    val projectPath: String,
    val refMinerOut: String,
    val project: Project
) {

    fun create(){
        val jsonContent = Files.readString(Path.of(refMinerOut))
        val json = JsonParser.parseString(jsonContent)
        for (filename in json.asJsonObject.keySet()) {
            val commitInfo = json.asJsonObject[filename].asJsonArray[0]
            val commitHash = commitInfo.asJsonObject["sha1"].asString

            // Checkout commit

            // Open file
            val editorFilePair = try {
                openFile(filename)
            } catch (e: Exception) {
                print("Skipping. File not found")
                continue
            }
            val editor = editorFilePair.first
            val file = editorFilePair.second

            val refactorings = commitInfo.asJsonObject["refactorings"].asJsonArray
            CreateBenchmarkForFile(filename, project, editor, file, refactorings).create()

            // Make commit and checkout branch/tag
        }
    }

    private fun openFile(filePath: String): Pair<Editor, PsiFile> {
//        runWriteAction {  }
        var ret : Pair<Editor, PsiFile>? = null
         invokeAndWaitIfNeeded {
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
         }.wait()

        return ret!!
    }

}