package com.intellij.ml.llm.template.models.ollama

import com.google.gson.annotations.SerializedName
import com.intellij.ml.llm.template.models.LLMBaseResponse
import com.intellij.ml.llm.template.models.LLMResponseChoice
import kotlinx.serialization.json.JsonObject

data class OllamaResponse(
    @SerializedName("model")
    val model: String,

    @SerializedName("createdAt")
    val createdAtTz: String,

    @SerializedName("message")
    val message: OllamaMessage,

    @SerializedName("done")
    val done: Boolean
): LLMBaseResponse {
    override fun getSuggestions(): List<LLMResponseChoice> {
        return listOf(LLMResponseChoice(message.content, if(done)"completed" else "incomplete"))
    }
}


data class OllamaMessage(
    @SerializedName("role")
    val role: String,

    @SerializedName("content")
    var content: String
){

}