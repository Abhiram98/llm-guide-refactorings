package com.intellij.ml.llm.template.utils

import dev.langchain4j.model.voyageai.VoyageAiEmbeddingModelName
import kotlin.test.Test

class VoyageAiEmbeddingModelITTest{

    @Test
    fun testComputeVoyageAiCosineSimilarity() {
        val text1 = "public void method() { System.out.println(\"Hello World\"); }"
        val text2 = "public class MyClass { public void method() { System.out.println(\"Hello World\"); } }"
        val voyageAiEmbeddingModelIT = VoyageAiEmbeddingModelIT()
        val result = voyageAiEmbeddingModelIT.computeVoyageAiCosineSimilarity(text1, text2, VoyageAiEmbeddingModelName.VOYAGE_3_LITE)
        println(result)
    }
}