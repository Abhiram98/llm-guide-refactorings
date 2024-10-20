package com.intellij.ml.llm.template.settings

import com.intellij.ml.llm.template.models.grazie.GrazieGPT4
import com.intellij.ml.llm.template.models.grazie.GrazieGPT4o
import com.intellij.ml.llm.template.models.grazie.GrazieGPT4omini
import com.intellij.ml.llm.template.models.ollama.localOllamaMistral
import com.intellij.ml.llm.template.models.openai.CredentialsHolder
import com.intellij.ml.llm.template.models.openai.getOpenAiModel
import com.intellij.openapi.components.*
import com.intellij.util.xmlb.annotations.OptionTag
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.openai.OpenAiChatModelName
import dev.langchain4j.model.openai.OpenAiModelName.GPT_3_5_TURBO
import dev.langchain4j.model.openai.OpenAiModelName.GPT_4

@Service(Service.Level.APP)
@State(
    name = "RefAgentSettings",
    storages = [Storage(value = "llm.for.code.xml", roamingType = RoamingType.DISABLED, exportable = true)]
)
class RefAgentSettingsManager : PersistentStateComponent<RefAgentSettings> {

    companion object {

        fun getInstance() = service<RefAgentSettingsManager>()
    }

    private var state = RefAgentSettings()

    override fun getState(): RefAgentSettings = state

    override fun loadState(newState: RefAgentSettings) {
        state = newState
    }

    fun getOpenAiKey(): String {
        return CredentialsHolder.getInstance().getOpenAiApiKey() ?: ""
    }

    fun setOpenAiKey(key: String) {
        CredentialsHolder.getInstance().setOpenAiApiKey(key)
    }

    fun getAiModel() = state.aiModel

    fun setAiModel(aiModel: String?){
        if (aiModel!=null)
            state.aiModel = aiModel
    }

    fun getOpenAiOrganization(): String {
        return CredentialsHolder.getInstance().getOpenAiOrganization() ?: ""
    }

    fun setOpenAiOrganization(key: String) {
        CredentialsHolder.getInstance().setOpenAiOrganization(key)
    }

    fun useOpenAiCompletion() = state.useOpenAi

    fun getTemperature(): Double = state.llmSettings.temperature.toDouble()

    fun setTemperature(temperature: Double) {
        state.llmSettings.temperature = temperature.toFloat()
    }

    fun getPresencePenalty(): Double = state.llmSettings.presencePenalty.toDouble()

    fun setPresencePenalty(penalty: Double) {
        state.llmSettings.presencePenalty = penalty.toFloat()
    }

    fun getFrequencyPenalty(): Double = state.llmSettings.frequencyPenalty.toDouble()

    fun setFrequencyPenalty(penalty: Double) {
        state.llmSettings.frequencyPenalty = penalty.toFloat()
    }

    fun getTopP(): Double = state.llmSettings.topP.toDouble()

    fun setTopP(topP: Double) {
        state.llmSettings.topP = topP.toFloat()
    }

    fun getUseLocalLLM(): Boolean{
        return state.useOllamaToCreateObj
    }

    fun getAnonymizeTelemetry(): Boolean{
        return state.anonymize_telemetry
    }

    fun setAnonymizeTelemetry(v: Boolean){
        state.anonymize_telemetry = v
    }

    fun setUseLocalLLM(b: Boolean){
        state.useOllamaToCreateObj = b
    }
    fun getNumberOfSamples(): Int = state.llmSettings.numberOfSamples

    fun getMaxTokens(): Int = state.llmSettings.maxTokens

    fun getPromptLength(): Int = state.llmSettings.promptLength

    fun getSuffixLength(): Int = state.llmSettings.suffixLength

    fun getNumberOfIterations(): Int = state.llmSettings.numberOfIterations

    fun createAndGetAiModel(): ChatLanguageModel? {
        when (state.aiModel) {
            "grazie-gpt-4" -> {
                return GrazieGPT4
            }
            "grazie-gpt-4o" -> {
                return GrazieGPT4o
            }
            "grazie-gpt-4o-mini" -> {
                return GrazieGPT4omini
            }
            "openai-gpt-4" -> {
                return getOpenAiModel(
                    GPT_4, getOpenAiKey(), state.llmSettings.temperature.toDouble())
            }
            "openai-gpt-3.5-turbo" -> {
                return getOpenAiModel(
                    GPT_3_5_TURBO, getOpenAiKey(), state.llmSettings.temperature.toDouble())
            }
            "openai-gpt-4o-mini" -> {
                return getOpenAiModel(
                    OpenAiChatModelName.GPT_4_O_MINI.toString(), getOpenAiKey(), state.llmSettings.temperature.toDouble())
            }
            "ollama" -> {
                return localOllamaMistral
            }
        }
        return null
    }

}

class RefAgentSettings : BaseState() {
    @get:OptionTag("data_sharing")
    var isDataSharingEnabled by property(false)

    @get:OptionTag("data_sharing_initialized")
    var isDataSharingOptionInitialized by property(false)

    fun setDataSharingOption(newValue: Boolean) {
        isDataSharingOptionInitialized = true
        isDataSharingEnabled = newValue
    }

    @get:OptionTag("use_open_ai")
    var useOpenAi by property(true)

    @get:OptionTag("ai_model")
    var aiModel = "openai-gpt-4o-mini"

    @get:OptionTag("use_local_llm")
    var useOllamaToCreateObj = false

    @get:OptionTag("open_ai")
    var llmSettings by property(LLMSettings()) { it == LLMSettings() }

    @get:OptionTag("anonymize_telemetry")
    var anonymize_telemetry by property(true)
}

class LLMSettings : BaseState() {
    @get:OptionTag("temperature")
    var temperature by property(0.0f)

    @get:OptionTag("presence_penalty")
    var presencePenalty by property(0.0f)

    @get:OptionTag("frequency_penalty")
    var frequencyPenalty by property(0.0f)

    @get:OptionTag("top_p")
    var topP by property(1.0f)

    @get:OptionTag("number_of_samples")
    var numberOfSamples by property(1)

    @get:OptionTag("number_of_iterations")
    var numberOfIterations by property(3)

    @get:OptionTag("max_tokens")
    var maxTokens by property(64)

    @get:OptionTag("prompt_length")
    var promptLength by property(256)

    @get:OptionTag("suffix_length")
    var suffixLength by property(256)
}
