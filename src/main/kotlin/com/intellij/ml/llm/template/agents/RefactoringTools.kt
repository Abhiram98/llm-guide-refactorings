package com.intellij.ml.llm.template.agents


import ai.grazie.code.agents.core.tools.model.ToolDescriptor
import ai.grazie.code.agents.core.tools.model.ToolParameterDescriptor
import ai.grazie.code.agents.core.tools.model.ToolParameterType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
        const val NAME = "get_source_code"

        object Params {
            const val filePath = "file_path"
        }
    }

    object CurateTests {
        const val NAME = "curate_test_class"

        object Params {
            const val filePath = "file_path"
        }
    }

    object RunTestClass {
        const val NAME = "run_test_class"

        object Params {
            const val filePath = "file_path"
        }
    }

    object ReplaceFile {
        const val NAME = "replace_file_contents"

        object Params {
            const val filePath = "file_path"
            const val newContent = "new_content"
        }

        @Serializable
        data class CallParams(
            @SerialName(Params.filePath)
            val filePath: String,
            @SerialName(Params.newContent)
            val newContent: String
        )
    }

    object ReplaceMethod {
        const val NAME = "replace_method_contents"

        object Params {
            const val filePath = "file_path"
            const val methodName = "method_name"
            const val newContent = "new_content"
            const val lineNum = "line_num"
        }

        @Serializable
        data class CallParams(
            @SerialName(Params.filePath)
            val filePath: String,
            @SerialName(Params.methodName)
            val methodName: String,
            @SerialName(Params.newContent)
            val newContent: String,
            @SerialName(Params.lineNum)
            val lineNum: Int? = null
        )
    }

    object IntroduceParameterLiteral{
        const val NAME = "introduce_parameter_literal"
        object Params {
            const val methodName = "method_name"
            const val methodLineNum = "method_line_num"
            const val parameterName = "parameter_name"
            const val literalValue = "literal_value"
        }

        @Serializable
        data class CallParams(
            @SerialName(IntroduceParameterLiteral.Params.methodName)
            val methodName: String,
            @SerialName(IntroduceParameterLiteral.Params.methodLineNum)
            val methodLineNum: Int? = null,
            @SerialName(IntroduceParameterLiteral.Params.parameterName)
            val parameterName: String,
            @SerialName(IntroduceParameterLiteral.Params.literalValue)
            val literalValue: String,
        )

    }

    object IntroduceParameterLocalVariable{
        const val NAME = "introduce_parameter_variable"
        object Params {
            const val methodName = "method_name"
            const val methodLineNum = "method_line_num"
            const val parameterName = "parameter_name"
            const val variableName = "variable_name"
        }

        @Serializable
        data class CallParams(
            @SerialName(Params.methodName)
            val methodName: String,
            @SerialName(Params.methodLineNum)
            val methodLineNum: Int? = null,
            @SerialName(Params.parameterName)
            val parameterName: String,
            @SerialName(Params.variableName)
            val variableName: String,
        )

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
        ),

        ToolDescriptor(
            name = CurateTests.NAME,
            description = """Find and/or create appropriate test-cases for the source code.""".trimIndent(),
            requiredParameters = listOf(),
            optionalParameters = listOf(),
        ),

        ToolDescriptor(
            name = RunTestClass.NAME,
            description = """Run the curated test-cases and report results.""".trimIndent(),
            requiredParameters = listOf(),
            optionalParameters = listOf(),
        ),

        ToolDescriptor(
            name = ReplaceFile.NAME,
            description = """Replace the entire contents of the chosen file with the newly provided contents,
                 overwriting any existing data.""".trimIndent(),
            requiredParameters = listOf(
                ToolParameterDescriptor(
                    name = ReplaceFile.Params.filePath,
                    description = "The path to the file that will be updated.",
                    type = ToolParameterType.String,
                ),
                ToolParameterDescriptor(
                    name = ReplaceFile.Params.newContent,
                    description = "The replacement text to overwrite the original file contents with.",
                    type = ToolParameterType.String,
                )
            ),
            optionalParameters = listOf(),
        ),

        ToolDescriptor(
            name = ReplaceMethod.NAME,
            description = """Replace the entire contents of the chosen method with the newly provided contents, overwriting 
                any existing data.""".trimIndent(),
            requiredParameters = listOf(
                ToolParameterDescriptor(
                    name = ReplaceMethod.Params.filePath,
                    description = "The path to the file that will be updated.",
                    type = ToolParameterType.String,
                ),
                ToolParameterDescriptor(
                    name = ReplaceMethod.Params.methodName,
                    description = "The name of the method that will be updated.",
                    type = ToolParameterType.String,
                ),
                ToolParameterDescriptor(
                    name = ReplaceMethod.Params.newContent,
                    description = "The replacement text to overwrite the method's contents with.",
                    type = ToolParameterType.String,
                )
            ),
            optionalParameters = listOf(
                ToolParameterDescriptor(
                    name = ReplaceMethod.Params.lineNum,
                    description = "Line number to identify the method at",
                    type = ToolParameterType.Integer
                )
            ),
        ),




        )
}