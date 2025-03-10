package com.intellij.ml.llm.template.utils

import kotlin.test.Test

class CodeBertScoreTest {

    @Test
    fun testCodeBertScore() {
        var text1 = "foo"
        var text2 = "bar"
        var score = CodeBertScore.computeCodeBertScore(text1, text2)
        print(score)
    }
}