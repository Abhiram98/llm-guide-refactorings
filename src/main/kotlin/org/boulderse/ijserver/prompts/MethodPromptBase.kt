package org.boulderse.ijserver.prompts

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import org.boulderse.ijserver.models.openai.OpenAiChatMessage

abstract class MethodPromptBase {
    abstract fun getPrompt(methodCode: String): MutableList<ChatMessage>
}
