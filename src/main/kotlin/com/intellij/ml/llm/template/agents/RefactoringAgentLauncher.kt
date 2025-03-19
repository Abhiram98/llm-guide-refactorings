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
import com.intellij.codeInsight.unwrap.ScopeHighlighter
import com.intellij.ml.llm.template.LLMBundle
import com.intellij.ml.llm.template.intentions.ApplySuggestRefactoringIntention
import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.refactoringobjects.extractfunction.ExtractMethodFactory
import com.intellij.ml.llm.template.refactoringobjects.renamevariable.RenameVariableFactory
import com.intellij.ml.llm.template.server.RefactoringServer
import com.intellij.ml.llm.template.telemetry.*
import com.intellij.ml.llm.template.testcuration.TestSelector
import com.intellij.ml.llm.template.ui.CompletedRefactoringsPanel
import com.intellij.ml.llm.template.utils.CodeTransformer
import com.intellij.ml.llm.template.utils.EFNotification
import com.intellij.ml.llm.template.utils.addLineNumbersToCodeSnippet
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.ui.awt.RelativePoint
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.idea.base.codeInsight.handlers.fixers.endLine
import org.jetbrains.kotlin.idea.base.codeInsight.handlers.fixers.startLine
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import java.awt.Point
import java.awt.Rectangle
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities


class RefactoringAgentLauncher(val project: Project, val editor: Editor, val file: PsiFile){
    object RefAgent: IdeFormerAgent.GrazieDefault("refactoring-agent")

    val testSelector = TestSelector.createSelector(10, project)
    val codeTransformer = CodeTransformer()
    val performedRefactorings = mutableListOf<AbstractRefactoring>()
    val telemetryDataManager = EFTelemetryDataManager()

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
                        if (failedException == null) {
                            performedRefactorings.add(refObjs[0])
                            "success"
                        }
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
                        performedRefactorings.add(refObj)
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

            tool(RefactoringTools.CurateTests.NAME){
                args ->
                runReadAction{ testSelector.collectTestSamplesForCurrentFile(file.virtualFile, project) }
                testSelector.runAndKeepPassingTests()
                return@tool testSelector.getTestNames().toString()
            }

            tool(RefactoringTools.RunTestClass.NAME){
                    args -> testSelector.runTests()
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

        telemetryDataManager.newSession()
        telemetryDataManager.addHostFunctionTelemetryData(
            EFTelemetryDataUtils.buildHostFunctionTelemetryData(
                codeSnippet = file.text,
                lineStart = file.startLine(editor.document),
                bodyLineStart = file.endLine(editor.document),
                language = file.language.id.toLowerCaseAsciiOnly(),
                filePath = file.virtualFile.path,
                hostClassPsi = null
            )
        )


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
            try{
                IdeFormerRunner(
                    client = client,
                    toolRegistry = toolRegistry,
                    eventHandler = eventHandler,
                    agent = RefAgent,
                    agentConfig = agentConfig
                ).run(sourceCode)
            } catch (e: Exception){
                print("Something failed.")
            }

        }
        // Show completed refactorings to the developer.
        invokeLater {
            showCompletedRefactoringOptionsPopup(
                project, editor, file, performedRefactorings, codeTransformer,
            )
        }
    }

    private fun showCompletedRefactoringOptionsPopup(
        project: Project,
        editor: Editor,
        file: PsiFile,
        candidates: List<AbstractRefactoring>,
        codeTransformer: CodeTransformer
    ) {
        if (candidates.isEmpty()){
            // Show notification
            return
        }
        val highlighter = AtomicReference(ScopeHighlighter(editor))
        telemetryDataManager.setRefactoringObjects(candidates)
        val efPanel = CompletedRefactoringsPanel(
            project = project,
            editor = editor,
            file = file,
            candidates = candidates,
            codeTransformer = codeTransformer,
            highlighter = highlighter,
            efTelemetryDataManager = telemetryDataManager
        )
        efPanel.initTable()
        val elapsedTimeTelemetryDataObserver = TelemetryElapsedTimeObserver()
        efPanel.addObserver(elapsedTimeTelemetryDataObserver)
        val panel = efPanel.createPanel()

        // Create the popup
        val efPopup =
            JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, efPanel.myRefactoringCandidateTable)
                .setRequestFocus(true)
                .setTitle(LLMBundle.message("ef.candidates.completed.popup.title"))
                .setResizable(true)
                .setMovable(true)
                .setCancelOnClickOutside(false)
                .createPopup()

        // Add onClosed listener
        efPopup.addListener(object : JBPopupListener {
            override fun onClosed(event: LightweightWindowEvent) {
                elapsedTimeTelemetryDataObserver.update(
                    EFNotification(
                        EFTelemetryDataElapsedTimeNotificationPayload(TelemetryDataAction.STOP, 0)
                    )
                )
                elapsedTimeTelemetryDataObserver.buildElapsedTimeTelemetryData(telemetryDataManager = telemetryDataManager)
                highlighter.getAndSet(null).dropHighlight()
//                sendTelemetryData() TODO: implement this!
            }

            override fun beforeShown(event: LightweightWindowEvent) {
                super.beforeShown(event)
                elapsedTimeTelemetryDataObserver.update(
                    EFNotification(
                        EFTelemetryDataElapsedTimeNotificationPayload(TelemetryDataAction.START, 0)
                    )
                )
            }
        })

        // set the popup as delegate to the Extract Function panel
        efPanel.setDelegatePopup(efPopup)

        // Show the popup at the top right corner of the current editor
        val contentComponent = editor.contentComponent
        val visibleRect: Rectangle = contentComponent.visibleRect
        val point = Point(visibleRect.x + visibleRect.width - 500, visibleRect.y)
        efPopup.show(RelativePoint(contentComponent, point))
    }
}