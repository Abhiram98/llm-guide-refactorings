package com.intellij.ml.llm.template.agents

import ai.grazie.code.agents.core.JwtTokenProvider
import ai.grazie.code.agents.core.event.EventHandler
import ai.grazie.code.agents.core.model.Temperature
import ai.grazie.code.agents.core.model.agent.AgentSystemPromptProvider
import ai.grazie.code.agents.core.model.tools.ToolDescriptorProvider
import ai.grazie.code.agents.core.tool.singleStageStatelessToolRegistry
import ai.grazie.code.agents.ideformer.IdeFormerRunner
import ai.grazie.code.agents.ideformer.model.agent.IdeFormerAgent
import ai.grazie.model.llm.profile.LLMProfileID
import ai.grazie.utils.annotations.ExperimentalAPI
import com.intellij.ml.llm.template.refactoringobjects.extractfunction.ExtractMethodFactory
import com.intellij.ml.llm.template.refactoringobjects.renamevariable.RenameVariableFactory
import com.intellij.ml.llm.template.server.RefactoringServer
import com.intellij.ml.llm.template.utils.addLineNumbersToCodeSnippet
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import javax.swing.SwingUtilities


class RefactoringAgentLauncher(val project: Project, val editor: Editor, val file: PsiFile){
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
                print("Args: $args")
                try{
                    val params = Json.decodeFromString<RefactoringServer.ExtractMethodParams>(args.toString())
                    println("extracting lines ${params.startLine} -> ${params.endLine}: ${params.newName}")

                    // Call IJ rename API here.
                    val refObjs = ExtractMethodFactory.fromStartEndLine(
                        editor,
                        file,
                        params.startLine,
                        params.endLine,
                        params.newName
                    )
                    val response = if (refObjs.isNotEmpty()) {
                        var failedException: Exception? = null
                        SwingUtilities.invokeAndWait {
                            try {
                                refObjs[0].performRefactoring(project, editor, file)
                            } catch (ex: Exception) {
                                failedException = ex
                            }
                        }
                        if (failedException == null)
                            "success"
                        else
                            throw failedException!!
                    } else
                        "couldn't create a refactoring object."
                    return@tool response
                }catch (exc: Exception) {
                    return@tool "Error. " + exc.message.toString()
                }
            }

            tool(RefactoringTools.Rename.NAME){
                args ->
                try{
                    val params = Json.decodeFromString<RefactoringServer.RenameParams>(args.toString())
                    println("renaming ${params.oldName}@${params.lineNum} -> ${params.newName}")

                    // Call IJ rename API here.
                    val renameObject = RenameVariableFactory.fromOldNewNameAll(
                        project, editor, file, params.oldName, params.newName
                    )
                    val response = if (renameObject.isNotEmpty()) {
                        val refObj = if (renameObject.size > 1) {
                            if (params.lineNum == null)
                                throw Exception(
                                    "too many matching variables/field. " +
                                            "Please choose a line number to identify the variable/field to be renamed."
                                )
                            val objs = renameObject.filter { it.startLoc + 1 == params.lineNum }
                            if (objs.size > 1) {
                                throw Exception(
                                    "too many matching variables/field. " +
                                            "Please choose a line number to identify the variable/field to be renamed."
                                )
                            } else if (objs.isEmpty()) {
                                throw Exception("No matching variable/field at the given line number.")
                            } else {
                                objs[0]
                            }
                        } else {
                            renameObject[0]
                        }
                        SwingUtilities.invokeAndWait { refObj.performRefactoring(project, editor, file) }
                        "success"
                    } else
                        "could not identify a variable to rename"
                    return@tool response
                } catch (exc: Exception){
                    return@tool "Error. " + exc.message.toString()
                }
            }

            tool(RefactoringTools.GetSource.NAME){
                args -> return@tool file.text
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
            llmConfig = IdeFormerAgent.GrazieDefault.Config.LLM(
                profile = LLMProfileID("openai-gpt-4o-mini"),
                temperature = Temperature(0.5)
            ),
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


//            val sourceCode = addLineNumbersToCodeSnippet(file?.text?:"", 1)
            val sourceCode = file.text
            /**
             * 8. Start your AI agent
             * */
            IdeFormerRunner(
                client = client,
                toolRegistry = toolRegistry,
                eventHandler = eventHandler,
                agent = RefAgent,
                agentConfig = agentConfig
            ).run(sourceCode)

        }
    }
}