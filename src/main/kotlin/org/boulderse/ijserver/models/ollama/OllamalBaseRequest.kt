package org.boulderse.ijserver.models.ollama

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.boulderse.ijserver.models.LLMBaseRequest
import org.boulderse.ijserver.models.openai.OpenAIChatResponse
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.io.HttpRequests
import java.net.HttpURLConnection

open class OllamalBaseRequest<Body>(path: String, body: Body) : LLMBaseRequest<Body>(body) {
    private val url = "http://localhost:11434/api/$path"
    private val logger = Logger.getInstance(javaClass)

    private fun decodeResponse(response: String): String {
        var content = ""
        val jsonParts = response.split("}\n{")
        for ((index, jsonPart) in jsonParts.withIndex()) {
            var jsonPartCorrected = "null";
            if (index != 0) {
                jsonPartCorrected = "{$jsonPart}"
            }else{
                jsonPartCorrected = "$jsonPart}"
            }

            try {
                content += Gson().fromJson(jsonPartCorrected, OllamaResponse::class.java)
                                 .message.content
//                content +=
//                    Json.decodeFromString<DecodeResponse>(jsonPartCorrected)!!.response
            } catch (e: Exception) {
                logger.debug("Error decoding JSON part $jsonPart: ${e.message}")
            }
        }
        return content
    }

    private fun getLastJson(response: String): OllamaResponse{
        val lastJson = "{"+response.split("}\n{").last()
        return Gson().fromJson(lastJson, OllamaResponse::class.java)
    }

    override fun sendSync(): OllamaResponse? {
        val jsonBody = GsonBuilder().create().toJson(body)

        return HttpRequests.post(url, "application/json")
//            .tuner {
//                it.setRequestProperty("Authorization", "Bearer $apiKey")
//                CredentialsHolder.getInstance().getOpenAiOrganization()?.let { organization ->
//                    it.setRequestProperty("OpenAI-Organization", organization)
//                }
//            }
            .connect { request -> request.write(jsonBody)
                val responseCode = (request.connection as HttpURLConnection).responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = request.readString()
                    Logger.getInstance("#com.intellij.ml.llm").info("Raw response:\n${response}")
                    val responseDecoded = decodeResponse(response)
                    val ollamaResponse = getLastJson(response)
                    ollamaResponse.message.content = responseDecoded
                    ollamaResponse
                } else {
                    null
                }
            }
    }
}