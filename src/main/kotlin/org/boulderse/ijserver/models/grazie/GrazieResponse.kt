package org.boulderse.ijserver.models.grazie

import com.google.gson.annotations.SerializedName
import org.boulderse.ijserver.models.LLMBaseResponse
import org.boulderse.ijserver.models.LLMResponseChoice
import org.boulderse.ijserver.models.ollama.OllamaMessage

data class GrazieResponse(
    @SerializedName("llm_response")
    val llmResponse: String,

    @SerializedName("status")
    val status: String
): LLMBaseResponse {
    override fun getSuggestions(): List<LLMResponseChoice> {
        return listOf(LLMResponseChoice(llmResponse, status))
    }
}
