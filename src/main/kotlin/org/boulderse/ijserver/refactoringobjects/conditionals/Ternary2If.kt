package org.boulderse.ijserver.refactoringobjects.conditionals

import com.intellij.psi.PsiConditionalExpression
import com.intellij.psi.PsiElement
import com.siyeh.ig.controlflow.ConditionalExpressionInspection
import org.boulderse.ijserver.refactoringobjects.CodeInspectionFactory
import org.boulderse.ijserver.refactoringobjects.MyRefactoringFactory

class Ternary2If {
    companion object {
        val preview = fun(element: PsiElement): String = "Convert Ternary Operator to If Statements"
        val factory =
            CodeInspectionFactory<PsiConditionalExpression, MyRefactoringFactory>(
                "Convert Ternary Operator to If Statements",
                "convert_ternary2if",
                """
                def convert_ternary2if(line_start):
                ""${'"'}
                Converts ternary (conditional) expressions to if-else statements where applicable.

                This function refactors code by replacing ternary (conditional) expressions with equivalent if-else statements,
                starting from the specified line number `line_start`. It assumes that the necessary updates to the source code
                are handled externally.

                Parameters:
                - line_start (int): The line number from which to start searching for ternary expressions to convert. Must be a positive integer.
                ""${'"'}
                """.trimIndent(),
                PsiConditionalExpression::class.java,
                ConditionalExpressionInspection(),
                preview,
                reverseRefactoringFactory = If2Ternary.factory,
            )
    }
}
