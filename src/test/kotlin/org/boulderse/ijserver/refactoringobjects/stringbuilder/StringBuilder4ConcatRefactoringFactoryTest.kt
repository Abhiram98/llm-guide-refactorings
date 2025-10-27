package org.boulderse.ijserver.refactoringobjects.stringbuilder

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiAssignmentExpression
import com.intellij.psi.PsiConditionalExpression
import com.intellij.psi.PsiPolyadicExpression
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase
import com.intellij.refactoring.suggested.startOffset
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import com.siyeh.ig.controlflow.ConditionalExpressionInspection
import com.siyeh.ig.migration.StringBufferReplaceableByStringBuilderInspection
import com.siyeh.ig.performance.StringConcatenationInLoopsInspection
import com.siyeh.ipp.concatenation.ReplaceConcatenationWithStringBufferIntention
import org.jetbrains.kotlin.idea.base.psi.getLineStartOffset
import org.jetbrains.kotlin.idea.core.moveCaret
import org.junit.jupiter.api.Assertions.*

class StringBuilder4ConcatRefactoringFactoryTest : LightPlatformCodeInsightTestCase() {
    private var projectPath = "src/test"

    override fun getTestDataPath(): String = projectPath

    fun testStringBuilder() {
        configureByFile("/testdata/HelloWorld.java")
        editor.moveCaret(file.getLineStartOffset(63)!!)
        val refObjs =
            StringBuilderRefactoringFactory.createObjectsFromFuncCall(
                "use_string_builder('s')",
                project,
                editor,
                file,
            )
        assert(refObjs.isNotEmpty())
        refObjs[0].performRefactoring(project, editor, file)

        println(file.text)
        assert(
            file.text.contains(
                "    public static void constructString(Integer a, Boolean b){\n" +
                    "        String s = new StringBuilder().append(\"String: \").append(\"s\").append(a).append(b).toString();\n" +
                    "        System.out.println(s);\n" +
                    "    }",
            ),
        )
    }

    fun testStringBuilderLoop() {
        configureByFile("/testdata/HelloWorld.java")

        editor.moveCaret(file.getLineStartOffset(55)!!)
        val refObjs =
            StringBuilderRefactoringFactory.createObjectsFromFuncCall(
                "use_string_builder('result')",
                project,
                editor,
                file,
            )
        assert(refObjs.isNotEmpty())
        refObjs[0].performRefactoring(project, editor, file)

        println(file.text)
        assert(
            file.text.contains(
                "        StringBuilder result = new StringBuilder(\"Array: \");\n" +
                    "\n" +
                    "        for(int i = 0; i < array.size(); i++) {\n" +
                    "            result.append(\"Element: \").append(i + 1);\n" +
                    "            result.append(array.get(i).toString());\n" +
                    "            result.append(\"\\n\");\n" +
                    "        }\n" +
                    "        System.out.println(result);",
            ),
        )
    }
}
