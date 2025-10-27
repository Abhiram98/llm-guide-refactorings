package org.boulderse.ijserver.models.ollama

import org.boulderse.ijserver.models.*
import org.boulderse.ijserver.models.openai.OpenAiChatRequestBody

class OllamaRequestProvider(
    completionModel: String,
    editModel: String,
    chatModel: String,
) : LLMRequestProvider(completionModel, editModel, chatModel) {
    override fun createChatGPTRequest(body: OpenAiChatRequestBody): LLMBaseRequest<*> =
        OllamalBaseRequest<OpenAiChatRequestBody>("chat", body)
}

val MistralChatRequestProvider =
    OllamaRequestProvider("mistral:text", "mistral", "mistral:instruct")
