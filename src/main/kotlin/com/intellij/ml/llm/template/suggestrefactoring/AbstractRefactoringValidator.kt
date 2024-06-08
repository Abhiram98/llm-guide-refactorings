package com.intellij.ml.llm.template.suggestrefactoring

import com.google.gson.Gson
import com.intellij.ml.llm.template.models.LLMBaseResponse
import com.intellij.ml.llm.template.models.LLMRequestProvider
import com.intellij.ml.llm.template.models.openai.OpenAiChatMessage
import com.intellij.ml.llm.template.models.sendChatRequest
import com.intellij.ml.llm.template.prompts.GetRefactoringObjParametersPrompt
import com.intellij.ml.llm.template.prompts.SuggestRefactoringPrompt
import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.refactoringobjects.MyRefactoringFactory
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

abstract class AbstractRefactoringValidator(
    private val efLLMRequestProvider: LLMRequestProvider,
    private val project: Project,
    private val editor: Editor,
    private val file: PsiFile,
    private val functionSrc: String,
    private var apiResponseCache: MutableMap<String, MutableMap<String, LLMBaseResponse>>
) {
    private val logger = Logger.getInstance(javaClass)
    abstract fun isExtractMethod(atomicSuggestion: AtomicSuggestion): Boolean;

    fun getParamsAndCreateObject(
        atomicSuggestion: AtomicSuggestion,
        refactoringFactory: MyRefactoringFactory
    ): List<AbstractRefactoring>? {
        val messageList: MutableList<OpenAiChatMessage> =
            setupOpenAiChatMessages(atomicSuggestion, refactoringFactory)

        val response =
            apiResponseCache[functionSrc]?.get(atomicSuggestion.getSerialized())
                ?:sendChatRequest(
                    project, messageList, efLLMRequestProvider.chatModel, efLLMRequestProvider
                    )
        if (response != null) {
            cacheResponse(atomicSuggestion, response)

            val funcCall: String = response.getSuggestions()[0].text
//            logger.debug(funcCall)
            if (funcCall.startsWith(refactoringFactory.apiFunctionName)) {
//                logger.debug("Looks like a ${refactoringFactory.apiFunctionName} call!")
                logger.info("Attempting to form refactoring object: $funcCall")
                val createdObjectsFromFuncCall = refactoringFactory.createObjectsFromFuncCall(
                    funcCall,
                    project,
                    editor,
                    file
                )
                createdObjectsFromFuncCall.forEach { it.description = atomicSuggestion.longDescription }
                return createdObjectsFromFuncCall
            }
        }
        return null
    }

    private fun cacheResponse(
        atomicSuggestion: AtomicSuggestion,
        response: LLMBaseResponse?
    ) {
        if (response==null)
            return

        if (apiResponseCache[functionSrc] == null) {
            apiResponseCache[functionSrc] = mutableMapOf()
        }
        apiResponseCache.get(functionSrc)!!.get(atomicSuggestion.getSerialized())
            ?: apiResponseCache[functionSrc]!!.put(atomicSuggestion.getSerialized(), response)
    }

    private fun setupOpenAiChatMessages(
        atomicSuggestion: AtomicSuggestion,
        refactoringFactory: MyRefactoringFactory
    ): MutableList<OpenAiChatMessage> {
        var messageList: MutableList<OpenAiChatMessage> = mutableListOf()
        val basePrompt = SuggestRefactoringPrompt().getPrompt(functionSrc)
        messageList.addAll(basePrompt)
        messageList.add(
            OpenAiChatMessage(
                "assistant",
                Gson().toJson(RefactoringSuggestion(mutableListOf(atomicSuggestion))).toString()
            )
        )

        messageList.addAll(
            GetRefactoringObjParametersPrompt.get(
                atomicSuggestion.shortDescription +
                        "Line: ${atomicSuggestion.start} to ${atomicSuggestion.end}",
                refactoringFactory.logicalName,
                refactoringFactory.APIDocumentation)
        )
        return messageList
    }





    companion object{
        fun getRawSuggestions(llmText: String): RefactoringSuggestion{
            var refactoringSuggestion: RefactoringSuggestion;
            try {
                refactoringSuggestion= Gson().fromJson(llmText, RefactoringSuggestion::class.java)
            } catch (e: Exception){
                refactoringSuggestion = Gson().fromJson("{$llmText}", RefactoringSuggestion::class.java)
            }
            return refactoringSuggestion
        }
    }

    abstract fun isRenameVariable(atomicSuggestion: AtomicSuggestion): Boolean

    // Return a list of refactoring objects from an llm suggestion.
    abstract suspend fun getRefactoringSuggestions(llmResponseText: String): List<AbstractRefactoring>

    abstract fun isEnhacedForRefactoring(atomicSuggestion: AtomicSuggestion): Boolean
    abstract fun isEnhancedSwitchRefactoring(suggestion: AtomicSuggestion): Boolean
    abstract fun isFor2While(suggestion: AtomicSuggestion): Boolean
    abstract fun isFor2Streams(suggestion: AtomicSuggestion): Boolean
    abstract fun isIf2Switch(suggestion: AtomicSuggestion): Boolean
    abstract fun isSwitch2If(suggestion: AtomicSuggestion): Boolean
    abstract fun isIf2Ternary(suggestion: AtomicSuggestion): Boolean
    abstract fun isTernary2If(suggestion: AtomicSuggestion): Boolean
    abstract fun isStringBuilder(suggestion: AtomicSuggestion): Boolean
    abstract fun isMoveMethod(suggestion: AtomicSuggestion): Boolean
}