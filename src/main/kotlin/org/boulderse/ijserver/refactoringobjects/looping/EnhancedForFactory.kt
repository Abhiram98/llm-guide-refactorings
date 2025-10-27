package org.boulderse.ijserver.refactoringobjects.looping

import com.intellij.psi.*
import com.siyeh.ig.migration.ForCanBeForeachInspection
import org.boulderse.ijserver.refactoringobjects.CodeInspectionFactory

class EnhancedForFactory {
    companion object {
        val preview = fun(forStatement: PsiElement): String {
            return "Use Enhanced For Loop" // TODO: include line number
        }

        val factory =
            CodeInspectionFactory(
                "Use Enhanced For Loop",
                "use_enhanced_forloop",
                """
                def use_enhanced_forloop(line_start):
                ""${'"'}
                Converts a conventional for-loop to an enhanced for-loop where applicable.

                This function is intended to refactor code by replacing conventional for-loops with enhanced for-loops (also known as 
                "foreach" loops) where applicable, starting from the specified line number `line_start`. It assumes that the necessary 
                updates to the source code are handled externally.

                Parameters:
                - line_start (int): The line number from which to start searching for conventional for-loops to convert. Must be a positive integer.
                ""${'"'}
                """.trimIndent(),
                PsiForStatement::class.java,
                ForCanBeForeachInspection(),
                preview,
                false,
                reverseRefactoringFactory = TraditionalForFactory,
            )
    }
}
