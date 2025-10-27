package org.boulderse.ijserver.utils

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import org.boulderse.ijserver.server.FindReplaceParams
import java.io.File
import kotlin.io.path.Path

class FileUtils {
    companion object {
        fun replaceFileContents(
            filePath: java.nio.file.Path,
            newContents: String,
        ) {
            val file = File(filePath.toString()).writeText(newContents)
        }

        fun replaceFileContentsInRange(
            filePath: java.nio.file.Path,
            startOffset: Int,
            endOffset: Int,
            newText: String,
        ) {
            val originalContent = File(filePath.toString()).readText()
            val newContent = originalContent.replaceRange(startOffset, endOffset, newText)
            File(filePath.toString()).writeText(newContent)
        }

        fun createFile(filePath: java.nio.file.Path) {
            File(filePath.toString()).createNewFile()
        }

        fun textBasedFindReplace(
            params: FindReplaceParams,
            editor: Editor,
            file: PsiFile,
        ): Boolean {
            var found1 = false
            val oldContents = runReadAction { editor.document.text }
            val newContents =
                if (params.lineNum != null) {
                    oldContents
                        .split("\n")
                        .mapIndexed { index, s ->
                            if (index + 1 == params.lineNum) {
                                found1 = params.findText in s
                                s.replace(params.findText, params.replaceText)
                            } else {
                                s
                            }
                        }.joinToString("\n")
                } else {
                    found1 = params.findText in oldContents
                    oldContents.replace(params.findText, params.replaceText)
                }

            FileUtils.replaceFileContents(
                Path(file.virtualFile.path),
                newContents,
            )

            return found1
        }
    }
}
