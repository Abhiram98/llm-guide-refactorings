package com.intellij.ml.llm.template.agents

import ai.grazie.code.agents.core.JwtTokenProvider
import ai.grazie.code.agents.core.event.EventHandler
import ai.grazie.code.agents.core.model.agent.AgentSystemPromptProvider
import ai.grazie.code.agents.core.model.tools.ToolDescriptorProvider
import ai.grazie.code.agents.core.tool.singleStageStatelessToolRegistry
import ai.grazie.code.agents.ideformer.IdeFormerRunner
import ai.grazie.code.agents.ideformer.model.agent.IdeFormerAgent
import ai.grazie.utils.annotations.ExperimentalAPI
import com.intellij.ml.llm.template.utils.addLineNumbersToCodeSnippet
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import kotlinx.coroutines.runBlocking


class RefactoringAgentLauncher(val project: Project?, val editor: Editor?, val file: PsiFile?){
    object RefAgent: IdeFormerAgent.GrazieDefault("refactoring-agent")
    fun do_extract(){

    }

    @OptIn(ExperimentalAPI::class)
    fun launch() {


        /** 1. Describe the list of tools for your agent
         *
         * IMPORTANT: Defining tool descriptors in code is good ONLY for fast experiments,
         * but for production purposes this method is not recommended.
         * Please, contribute to [ai.grazie.code.agents.tools.registry.GlobalAgentToolsRegistry.Tools] here:
         * https://github.com/JetBrains/code-engine/tree/main/code-agents/code-agents-tools-registry
         * and then feel free to use [ToolDescriptorProvider.fromRegistry] method.
         */
        val toolDescriptorProvider = ToolDescriptorProvider.static(RefactoringTools.toolsList)

        // 2. Implement the tools using ToolRegistry
        val toolRegistry = singleStageStatelessToolRegistry(toolDescriptorProvider) {
            tool(RefactoringTools.ExtractMethod.NAME) { args ->
                print("performing extract method.")
                do_extract()
                "success"
            }
        }

        /** 3. Provide agent configuration (including system prompt)
         *
         * IMPORTANT: Defining prompts in code as strins is good ONLY for fast experiments,
         * but for production purposes this method is not recommended.
         * Please, contribute to [ai.grazie.code.agents.tools.registry.GlobalAgentToolsRegistry.Prompts] here:
         * https://github.com/JetBrains/code-engine/tree/main/code-agents/code-agents-tools-registry
         * and then feel free to use [AgentSystemPromptProvider.fromRegistry] method.
         */
        val agentConfig = IdeFormerAgent.GrazieDefault.Config(
            llmConfig = IdeFormerAgent.GrazieDefault.Config.LLM(),
//        systemPrompt = AgentSystemPromptProvider.fromString(
//            """
//            You are an question answering agent with access to the calculator tools.
//            You need to answer 1 question with the best of your ability.
//            Be as concise as possible in your answers, and only return the number in your final answer.
//            Do not apply any locale-specific formatting to the result.
//            DO NOT ANSWER ANY QUESTIONS THAT ARE BESIDES PERFORMING CALCULATIONS!
//            DO NOT HALLUCINATE!
//            """.trimIndent()
//        ),
        )

        /**
         * 4. Provide a grazie token (for connecting with LLMs):
         *  - For testing purposes with `production` tokens please go to https://platform.jetbrains.ai/, and copy your development token from there
         *  - For testing purposes with `staging` tokens please go to https://platform.stgn.jetbrains.ai/, and copy your development token from there
         *  - In real applications, please first authorize user using JBA, and then use Grazie API to exchange JBA token to Grazie user token!
         * */
        val tokenProvider = object : JwtTokenProvider {
            override fun getToken(): String = IdeFormerService.token
            override fun subscribe(onTokenChanged: (token: String?) -> Unit) {}
            override fun invalidate() {}
        }

        /**
         * 5. Define how you would like to handle events in your agents
         * */
        val eventHandler = EventHandler {
            onResultReceived { result ->
                if (result != null) {
                    println("result: $result")
                }
            }
            onToolCalled { toolName, arguments, _ ->
                println("tool $toolName was called with arguments $arguments")
            }
            onException<Throwable> { exception ->
                println("error happened: ${exception.message}")
            }
        }


        runBlocking {
            /**
             * 7. Obtain a connection to IdeFormer process
             * */
            val client = IdeFormerService.getAgentClient(tokenProvider)


            val sourceCode = addLineNumbersToCodeSnippet(file?.text?:"", 1)
            /**
             * 8. Start your AI agent
             * */
            IdeFormerRunner(
                client = client,
                toolRegistry = toolRegistry,
                eventHandler = eventHandler,
                agent = RefAgent,
                agentConfig = agentConfig
            ).run("Refactoring this code. + $sourceCode")

        }
    }
}

fun main(){
    RefactoringAgentLauncher(null, null, null).launch()
}