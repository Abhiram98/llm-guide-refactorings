package org.boulderse.ijserver.refactoringobjects.conditionals

import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import org.boulderse.ijserver.utils.CodeTransformer
import org.boulderse.ijserver.utils.EFObserver
import org.jetbrains.kotlin.idea.core.moveCaret

class EnhancedSwitchFactoryTest : LightPlatformCodeInsightTestCase() {
    private var projectPath = "src/test"

    override fun getTestDataPath(): String = projectPath

    fun testSwitchInspection() {
        val codeTransformer = CodeTransformer()
        val efObserver = EFObserver()
        codeTransformer.addObserver(efObserver)
        configureByFile("/testdata/HelloWorld.java")
        editor.moveCaret(503)
        val refObjs =
            EnhancedSwitchFactory.factory.createObjectsFromFuncCall(
                "use_enhanced_switch(21)",
                project,
                editor,
                file,
            )
        assert(refObjs.isNotEmpty())
        refObjs[0].performRefactoring(project, editor, file)
        println(file.text)
        assert(
            file.text.contains(
                "        switch (num) {\n" +
                    "            case 1 -> System.out.println(\"ONE!!\");\n" +
                    "            case 2 -> System.out.println(\"TWO!!\");\n" +
                    "            case 5 -> System.out.println(\"FIVE!!\");\n" +
                    "            default -> System.out.println(num);\n" +
                    "        }",
            ),
        )
    }
}
