package com.intellij.ml.llm.template.models.grazie

import ai.grazie.api.gateway.client.SuspendableAPIGatewayClient
import ai.grazie.client.common.SuspendableHTTPClient
import ai.grazie.client.ktor.GrazieKtorHTTPClient
import ai.grazie.client.ktor.GrazieKtorHTTPClient.Client
import ai.grazie.model.auth.GrazieAgent
import ai.grazie.model.auth.v5.AuthData
import ai.grazie.model.cloud.AuthType
import ai.grazie.model.llm.annotation.ExperimentalLLM
import ai.grazie.model.llm.chat.v5.*
import ai.grazie.model.llm.parameters.OpenAILLMParameters
import ai.grazie.model.llm.profile.LLMProfileID
import ai.grazie.model.llm.profile.OpenAIProfileIDs
import ai.grazie.model.llm.prompt.LLMPromptID
import ai.grazie.utils.attributes.Attributes
import com.intellij.ml.llm.template.models.LLMBaseRequest
import com.intellij.ml.llm.template.models.LLMBaseResponse
import com.intellij.ml.llm.template.models.openai.OpenAiChatRequestBody
import com.intellij.ml.llm.template.settings.RefAgentSettingsManager
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.runBlocking

class GrazieBaseRequest(body: OpenAiChatRequestBody) : LLMBaseRequest<OpenAiChatRequestBody>(body)  {

    private val logger = Logger.getInstance(javaClass)
    private val url = "https://api.app.stgn.grazie.aws.intellij.net"
    private val grazieToken = RefAgentSettingsManager.getInstance().getOpenAiKey()
    private val authData = AuthData(
        token = grazieToken,
        originalUserToken = null,
        originalServiceToken = null,
        originalApplicationToken = grazieToken,
        grazieAgent = GrazieAgent("suggest-refactoring-research", "0.1"),
    )
    private val client = SuspendableAPIGatewayClient(
        serverUrl = url,
        authType = AuthType.Application,
        httpClient = SuspendableHTTPClient.WithV5(Client.Default, authData)
    )

    private fun getChatMessages(): LLMChat {

        val llmChatArray: MutableList<LLMChatMessage> = mutableListOf()
        for(message in body.messages){
            val chatMessage= when(message.role){
                "user"-> LLMChatUserMessage(message.content)
                "assistant"-> LLMChatAssistantMessage(message.content)
                "system" -> LLMChatSystemMessage(message.content)
                else -> {throw Exception("Unknown role")}
            }

            llmChatArray += chatMessage
        }
        return LLMChat(llmChatArray.toTypedArray())
    }

    private fun getOpenAIProfileId(): LLMProfileID{
        return body.model
    }

    private fun getAttributes(): Attributes{
        return Attributes(
            mutableMapOf
                (
                Pair(
                    OpenAILLMParameters.Chat.Temperature,
                    Attributes.Value.Double(body.temperature?:0.5)
                )
            )
        )
    }

    override fun sendSync(): LLMBaseResponse? {

        val response = runBlocking {
            try {
                val response = client.llm().V5().chat(
                    LLMPromptID("suggest-refactoring-research"),
                    getOpenAIProfileId(),
                    getChatMessages(),
                )
                logger.debug("Grazie request ID: ${response}")
                var finalString: String=""
//                response.collect()
                response.collect{
                    finalString+=it.content
                    return@collect
                }
                logger.debug("Response:\n$finalString")
                return@runBlocking GrazieResponse(finalString, "completed")
            } catch (e: Exception){
                logger.debug("Couldn't make request.")
//                return@runBlocking GrazieResponse("", "failed")
                throw e
            }
        }

        return response

    }
}