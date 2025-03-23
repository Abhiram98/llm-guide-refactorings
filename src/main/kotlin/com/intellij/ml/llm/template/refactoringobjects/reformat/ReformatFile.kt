package com.intellij.ml.llm.template.refactoringobjects.reformat

import com.intellij.codeInsight.actions.ReformatCodeAction
import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.SelectionModelImpl
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.jetbrains.rd.generator.nova.array

class ReformatFile {
    companion object {
        fun doReformat(file: PsiFile, startOffset: Int, endOffset: Int) {
            val processor = ReformatCodeProcessor(file, arrayOf(TextRange(startOffset, endOffset)))
            processor.run()

        }
    }
}