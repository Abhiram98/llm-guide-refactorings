package com.intellij.ml.llm.template.refactoringobjects.conditionals

import com.intellij.codeInspection.EnhancedSwitchBackwardMigrationInspection
import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.refactoringobjects.CodeInspectionFactory
import com.intellij.ml.llm.template.refactoringobjects.MyRefactoringFactory
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiIfStatement
import com.intellij.psi.PsiSwitchStatement

class OldStyleSwitch {
    companion object{
        val preview = fun(element: PsiElement): String{
            return "Convert to an old-style switch"
        }
        private val reverseFactory : MyRefactoringFactory = EnhancedSwitchFactory.factory
        val factory = CodeInspectionFactory<PsiSwitchStatement, MyRefactoringFactory>(
            "Replace with old style switch",
            "convert_to_old_switch",
            """def convert_to_old_switch(line_start):
    ""${'"'}
    Converts an enhanced switch statement into an old-style switch statement..

    This function refactors code by replacing an enhanced switch statement with an old-style switch statement,
    starting from the specified line number `line_start`. It assumes that the necessary updates to the source code
    are handled externally.

    Parameters:
    - line_start (int): The line number from which to start searching for the switch statement to convert. Must be a positive integer.
    ""${'"'}
""".trimIndent(),
            PsiSwitchStatement::class.java,
            EnhancedSwitchBackwardMigrationInspection(),
            preview,
            reverseRefactoringFactory = reverseFactory
        )
    }
}