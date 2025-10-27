package org.boulderse.ijserver.refactoringobjects.looping

import com.intellij.codeInspection.streamMigration.StreamApiMigrationInspection
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiForStatement
import org.boulderse.ijserver.refactoringobjects.CodeInspectionFactory

class For2Stream {
    companion object {
        val preview = fun(element: PsiElement): String = "Convert For loop to Java Streams"
        val factory =
            CodeInspectionFactory(
                "Convert For loop to Java Streams",
                "convert_for2stream",
                """
                def convert_for2stream(line_start):
                ""${'"'}
                Converts conventional for-loops to stream-based operations where applicable.
                
                This function refactors code by replacing conventional for-loops with equivalent stream-based operations,
                starting from the specified line number `line_start`. It assumes that the necessary updates to the source code
                are handled externally.
                
                Parameters:
                - line_start (int): The line number from which to start searching for for-loops to convert. Must be a positive integer.
                ""${'"'}
                """.trimIndent(),
                PsiForStatement::class.java,
                getInspection(),
                preview,
                true,
                reverseRefactoringFactory = Stream2For.factory,
            )

        private fun getInspection(): StreamApiMigrationInspection {
            val for2Stream = StreamApiMigrationInspection()
            for2Stream.SUGGEST_FOREACH = true
            for2Stream.REPLACE_TRIVIAL_FOREACH = true
            return for2Stream
        }
    }
}
