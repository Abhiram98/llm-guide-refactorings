@file:Suppress("UnstableApiUsage")

package org.boulderse.ijserver.models

import ai.grazie.model.cloud.exceptions.HTTPStatusException
import ai.grazie.model.llm.profile.LLMProfileID
import org.boulderse.ijserver.LLMBundle
import org.boulderse.ijserver.models.grazie.GrazieResponse
import org.boulderse.ijserver.models.openai.AuthorizationException
import org.boulderse.ijserver.models.openai.OpenAiChatMessage
import org.boulderse.ijserver.models.openai.OpenAiChatRequestBody
import org.boulderse.ijserver.settings.RefAgentSettingsManager
import org.boulderse.ijserver.showAuthorizationFailedNotification
import org.boulderse.ijserver.showRequestFailedNotification
import org.boulderse.ijserver.showUnauthorizedNotification
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.io.HttpRequests
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.output.Response
import java.io.IOException
import java.net.HttpURLConnection

private val logger = Logger.getInstance("#com.intellij.ml.llm.template.models")

fun sendEditRequest(
    project: Project,
    input: String,
    instruction: String,
    temperature: Double? = null,
    topP: Double? = null,
    numberOfSuggestions: Int? = null,
    llmRequestProvider: LLMRequestProvider = CodexRequestProvider,
): LLMBaseResponse? {
    val settings = RefAgentSettingsManager.getInstance()

    val request = llmRequestProvider.createEditRequest(
        input = input,
        instruction = instruction,
        temperature = temperature ?: settings.getTemperature(),
        topP = topP ?: settings.getTopP(),
        numberOfSuggestions = numberOfSuggestions ?: settings.getNumberOfSamples()
    )

    return sendRequest(project, request)
}

fun sendCompletionRequest(
    project: Project,
    input: String,
    suffix: String,
    maxTokens: Int? = null,
    temperature: Double? = null,
    presencePenalty: Double? = null,
    frequencyPenalty: Double? = null,
    topP: Double? = null,
    numberOfSuggestions: Int? = null,
    llmRequestProvider: LLMRequestProvider = CodexRequestProvider,
): LLMBaseResponse? {
    val request = llmRequestProvider.createCompletionRequest(
        input = input,
        suffix = suffix,
        maxTokens = maxTokens,
        temperature = temperature,
        topP = topP,
        numberOfSuggestions = numberOfSuggestions,
        presencePenalty = presencePenalty,
        frequencyPenalty = frequencyPenalty,
        logProbs = 1
    )

    return sendRequest(project, request)
}

fun sendChatRequest(
    project: Project,
    messages: List<ChatMessage>,
    model: String? = null,
    llmRequestProvider: LLMRequestProvider = GPTRequestProvider,
    temperature: Double = 0.5
): LLMBaseResponse? {
//    val request = llmRequestProvider.createChatGPTRequest(
//        OpenAiChatRequestBody(
//            model = model ?: llmRequestProvider.chatModel,
//            messages = messages,
//            temperature = temperature
//        )
//    )
//    return sendRequest(project, request)
    TODO("Deprecate function")
}

fun sendChatRequest(
    project: Project,
    messages: List<ChatMessage>,
    model: ChatLanguageModel
): GrazieResponse? {
    try {
        val response = model.generate(messages)
        return GrazieResponse(response.content().text(),
            if (response.finishReason()==null) "normal" else response.finishReason().toString())
    } catch (e: AuthorizationException) {
        showUnauthorizedNotification(project)
        throw e
    } catch (e: HTTPStatusException.Unauthorized){
        showUnauthorizedNotification(project)
        throw e
    }
    catch (e: HttpRequests.HttpStatusException) {
        when (e.statusCode) {
            HttpURLConnection.HTTP_UNAUTHORIZED -> showAuthorizationFailedNotification(project)
            else -> {
                showRequestFailedNotification(
                    project, LLMBundle.message("notification.request.failed.message", e.message ?: "")
                )
                logger.warn(e)
            }
        }
        throw e
    } catch (e: Exception) {
        showRequestFailedNotification(
            project, LLMBundle.message("notification.request.failed.message", e.message ?: "")
        )
        logger.warn(e)
        throw e
    }
}

fun sendChatRequest(
    messages: List<OpenAiChatMessage>,
    model: LLMProfileID,
    llmRequestProvider: LLMRequestProvider = GPTRequestProvider,
    temperature: Double = 0.5
): LLMBaseResponse? {
    val request = llmRequestProvider.createChatGPTRequest(
        OpenAiChatRequestBody(
            model = model,
            messages = messages,
            temperature = temperature
        )
    )
    return sendRequest(request)
}

private fun sendRequest(project: Project, request: LLMBaseRequest<*>): LLMBaseResponse? {
    try {
        return request.sendSync()
    } catch (e: AuthorizationException) {
        showUnauthorizedNotification(project)
    } catch (e: HttpRequests.HttpStatusException) {
        when (e.statusCode) {
            HttpURLConnection.HTTP_UNAUTHORIZED -> showAuthorizationFailedNotification(project)
            else -> {
                showRequestFailedNotification(
                    project, LLMBundle.message("notification.request.failed.message", e.message ?: "")
                )
                logger.warn(e)
            }
        }
    } catch (e: IOException) {
        showRequestFailedNotification(
            project, LLMBundle.message("notification.request.failed.message", e.message ?: "")
        )
        logger.warn(e)
    }
    return null
}


private fun sendRequest(request: LLMBaseRequest<*>): LLMBaseResponse? {
    return request.sendSync()
}