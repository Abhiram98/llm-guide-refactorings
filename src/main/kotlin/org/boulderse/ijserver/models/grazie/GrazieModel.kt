package org.boulderse.ijserver.models.grazie

import ai.grazie.model.llm.annotation.ExperimentalLLM
import ai.grazie.model.llm.profile.LLMProfileID
import ai.grazie.model.llm.profile.OpenAIProfileIDs
import org.boulderse.ijserver.models.openai.OpenAiChatMessage
import org.boulderse.ijserver.models.openai.OpenAiChatRequestBody
import org.boulderse.ijserver.settings.RefAgentSettings
import org.boulderse.ijserver.settings.RefAgentSettingsManager
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.output.Response

class GrazieModel(
    val llm: LLMProfileID
): ChatLanguageModel {
    override fun generate(messages: MutableList<ChatMessage>?): Response<AiMessage> {
        if (messages != null) {
            val response = GrazieBaseRequest(
                OpenAiChatRequestBody(
                    llm,
                    messages.map{
                        OpenAiChatMessage(
                            if(it.type().name=="AI") "assistant" else it.type().name.lowercase(),
                            it.text())
                                },
                    temperature = RefAgentSettingsManager.getInstance().getTemperature()
                )
            ).sendSync()

            return Response(AiMessage.from(response?.getSuggestions()?.get(0)?.text ?: "no-response"))
        }
        return Response(AiMessage.from("no-response"))
    }
}

val GrazieGPT4 = GrazieModel(OpenAIProfileIDs.Chat.GPT4)
val GrazieGPT4omini = GrazieModel(LLMProfileID("openai-gpt-4o-mini"))
@OptIn(ExperimentalLLM::class)
val GrazieGPT4o = GrazieModel(OpenAIProfileIDs.Chat.GPT4o)
