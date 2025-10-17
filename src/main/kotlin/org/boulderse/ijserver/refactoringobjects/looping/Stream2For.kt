package org.boulderse.ijserver.refactoringobjects.looping

import com.intellij.codeInspection.streamToLoop.StreamToLoopInspection
import org.boulderse.ijserver.refactoringobjects.CodeInspectionFactory
import org.boulderse.ijserver.refactoringobjects.MyRefactoringFactory
import org.boulderse.ijserver.refactoringobjects.conditionals.Ternary2If
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiForStatement
import com.intellij.psi.PsiMethodCallExpression
import com.siyeh.ig.controlflow.ForLoopReplaceableByWhileInspection

class Stream2For {
    companion object{
        val preview = fun(element: PsiElement): String{
            return "Convert Stream API to For Loop"
        }

        private val reverseFactory : MyRefactoringFactory = For2Stream.factory
        val factory = CodeInspectionFactory(
            "Convert Stream API to For Loop",
            "convert_stream2for",
            """def convert_stream2for(line_start):
            ""${'"'}
            Converts stream-based operations to conventional for-loops where applicable.
        
            This function refactors code by replacing stream-based operations with conventional for-loops,
            starting from the specified line number `line_start`. It assumes that the necessary updates to the source code
            are handled externally.
        
            Parameters:
            - line_start (int): The line number from which to start searching for the stream operation to convert. Must be a positive integer.
            ""${'"'}
            """.trimIndent(),
            PsiMethodCallExpression::class.java,
            StreamToLoopInspection(),
            preview,
            false,
            reverseRefactoringFactory = reverseFactory
        )
    }
}