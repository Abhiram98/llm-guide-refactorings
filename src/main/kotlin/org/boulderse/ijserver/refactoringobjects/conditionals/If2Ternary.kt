package org.boulderse.ijserver.refactoringobjects.conditionals

import org.boulderse.ijserver.refactoringobjects.CodeInspectionFactory
import org.boulderse.ijserver.refactoringobjects.MyRefactoringFactory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIfStatement
import com.siyeh.ig.style.SimplifiableIfStatementInspection

class If2Ternary {
    companion object{
        val preview = fun(element: PsiElement): String{
            return "Use Ternary Operator instead of If statements"
        }
        private val reverseFactory : MyRefactoringFactory = Ternary2If.factory
        val factory = CodeInspectionFactory<PsiIfStatement, MyRefactoringFactory>(
            "Use Ternary Operator",
            "convert_if2ternary",
            """def convert_if2ternary(line_start):
    ""${'"'}
    Converts simple if-else statements to ternary (conditional) expressions where applicable.

    This function refactors code by replacing simple if-else statements with ternary (conditional) expressions,
    starting from the specified line number `line_start`. It assumes that the necessary updates to the source code
    are handled externally.

    Parameters:
    - line_start (int): The line number from which to start searching for if-else statements to convert. Must be a positive integer.
    ""${'"'}
""".trimIndent(),
            PsiIfStatement::class.java,
            getInspectionObject(),
            preview,
            reverseRefactoringFactory = reverseFactory
        )



        private fun getInspectionObject(): SimplifiableIfStatementInspection {
            val inspection = SimplifiableIfStatementInspection()
            inspection.DONT_WARN_ON_TERNARY = false
            inspection.DONT_WARN_ON_CHAINED_ID = false
            return inspection
        }
    }
}