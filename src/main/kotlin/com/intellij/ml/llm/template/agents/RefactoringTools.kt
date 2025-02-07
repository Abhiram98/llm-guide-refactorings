package com.intellij.ml.llm.template.agents


import ai.grazie.code.agents.core.tools.model.ToolDescriptor
import ai.grazie.code.agents.core.tools.model.ToolParameterDescriptor
import ai.grazie.code.agents.core.tools.model.ToolParameterType

object RefactoringTools {

    object ExtractMethod {
        const val NAME = "extract_method"

        object Params {
            const val startLine = "start_line"
            const val endLine = "end_line"
            const val newName = "new_method_name"
        }
    }

    internal val toolsList = listOf(
        ToolDescriptor(
            name = ExtractMethod.NAME,
            description = "Extracts a method from the specified range of lines in a source code file and creates a new function with the given name. " +
                    "This function is intended to refactor a block of code within a file, taking the lines from `line_start` to `line_end`," +
                    " inclusive, and moving them into a new function named `new_function_name`. " +
                    "The original block of code is replaced with a call to the newly created function. ",
            requiredParameters = listOf(
                ToolParameterDescriptor(
                    name = ExtractMethod.Params.startLine,
                    description = "The starting line number from which the block of code will be extracted. Must be a positive integer.",
                    type = ToolParameterType.Integer,
                ),
                ToolParameterDescriptor(
                    name = ExtractMethod.Params.endLine,
                    description = "The ending line number to which the block of code will be extracted. Must be a positive integer greater than or equal to `line_start`.",
                    type = ToolParameterType.Integer,
                ),
                ToolParameterDescriptor(
                    name = ExtractMethod.Params.newName,
                    description = "The name of the new method that will contain the extracted block of code. Must be a valid function name.",
                    type = ToolParameterType.String,
                ),
            ),
            optionalParameters = listOf(),
        ),
    )
}