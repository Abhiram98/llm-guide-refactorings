package org.boulderse.ijserver.suggestrefactoring

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import dev.langchain4j.model.chat.ChatLanguageModel
import kotlinx.coroutines.*
import org.boulderse.ijserver.intentions.selectionPriority
import org.boulderse.ijserver.models.LLMBaseResponse
import org.boulderse.ijserver.models.LLMRequestProvider
import org.boulderse.ijserver.refactoringobjects.AbstractRefactoring
import org.boulderse.ijserver.refactoringobjects.conditionals.*
import org.boulderse.ijserver.refactoringobjects.extractfunction.ExtractMethodFactory
import org.boulderse.ijserver.refactoringobjects.looping.EnhancedForFactory
import org.boulderse.ijserver.refactoringobjects.looping.For2Stream
import org.boulderse.ijserver.refactoringobjects.looping.For2While
import org.boulderse.ijserver.refactoringobjects.movemethod.MoveMethodFactory
import org.boulderse.ijserver.refactoringobjects.renamevariable.RenameVariableFactory
import org.boulderse.ijserver.refactoringobjects.stringbuilder.StringBuilderRefactoringFactory
import kotlin.system.measureTimeMillis

class SimpleRefactoringValidatorSerial(
    private val efLLMRequestProvider: ChatLanguageModel,
    private val project: Project,
    private val editor: Editor,
    private val file: PsiFile,
    private val functionSrc: String,
    apiResponseCache: MutableMap<String, MutableMap<String, LLMBaseResponse>>,
) : AbstractRefactoringValidator(efLLMRequestProvider, project, editor, file, functionSrc, apiResponseCache) {
    private val logger = Logger.getInstance(javaClass)

    override suspend fun getRefactoringSuggestions(
        llmResponseText: String,
        limit: Int,
    ): List<AbstractRefactoring> {
        val refactoringSuggestion = getRawSuggestions(llmResponseText) ?: return emptyList()
        val allRefactoringObjects = mutableListOf<AbstractRefactoring>()

        val time =
            measureTimeMillis {
                val improvements = refactoringSuggestion.improvements.sortedBy { selectionPriority(it, this) }
                val subList =
                    if (improvements.size > limit) {
                        improvements.subList(0, limit)
                    } else {
                        improvements
                    }.sortedBy { isExtractMethod(it) }

                allRefactoringObjects.addAll(
                    buildObjectsFromImprovementsList(subList),
                )
            }

        logger.debug("Processing took $time ms")
        delay(3000)

        return allRefactoringObjects
    }

    override fun isEnhacedForRefactoring(atomicSuggestion: AtomicSuggestion): Boolean =
        atomicSuggestion.shortDescription.lowercase().contains("enhanced") &&
            atomicSuggestion.shortDescription.lowercase().contains("for")

    override fun isEnhancedSwitchRefactoring(suggestion: AtomicSuggestion): Boolean =
        suggestion.shortDescription.lowercase().contains("enhanced") &&
            suggestion.shortDescription.lowercase().contains("switch")

    override fun isFor2While(suggestion: AtomicSuggestion): Boolean {
        val lowercaseDescription = suggestion.shortDescription.lowercase()
        return lowercaseDescription.contains("for") &&
            lowercaseDescription.contains("while") &&
            lowercaseDescription.contains("convert") &&
            lowercaseDescription.indexOf("for") < lowercaseDescription.indexOf("while")
    }

    override fun isFor2Streams(suggestion: AtomicSuggestion): Boolean {
        val lowercaseDescription = suggestion.shortDescription.lowercase()
        return lowercaseDescription.contains("for") &&
            lowercaseDescription.contains("stream") &&
            lowercaseDescription.contains("convert") &&
            lowercaseDescription.indexOf("for") < lowercaseDescription.indexOf("stream")
    }

    override fun isIf2Switch(suggestion: AtomicSuggestion): Boolean {
        val lowercaseDescription = suggestion.shortDescription.lowercase()
        return lowercaseDescription.contains("if") &&
            lowercaseDescription.contains("switch") &&
            lowercaseDescription.contains("convert") &&
            lowercaseDescription.indexOf("if") < lowercaseDescription.indexOf("switch")
    }

    override fun isSwitch2If(suggestion: AtomicSuggestion): Boolean {
        val lowercaseDescription = suggestion.shortDescription.lowercase()
        return lowercaseDescription.contains("switch") &&
            lowercaseDescription.contains("if") &&
            lowercaseDescription.contains("convert") &&
            lowercaseDescription.indexOf("switch") < lowercaseDescription.indexOf("if")
    }

    override fun isIf2Ternary(suggestion: AtomicSuggestion): Boolean {
        val lowercaseDescription = suggestion.shortDescription.lowercase()
        return lowercaseDescription.contains("if") &&
            lowercaseDescription.contains("ternary") &&
            lowercaseDescription.contains("convert") &&
            lowercaseDescription.indexOf("if") < lowercaseDescription.indexOf("ternary")
    }

    override fun isTernary2If(suggestion: AtomicSuggestion): Boolean {
        val lowercaseDescription = suggestion.shortDescription.lowercase()
        return lowercaseDescription.contains("ternary") &&
            lowercaseDescription.contains("if") &&
            lowercaseDescription.contains("convert") &&
            lowercaseDescription.indexOf("ternary") < lowercaseDescription.indexOf("if")
    }

    override fun isStringBuilder(suggestion: AtomicSuggestion): Boolean =
        suggestion.shortDescription.lowercase().contains("string") &&
            suggestion.shortDescription.lowercase().contains("builder")

    override fun isMoveMethod(suggestion: AtomicSuggestion): Boolean =
        suggestion.shortDescription.lowercase().contains("move") &&
            suggestion.shortDescription.lowercase().contains("method")

    override suspend fun buildObjectsFromImprovementsList(improvementsList: List<AtomicSuggestion>): List<AbstractRefactoring> {
        val allRefactoringObjects = mutableListOf<AbstractRefactoring>()
        for (suggestion in improvementsList) {
            val refFactory =
                when {
                    isExtractMethod(suggestion) -> ExtractMethodFactory
                    isRenameVariable(suggestion) -> RenameVariableFactory
                    isEnhacedForRefactoring(suggestion) -> EnhancedForFactory.factory
                    isEnhancedSwitchRefactoring(suggestion) -> EnhancedSwitchFactory.factory
                    isFor2While(suggestion) -> For2While.factory
                    isFor2Streams(suggestion) -> For2Stream.factory
                    isIf2Switch(suggestion) -> If2Switch.factory
                    isSwitch2If(suggestion) -> Switch2IfFactory
                    isIf2Ternary(suggestion) -> If2Ternary.factory
                    isTernary2If(suggestion) -> Ternary2If.factory
                    isStringBuilder(suggestion) -> StringBuilderRefactoringFactory
                    isMoveMethod(suggestion) -> MoveMethodFactory
                    else -> ExtractMethodFactory // default
                }
            Thread.sleep(1000)
            val createdRefactoringObjects =
                getParamsAndCreateObject(suggestion, refFactory)
            Thread.sleep(2000)

            if (!createdRefactoringObjects.isNullOrEmpty()) {
                allRefactoringObjects.addAll(createdRefactoringObjects)
            }
        }
        return allRefactoringObjects
    }

    override fun isExtractMethod(atomicSuggestion: AtomicSuggestion): Boolean {
        return atomicSuggestion.shortDescription.lowercase().contains("extract")
//                && atomicSuggestion.shortDescription.lowercase().contains("method")
    }

    override fun isRenameVariable(atomicSuggestion: AtomicSuggestion): Boolean {
        return atomicSuggestion.shortDescription.lowercase().contains("rename")
//                && atomicSuggestion.shortDescription.lowercase().contains("variable")
    }
}
