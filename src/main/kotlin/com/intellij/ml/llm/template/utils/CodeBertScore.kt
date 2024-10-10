package com.intellij.ml.llm.template.utils

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.output.Response
import dev.langchain4j.model.voyageai.VoyageAiEmbeddingModel
import dev.langchain4j.model.voyageai.VoyageAiEmbeddingModelName
import dev.langchain4j.store.embedding.CosineSimilarity
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.Json
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.io.IOException
import java.net.Proxy
import java.time.Duration
import java.util.Arrays.asList
import java.util.concurrent.TimeUnit


@EnabledIfEnvironmentVariable(named = "VOYAGE_API_KEY", matches = ".+")
class VoyageAiEmbeddingModelIT {

    fun computeVoyageAiCosineSimilarity(psiMethod: PsiMethod, psiClass: PsiClass, modelName: VoyageAiEmbeddingModelName): Double {
        val methodBody = psiMethod.text
        val classBody = psiClass.text
        return computeVoyageAiCosineSimilarity(methodBody, classBody, modelName)
    }

    fun computeVoyageAiCosineSimilarity(text1: String, text2: String, modelName: VoyageAiEmbeddingModelName): Double {
        // given

        val model: EmbeddingModel = VoyageAiEmbeddingModel.builder()
            .apiKey("pa-6GcL1W5Z4KBXn0zXcrHu4Dg9iF8_uHi25rQdpUVcXXk")
            .modelName(modelName)
            .timeout(Duration.ofSeconds(60))
            .inputType("query")
            .logRequests(true)
            .logResponses(true)
            .build()

        val segment1 = TextSegment.from(text1)
        val segment2 = TextSegment.from(text2)

        // when
        val response: Response<List<Embedding>> = model.embedAll(asList(segment1, segment2))

        val embedding1: Embedding = response.content().get(0)

        val embedding2: Embedding = response.content().get(1)

        return CosineSimilarity.between(embedding1, embedding2)
    }
}
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

        private fun createHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .proxy(Proxy.NO_PROXY)  // Disable proxy
                .protocols(listOf(Protocol.HTTP_1_1))
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .connectionPool(ConnectionPool(0, 1, TimeUnit.MILLISECONDS)) // Disable connection pooling
                .build()
        }

        fun computeCodeBertScore(text1: String, text2: String): Double {
            // Parse the URL to get host and port
            val urlString = "https://438b-141-142-254-176.ngrok-free.app/compute_codebertscore"

            val client = createHttpClient()
            val requestData = CodeBertRequest(text1, text2)
            val jsonBody = json.encodeToString(CodeBertRequestSerializer, requestData)

            val requestBody = jsonBody.toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url(urlString)
                .addHeader("Content-Type", "application/json")
                .addHeader("Connection", "close")
                .post(requestBody)
                .build()

            return try {
                client.newCall(request).execute().use { response ->
                    when {
                        !response.isSuccessful -> {
                            println("HTTP error: ${response.code}")
                            println("Response body: ${response.body?.string()}")
                            -2.0
                        }
                        response.body == null -> {
                            println("Null response body")
                            -3.0
                        }
                        else -> {
                            val responseBody = response.body!!.string()
                            println("Received response: $responseBody")
                            try {
                                json.decodeFromString(CodeBertResponseSerializer, responseBody).score
                            } catch (e: SerializationException) {
                                println("Failed to parse response: $responseBody")
                                throw e
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                println("Network error: ${e.message}")
                e.printStackTrace()
                -4.0
            } catch (e: SerializationException) {
                println("JSON parsing error: ${e.message}")
                e.printStackTrace()
                -5.0
            } catch (e: Exception) {
                println("Unexpected error: ${e::class.simpleName} - ${e.message}")
                e.printStackTrace()
                -6.0
            }
        }
    }
}