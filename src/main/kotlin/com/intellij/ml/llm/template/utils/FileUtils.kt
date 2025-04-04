package com.intellij.ml.llm.template.utils

import com.intellij.testFramework.utils.io.createFile
import java.io.File

class FileUtils {
    companion object{
        fun replaceFileContents(filePath: java.nio.file.Path, newContents: String){
            val file = File(filePath.toString()).writeText(newContents)
        }

        fun replaceFileContentsInRange(filePath: java.nio.file.Path, startOffset: Int, endOffset:Int, newText: String){
            val originalContent = File(filePath.toString()).readText()
            val newContent = originalContent.replaceRange(startOffset, endOffset, newText)
            File(filePath.toString()).writeText(newContent)
        }

        fun createFile(filePath: java.nio.file.Path){
            File(filePath.toString()).createNewFile()
        }
    }
}