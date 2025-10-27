package org.boulderse.ijserver.refactoringobjects

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

interface MyRefactoringFactory {
//    fun getParamsAndCreate(rawSuggestion: AtomicSuggestion, efLLMRequestProvider: LLMRequestProvider): AbstractRefactoring

    // Return refactoring objects from a function call.
    // May return more than 1 object after enhancing data ensure a well-formed object
    // Ex: extract method enhancements to ensure line numbers are a valid block.
    // extract_method(10, 20, "newMethodName")
    // rename_variable("x", "count")
    fun createObjectsFromFuncCall(
        funcCall: String,
        project: Project,
        editor: Editor,
        file: PsiFile,
    ): List<AbstractRefactoring>

    // Logical name for the refactoring. Different from apiFunctionName
    // because this variable can contain spaces
    val logicalName: String

    /*
    Function name passed to the LLM, while asking it to create params.
    Different from logicalName because it must be a valid identifier.
     */
    val apiFunctionName: String

    // API Documentation for passed to the LLM, while asking it to create params.
    val APIDocumentation: String

    fun getParamsFromFuncCall(funcCall: String): List<String> {
        val removedWhite = funcCall.replace(" ", "")
        val parenthesisRegex = Regex("\\(.*\\)")
        val matchResult = parenthesisRegex.find(removedWhite)?.value
        if (matchResult != null) {
            val withoutParen = matchResult.slice(IntRange(1, matchResult.length - 2))
            val params = withoutParen.split(",")
            val processedParams: MutableList<String> = mutableListOf()
            for (p in params) {
                processedParams.add(processParam(p))
            }
            return processedParams
        }
        return listOf()
    }

    fun processParam(p: String): String {
        if (p.contains("=")) {
            return p.split("=")[1]
        }
        return p
    }

    fun getStringFromParam(param: String) =
        param
            .replace("\"", "")
            .replace("\'", "")
}
