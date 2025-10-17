package org.boulderse.ijserver.refactoringobjects.conditionals

import com.intellij.codeInspection.EnhancedSwitchMigrationInspection
import org.boulderse.ijserver.refactoringobjects.CodeInspectionFactory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiSwitchStatement
class EnhancedSwitchFactory {
    companion object{
        val preview = fun(switchStatement: PsiElement): String{
            return "Use Enhanced Switch"
        }


        val factory = CodeInspectionFactory(
            "Use Enhanced Switch",
            "use_enhanced_switch",
            """def use_enhanced_switch(line_start):
    ""${'"'}
    Converts a conventional switch-case statement to an enhanced switch expression where applicable.

    This function refactors code by replacing conventional switch-case statements with enhanced switch expressions 
    starting from the specified line number `line_start`. It assumes that 
    the necessary updates to the source code are handled externally.

    Parameters:
    - line_start (int): The line number from in which the switch-case statements to convert are present. Must be a positive integer.
    ""${'"'}
    """.trimIndent(),
            PsiSwitchStatement::class.java,
            EnhancedSwitchMigrationInspection(),
            preview,
            false,
            reverseRefactoringFactory = OldStyleSwitch.factory
        )
    }
}


