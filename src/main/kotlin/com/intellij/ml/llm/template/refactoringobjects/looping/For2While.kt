package com.intellij.ml.llm.template.refactoringobjects.looping

import com.intellij.ml.llm.template.refactoringobjects.CodeInspectionFactory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiForStatement
import com.siyeh.ig.controlflow.ForLoopReplaceableByWhileInspection

class For2While {
    companion object{

        val preview = fun(element: PsiElement): String{
            return "Convert For loop to While loop"
        }

        val factory = CodeInspectionFactory(
            "Convert For Loop to While Loop",
            "convert_for2while",
            """def convert_for2while(line_start):
    ""${'"'}
    Converts for-loops to while-loops where applicable.

    This function refactors code by replacing for-loops with equivalent while-loops, starting from the specified 
    line number `line_start`. It assumes that the necessary updates to the source code are handled externally.

    Parameters:
    - line_start (int): The line number from which to start searching for for-loops to convert. Must be a positive integer.
    ""${'"'}
""".trimIndent(),
            PsiForStatement::class.java,
            ForLoopReplaceableByWhileInspection(),
            preview,
            true
        )
    }
}