package org.boulderse.ijserver.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import org.boulderse.ijserver.LLMBundle

class RefAgentConfigurable : BoundConfigurable(LLMBundle.message("settings.configurable.display.name")) {
    private val settings = service<RefAgentSettingsManager>()

    override fun createPanel(): DialogPanel =
        panel {
            row(LLMBundle.message("settings.configurable.openai.key.label")) {
                passwordField().bindText(
                    settings::getOpenAiKey,
                    settings::setOpenAiKey,
                )
                browserLink("Sign up for API key", "https://platform.openai.com/signup")
            }
            row(LLMBundle.message("settings.configurable.openai.model.label")) {
                comboBox(
                    listOf(
                        "grazie-gpt-4o",
                        "grazie-gpt-4o-mini",
                        "grazie-gpt-4",
                        "openai-gpt-4",
                        "openai-gpt-3.5-turbo",
                        "openai-gpt-4o-mini",
                        "ollama",
                    ),
                ).bindItem(
                    settings::getAiModel,
                    settings::setAiModel,
                )
            }
            row(LLMBundle.message("settings.configurable.openai.use.ollama.obj.creation")) {
                checkBox("Yes").bindSelected(settings::getUseLocalLLM, settings::setUseLocalLLM)
            }
            row(LLMBundle.message("settings.configurable.openai.anonymize.telemetry")) {
                checkBox("Yes").bindSelected(settings::getAnonymizeTelemetry, settings::setAnonymizeTelemetry)
            }
        }
}

fun openSettingsDialog(project: Project?) {
    ShowSettingsUtil.getInstance().showSettingsDialog(project, RefAgentConfigurable::class.java)
}
