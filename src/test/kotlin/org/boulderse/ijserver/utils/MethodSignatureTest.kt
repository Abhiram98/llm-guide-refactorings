package org.boulderse.ijserver.utils

import org.junit.Test
import org.junit.jupiter.api.Assertions.*

class MethodSignatureTest{

    @Test
    fun testSignatureGeneration(){
        val methodSignatureString = "public acquireShared(resourceType Locks.ResourceType, resourceIds long...) : void"
        val signature = MethodSignature.getMethodSignatureParts(methodSignatureString)
        print(signature)
        assert(signature!!.paramsList[0].type=="Locks.ResourceType")
        assert(signature.paramsList[1].type=="long...")
    }

    @Test
    fun testSignatureGenerationCommaInParam(){
        val methodSignatureString = "private executeIndexedQuery(cache AdvancedCache<byte[],byte[]>, cacheConfiguration Configuration, serCtx SerializationContext, request QueryRequest) : QueryResponse"
        val signature = MethodSignature.getMethodSignatureParts(methodSignatureString)
        print(signature)
        assert(signature!!.paramsList[0].type=="AdvancedCache<byte[],byte[]>")
    }

    @Test
    fun testSignatureGenerationQuestionMarkInParam(){
        val methodSignatureString = "private buildLuceneQuery(cache AdvancedCache<?,?>, isCompatMode boolean, serCtx SerializationContext, jpqlString String, startOffset long, maxResults int) : Query"
        val signature = MethodSignature.getMethodSignatureParts(methodSignatureString)
        print(signature)
        assert(signature!!.paramsList[0].type=="AdvancedCache<?,?>")
    }


}