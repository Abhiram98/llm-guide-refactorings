package org.boulderse.ijserver.models.openai

import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.openai.OpenAiModelName.GPT_3_5_TURBO
import dev.langchain4j.model.openai.OpenAiModelName.GPT_4


fun getOpenAiModel(modelName:String, apiKey:String, temperature: Double): OpenAiChatModel? {
    return OpenAiChatModel.builder()
        .apiKey(apiKey)
        .modelName(modelName)
        .temperature(temperature)
//        .responseFormat()
        .build()
}

fun OpenAiGpt4(apiKey: String) = getOpenAiModel(GPT_4, apiKey, 0.7)
fun OpenAiGpt3_5_turbo(apiKey: String) = getOpenAiModel(GPT_3_5_TURBO, apiKey, 0.7)
