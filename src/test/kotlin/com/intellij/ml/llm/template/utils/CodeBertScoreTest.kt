package com.intellij.ml.llm.template.utils
import org.junit.Test

class CodeBertScoreTest {
    @Test
    fun testComputeCodeBertScore_NetworkError() {
        // Call the method and assert the result
        val score = CodeBertScore.computeCodeBertScore("print", "System.out.println")
        println(score)
//        assertEquals(-4.0, score, 0.0)
    }
}