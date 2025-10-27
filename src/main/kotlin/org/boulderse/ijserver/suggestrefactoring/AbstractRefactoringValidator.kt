package org.boulderse.ijserver.suggestrefactoring

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.google.gson.stream.MalformedJsonException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.model.chat.ChatLanguageModel
import org.boulderse.ijserver.models.LLMBaseResponse
import org.boulderse.ijserver.models.sendChatRequest
import org.boulderse.ijserver.prompts.GetRefactoringObjParametersPrompt
import org.boulderse.ijserver.prompts.SuggestRefactoringPrompt
import org.boulderse.ijserver.refactoringobjects.AbstractRefactoring
import org.boulderse.ijserver.refactoringobjects.MyRefactoringFactory
import kotlin.math.max
import kotlin.math.min

abstract class AbstractRefactoringValidator(
    private val llmChatModel: ChatLanguageModel,
    private val project: Project,
    private val editor: Editor,
    private val file: PsiFile,
    private val functionSrc: String,
    private var apiResponseCache: MutableMap<String, MutableMap<String, LLMBaseResponse>>,
) {
    private val logger = Logger.getInstance(javaClass)

    abstract fun isExtractMethod(atomicSuggestion: AtomicSuggestion): Boolean

    fun getParamsAndCreateObject(
        atomicSuggestion: AtomicSuggestion,
        refactoringFactory: MyRefactoringFactory,
    ): List<AbstractRefactoring>? {
        val messageList: MutableList<ChatMessage> =
            setupOpenAiChatMessages(atomicSuggestion, refactoringFactory)

        val response =
            apiResponseCache[functionSrc]?.get(atomicSuggestion.getSerialized())
                ?: sendChatRequest(
                    project,
                    messageList,
                    llmChatModel,
                )
        if (response != null) {
            val funcCallListString: String = response.getSuggestions()[0].text
            val refObjects = mutableListOf<AbstractRefactoring>()
            val jsonArray =
                try {
                    JsonParser.parseString(sanitiseJsonString(funcCallListString)) as JsonArray
                } catch (e: JsonSyntaxException) {
                    return null // failed to decode json
                }
            cacheResponse(atomicSuggestion, response)
            for (funcCall in jsonArray) {
                val r = getRefactoringObject(funcCall.asString, refactoringFactory, atomicSuggestion)
                if (r != null) {
                    refObjects.addAll(r)
                }
            }
            if (refObjects.size > 0) return refObjects
        }
        return null
    }

    private fun sanitiseJsonString(funcCallListString: String) = funcCallListString.replace("\\_", "_")

    private fun getRefactoringObject(
        funcCall: String,
        refactoringFactory: MyRefactoringFactory,
        atomicSuggestion: AtomicSuggestion,
    ): List<AbstractRefactoring>? {
        val extractedFuncCall =
            if (funcCall.startsWith(refactoringFactory.apiFunctionName)) {
                funcCall
            } else {
                tryFindFuncCall(funcCall, refactoringFactory.apiFunctionName)
            }
//            logger.debug(funcCall)
        if (extractedFuncCall.startsWith(refactoringFactory.apiFunctionName)) {
//                logger.debug("Looks like a ${refactoringFactory.apiFunctionName} call!")
            logger.info("* Creating IntelliJ Refactoring Object: $extractedFuncCall")
            val createdObjectsFromFuncCall =
                try {
                    refactoringFactory.createObjectsFromFuncCall(
                        funcCall,
                        project,
                        editor,
                        file,
                    )
                } catch (e: Exception) {
                    logger.info("Failed to create refactoring object: ${e.message}")
                    return null
                }
            if (createdObjectsFromFuncCall.isNotEmpty()) {
                logger.info(
                    "Status: Successfully created ${createdObjectsFromFuncCall.size} refactoring object(s).".prependIndent(
                        "    ",
                    ),
                )
            } else {
                logger.info("Status: No refactoring objects were created.".prependIndent("    "))
            }
            createdObjectsFromFuncCall.forEach { it.description = atomicSuggestion.longDescription }
            return createdObjectsFromFuncCall
        }
        return null
    }

    private fun tryFindFuncCall(
        funcCall: String,
        apiFunctionName: String,
    ): String {
        // sanitise string
        val sanitisedFuncCall = funcCall.replace("\\", "")
        if (sanitisedFuncCall.contains(apiFunctionName)) {
            return "$apiFunctionName${sanitisedFuncCall.split(apiFunctionName)[1]}"
        }
        return ""
    }

    private fun cacheResponse(
        atomicSuggestion: AtomicSuggestion,
        response: LLMBaseResponse?,
    ) {
        if (response == null) {
            return
        }

        if (apiResponseCache[functionSrc] == null) {
            apiResponseCache[functionSrc] = mutableMapOf()
        }
        apiResponseCache.get(functionSrc)!!.get(atomicSuggestion.getSerialized())
            ?: apiResponseCache[functionSrc]!!.put(atomicSuggestion.getSerialized(), response)
    }

    private fun setupOpenAiChatMessages(
        atomicSuggestion: AtomicSuggestion,
        refactoringFactory: MyRefactoringFactory,
    ): MutableList<ChatMessage> {
        var messageList: MutableList<ChatMessage> = mutableListOf()
        val paddingLines = 3
        val firstLine = functionSrc.split('.')[0].toInt()
        val fromIndex = max(atomicSuggestion.start - firstLine - paddingLines, 0)
        val toIndex = min(atomicSuggestion.end - firstLine + paddingLines, functionSrc.lines().size)
        val changedCode =
            functionSrc
                .lines()
                .subList(fromIndex, toIndex)
                .joinToString("\n")
        val basePrompt = SuggestRefactoringPrompt().getPromptWithoutExample(changedCode)
        messageList.addAll(basePrompt)
        messageList.add(
            AiMessage.from(
                Gson().toJson(RefactoringSuggestion(mutableListOf(atomicSuggestion))).toString(),
            ),
        )

        messageList.addAll(
            GetRefactoringObjParametersPrompt.get(
                atomicSuggestion.shortDescription +
                    "Line: ${atomicSuggestion.start} to ${atomicSuggestion.end}",
                refactoringFactory.logicalName,
                refactoringFactory.APIDocumentation,
            ),
        )
        return messageList
    }

    companion object {
        fun getRawSuggestions(llmText: String): RefactoringSuggestion? {
            var refactoringSuggestion: RefactoringSuggestion
            try {
                refactoringSuggestion = Gson().fromJson(llmText, RefactoringSuggestion::class.java)
            } catch (e: Exception) {
                try {
                    refactoringSuggestion = Gson().fromJson("{$llmText}", RefactoringSuggestion::class.java)
                } catch (e: Exception) {
                    return null
                }
            }
            return refactoringSuggestion
        }
    }

    abstract fun isRenameVariable(atomicSuggestion: AtomicSuggestion): Boolean

    // Return a list of refactoring objects from an llm suggestion.
    abstract suspend fun getRefactoringSuggestions(
        llmResponseText: String,
        limit: Int,
    ): List<AbstractRefactoring>

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

    abstract suspend fun buildObjectsFromImprovementsList(improvementsList: List<AtomicSuggestion>): List<AbstractRefactoring>
}
