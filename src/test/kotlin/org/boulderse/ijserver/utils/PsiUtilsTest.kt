package org.boulderse.ijserver.utils

import org.junit.Test

class PsiUtilsTest{

    @Test
    fun testCosineSimilarity(){
        val text1 = "The quick brown fox"
        val text2 = "The brown fox jumps"
        assert(PsiUtils.computeCosineSimilarity(text1, text2)==0.75)
    }
}