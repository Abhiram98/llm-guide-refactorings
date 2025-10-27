package org.boulderse.ijserver.models.grazie

import org.boulderse.ijserver.models.LLMBaseRequest
import org.boulderse.ijserver.models.LLMRequestProvider
import org.boulderse.ijserver.models.openai.OpenAiChatRequestBody

class GrazieRequestProvider(
    completionModel: String,
    editModel: String,
    chatModel: String,
) : LLMRequestProvider(completionModel, editModel, chatModel) {
    override fun createChatGPTRequest(body: OpenAiChatRequestBody): LLMBaseRequest<*> = GrazieBaseRequest(body)
}

val GrazieGPT4RequestProvider =
    GrazieRequestProvider("GPT-4", "GPT-4", "GPT-4")
