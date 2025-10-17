package org.boulderse.ijserver.refactoringobjects.extractfunction

import org.boulderse.ijserver.utils.CodeTransformer
import org.boulderse.ijserver.utils.EFObserver
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import org.junit.jupiter.api.Assertions.*

class InlineMethodFactoryTest: LightPlatformCodeInsightTestCase() {
    private var projectPath = "src/test"
    override fun getTestDataPath(): String {
        return projectPath
    }


    fun testInlineMethod() {
        configureByFile("/testdata/HelloWorld.java")

        val refObjs = InlineMethodFactory.createObjectsFromFuncCall(
            "inline_method(\"constructString\")", project, editor, file
        )

        assert(refObjs.isNotEmpty())
        refObjs[0].performRefactoring(project, editor, file)
        print(file.text)
    }
}