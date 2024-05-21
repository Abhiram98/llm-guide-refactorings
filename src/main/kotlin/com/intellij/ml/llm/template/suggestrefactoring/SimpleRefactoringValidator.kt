package com.intellij.ml.llm.template.suggestrefactoring

import com.intellij.ml.llm.template.models.LLMBaseResponse
import com.intellij.ml.llm.template.models.LLMRequestProvider
import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.refactoringobjects.MyRefactoringFactory
import com.intellij.ml.llm.template.refactoringobjects.conditionals.*
import com.intellij.ml.llm.template.refactoringobjects.looping.EnhancedForFactory
import com.intellij.ml.llm.template.refactoringobjects.renamevariable.RenameVariableFactory
import com.intellij.ml.llm.template.refactoringobjects.extractfunction.ExtractMethodFactory
import com.intellij.ml.llm.template.refactoringobjects.looping.For2Stream
import com.intellij.ml.llm.template.refactoringobjects.looping.For2While
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

class SimpleRefactoringValidator(
    private val efLLMRequestProvider: LLMRequestProvider,
    private val project: Project,
    private val editor: Editor,
    private val file: PsiFile,
    private val functionSrc: String,
    apiResponseCache: MutableMap<String, MutableMap<String, LLMBaseResponse>>
) : AbstractRefactoringValidator(efLLMRequestProvider, project, editor, file, functionSrc, apiResponseCache) {


    override suspend fun getRefactoringSuggestions(llmResponseText: String): List<AbstractRefactoring> {
        val refactoringSuggestion = getRawSuggestions(llmResponseText)
        val allRefactoringObjects= mutableListOf<AbstractRefactoring>()


        val time = measureTimeMillis {
            coroutineScope {
                refactoringSuggestion.improvements.map { suggestion ->
                    async(Dispatchers.Default) {
                        val refFactory = when {
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
                            else -> ExtractMethodFactory // default
                        }

                        val createdRefactoringObjects =
                            getParamsAndCreateObject(suggestion, refFactory)

                        if (!createdRefactoringObjects.isNullOrEmpty()) {
                            println("Successfully created ${createdRefactoringObjects.size} refactoring object(s).")
                            createdRefactoringObjects
                        } else {
                            println("No refactoring objects were created.")
                            emptyList()
                        }
                    }
                }.awaitAll().flatten().let {
                    allRefactoringObjects.addAll(it)
                }
            }
        }

        println("Processing took $time ms")

        return allRefactoringObjects;

    }

    override fun isEnhacedForRefactoring(atomicSuggestion: AtomicSuggestion): Boolean {
        return atomicSuggestion.shortDescription.lowercase().contains("enhanced")
                && atomicSuggestion.shortDescription.lowercase().contains("for")
    }

    override fun isEnhancedSwitchRefactoring(suggestion: AtomicSuggestion): Boolean {
        return suggestion.shortDescription.lowercase().contains("enhanced")
                && suggestion.shortDescription.lowercase().contains("switch")
    }

    override fun isFor2While(suggestion: AtomicSuggestion): Boolean {
        val lowercaseDescription = suggestion.shortDescription.lowercase()
        return lowercaseDescription.contains("for")
                && lowercaseDescription.contains("while")
                && lowercaseDescription.contains("convert")
                && lowercaseDescription.indexOf("for") < lowercaseDescription.indexOf("while")
    }

    override fun isFor2Streams(suggestion: AtomicSuggestion): Boolean {
        val lowercaseDescription = suggestion.shortDescription.lowercase()
        return lowercaseDescription.contains("for")
                && lowercaseDescription.contains("stream")
                && lowercaseDescription.contains("convert")
                && lowercaseDescription.indexOf("for") < lowercaseDescription.indexOf("stream")
    }

    override fun isIf2Switch(suggestion: AtomicSuggestion): Boolean {
        val lowercaseDescription = suggestion.shortDescription.lowercase()
        return lowercaseDescription.contains("if")
                && lowercaseDescription.contains("switch")
                && lowercaseDescription.contains("convert")
                && lowercaseDescription.indexOf("if") < lowercaseDescription.indexOf("switch")
    }

    override fun isSwitch2If(suggestion: AtomicSuggestion): Boolean {
        val lowercaseDescription = suggestion.shortDescription.lowercase()
        return lowercaseDescription.contains("switch")
                && lowercaseDescription.contains("if")
                && lowercaseDescription.contains("convert")
                && lowercaseDescription.indexOf("switch") < lowercaseDescription.indexOf("if")
    }

    override fun isIf2Ternary(suggestion: AtomicSuggestion): Boolean {
        val lowercaseDescription = suggestion.shortDescription.lowercase()
        return lowercaseDescription.contains("if")
                && lowercaseDescription.contains("ternary")
                && lowercaseDescription.contains("convert")
                && lowercaseDescription.indexOf("if") < lowercaseDescription.indexOf("ternary")
    }

    override fun isTernary2If(suggestion: AtomicSuggestion): Boolean {
        val lowercaseDescription = suggestion.shortDescription.lowercase()
        return lowercaseDescription.contains("ternary")
                && lowercaseDescription.contains("if")
                && lowercaseDescription.contains("convert")
                && lowercaseDescription.indexOf("ternary") < lowercaseDescription.indexOf("if")
    }


    override fun isExtractMethod(atomicSuggestion: AtomicSuggestion): Boolean{
        return atomicSuggestion.shortDescription.lowercase().contains("extract")
//                && atomicSuggestion.shortDescription.lowercase().contains("method")
    }


    override fun isRenameVariable(atomicSuggestion: AtomicSuggestion): Boolean{
        return atomicSuggestion.shortDescription.lowercase().contains("rename")
//                && atomicSuggestion.shortDescription.lowercase().contains("variable")
    }

}