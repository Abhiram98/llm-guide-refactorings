package com.intellij.ml.llm.template.settings

import com.intellij.ml.llm.template.LLMBundle
import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel

class RefAgentConfigurable : BoundConfigurable(LLMBundle.message("settings.configurable.display.name")) {
    private val settings = service<RefAgentSettingsManager>()

    override fun createPanel(): DialogPanel {
        return panel {
            row(LLMBundle.message("settings.configurable.openai.key.label")) {
                passwordField().bindText(
                    settings::getOpenAiKey, settings::setOpenAiKey
                )
                browserLink("Sign up for API key", "https://platform.openai.com/signup")
            }
            row(LLMBundle.message("settings.configurable.openai.organization.label")) {
                passwordField().bindText(
                    settings::getOpenAiOrganization, settings::setOpenAiOrganization
                )
            }
            row(LLMBundle.message("settings.configurable.openai.model.label")) {
                comboBox(
                    listOf(
                        "grazie",
                        "openai-gpt-4",
                        "openai-gpt-3.5-turbo",
                        "openai-gpt-4-o-mini",
                        "ollama",
                        "claude-sonnet"
                    )).bindItem(
                    settings::getAiModel, settings::setAiModel
                )
            }
            row(LLMBundle.message("settings.configurable.openai.use.ollama.obj.creation")) {
                checkBox("Yes")
                    .bindSelected(settings::getUseLocalLLM, settings::setUseLocalLLM)
            }
        }
    }
}

fun openSettingsDialog(project: Project?) {
    ShowSettingsUtil.getInstance().showSettingsDialog(project, RefAgentConfigurable::class.java)
}