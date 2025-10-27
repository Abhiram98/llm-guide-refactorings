package org.boulderse.ijserver.suggestrefactoring

import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import kotlinx.coroutines.runBlocking
import org.boulderse.ijserver.models.GPTExtractFunctionRequestProvider
import org.boulderse.ijserver.models.grazie.GrazieGPT4

class SimpleRefactoringValidatorTest : LightPlatformCodeInsightTestCase() {
//    private var projectPath = "src/test"
//    override fun getTestDataPath(): String {
//        return projectPath
//    }

    fun testReadResourceFile() {
        val content = SimpleRefactoringValidatorTest::class.java.getResource("/func_src.txt")?.readText()
        println(content)
    }

    fun testGetRefactoringSuggestions() {
        val srcFile = "/testdata/A1_CSC540.java"

        val funcSrc = SimpleRefactoringValidatorTest::class.java.getResource("/func_src.txt")?.readText()
        val llmResponse = SimpleRefactoringValidatorTest::class.java.getResource("/llm_output.txt")?.readText()

        runBlocking {
            val suggestions =
                SimpleRefactoringValidator(
                    GrazieGPT4,
                    project,
                    editor,
                    file,
                    funcSrc!!,
                    mutableMapOf(),
                ).getRefactoringSuggestions(llmResponse!!, 1000)

            println(suggestions)

            assert(suggestions.size == 2)
        }
    }
}
