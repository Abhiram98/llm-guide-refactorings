package com.intellij.ml.llm.template.utils

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.IOException

class CodeBertScore {
    data class CodeBertRequest(val text1: String, val text2: String)

    data class CodeBertResponse(val score: Double)

    @OptIn(ExperimentalSerializationApi::class)
    object CodeBertRequestSerializer : KSerializer<CodeBertRequest> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("CodeBertRequest") {
            element<String>("text1")
            element<String>("text2")
        }

        override fun serialize(encoder: Encoder, value: CodeBertRequest) {
            encoder.encodeStructure(descriptor) {
                encodeStringElement(descriptor, 0, value.text1)
                encodeStringElement(descriptor, 1, value.text2)
            }
        }

        override fun deserialize(decoder: Decoder): CodeBertRequest {
            var text1 = ""
            var text2 = ""
            decoder.decodeStructure(descriptor) {
                while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        0 -> text1 = decodeStringElement(descriptor, 0)
                        1 -> text2 = decodeStringElement(descriptor, 1)
                        CompositeDecoder.DECODE_DONE -> break
                        else -> error("Unexpected index: $index")
                    }
                }
            }
            return CodeBertRequest(text1, text2)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    object CodeBertResponseSerializer : KSerializer<CodeBertResponse> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("CodeBertResponse") {
            element<Double>("score")
        }

        override fun serialize(encoder: Encoder, value: CodeBertResponse) {
            encoder.encodeStructure(descriptor) {
                encodeDoubleElement(descriptor, 0, value.score)
            }
        }

        override fun deserialize(decoder: Decoder): CodeBertResponse {
            var score = 0.0
            decoder.decodeStructure(descriptor) {
                while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        0 -> score = decodeDoubleElement(descriptor, 0)
                        CompositeDecoder.DECODE_DONE -> break
                        else -> error("Unexpected index: $index")
                    }
                }
            }
            return CodeBertResponse(score)
        }
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun computeCodeBertScore(psiMethod: PsiMethod, psiClass: PsiClass): Double {
            val methodBody = psiMethod.text
            val classBody = psiClass.text
            return computeCodeBertScore(methodBody, classBody)
        }

        fun computeCodeBertScore(text1: String, text2: String): Double {
            val client = OkHttpClient()
            val requestData = CodeBertRequest(text1, text2)
            val jsonBody = json.encodeToString(CodeBertRequestSerializer, requestData)
            val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody)
            val request = Request.Builder()
                .url("https://75d447f5c710daed3ef132d70d88e83c.serveo.net/compute_codebertscore")
                .post(requestBody)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        // HTTP error
                        println("HTTP error: ${response.code}")
                        return -2.0
                    }
                    val responseBody = response.body?.string()
                    if (responseBody == null) {
                        // Null response body
                        println("Null response body")
                        return -3.0
                    }
                    val codeBertResponse = json.decodeFromString(CodeBertResponseSerializer, responseBody)
                    return codeBertResponse.score
                }
            } catch (e: IOException) {
                e.printStackTrace()
                return -4.0 // Network error
            } catch (e: SerializationException) {
                e.printStackTrace()
                return -5.0 // JSON parsing error
            } catch (e: Exception) {
                e.printStackTrace()
                return -6.0 // Other errors
            }
        }

    }
}