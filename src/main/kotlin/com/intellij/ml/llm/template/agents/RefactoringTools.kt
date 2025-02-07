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

    object Rename {
        const val NAME = "rename"

        object Params {
            const val oldName = "old_name"
            const val newName = "new_name"
            const val lineNum = "line_num"
        }
    }

    object GetSource {
        const val NAME = "get_updated_source"

        object Params {
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
        ToolDescriptor(
            name = Rename.NAME,
            description = """Renames occurrences of a variable within the scope of a function or method.

            This function is intended to refactor code by replacing all occurrences of the variable named `old_variable_name`
            with the new variable name `new_name` within the scope of the function or method where it is called.""".trimIndent(),
            requiredParameters = listOf(
                ToolParameterDescriptor(
                    name = Rename.Params.oldName,
                    description = "The name of the variable to be renamed.",
                    type = ToolParameterType.String,
                ),
                ToolParameterDescriptor(
                    name = Rename.Params.newName,
                    description = "The new name for the variable.",
                    type = ToolParameterType.String,
                )
            ),
            optionalParameters = listOf(
                ToolParameterDescriptor(
                    name = Rename.Params.lineNum,
                    description = "An optional parameter to identify the variable using a line number, if there are multiple variables with the same name",
                    type = ToolParameterType.String,
                ),
            ),
        ),

        ToolDescriptor(
            name = GetSource.NAME,
            description = """Get the updated source code of the file""".trimIndent(),
            requiredParameters = listOf(),
            optionalParameters = listOf(),
        )
    )
}