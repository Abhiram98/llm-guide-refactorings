package org.boulderse.ijserver.models.ollama

import dev.langchain4j.model.ollama.OllamaChatModel

val localOllamaLlama2 =
    OllamaChatModel
        .builder()
        .baseUrl("http://localhost:11434")
        .modelName("llama2")
        .temperature(0.8)
        .build()

val localOllamaMistral =
    OllamaChatModel
        .builder()
        .baseUrl("http://localhost:11434")
        .modelName("mistral:instruct")
        .temperature(0.8)
        .build()

fun createModelFromNameTemperature(
    modelName: String,
    temperature: Double,
    baseUrl: String,
): OllamaChatModel? =
    OllamaChatModel
        .builder()
        .baseUrl(baseUrl)
        .modelName(modelName)
        .temperature(temperature)
        .build()
