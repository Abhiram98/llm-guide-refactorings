package org.boulderse.ijserver.refactoringobjects.conditionals

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIfStatement
import com.siyeh.ig.migration.IfCanBeSwitchInspection
import org.boulderse.ijserver.refactoringobjects.CodeInspectionFactory
import org.boulderse.ijserver.refactoringobjects.MyRefactoringFactory

class If2Switch {
    companion object {
        val preview = fun(element: PsiElement): String = "Convert If to Switch"

        val factory =
            CodeInspectionFactory(
                "Convert If to Switch",
                "convert_if2switch",
                """
                def convert_if2switch(line_start):
                ""${'"'}
                Converts a series of if-elif-else statements to a switch-case statement where applicable.

                This function refactors code by replacing a series of if-elif-else statements with a switch-case statement,
                starting from the specified line number `line_start`. It assumes that the necessary updates to the source code
                are handled externally.

                Parameters:
                - line_start (int): The line number from which to start searching for if-elif-else statements to convert. Must be a positive integer.
                ""${'"'}
                """.trimIndent(),
                PsiIfStatement::class.java,
                getInspectionObj(),
                preview,
                reverseRefactoringFactory = Switch2IfFactory,
            )

        private fun getInspectionObj(): IfCanBeSwitchInspection {
            val inspection = IfCanBeSwitchInspection()
            inspection.minimumBranches = 1
            inspection.suggestIntSwitches = true
            inspection.suggestEnumSwitches = true
            inspection.onlySuggestNullSafe = false
            return inspection
        }
    }
}
