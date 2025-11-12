package org.boulderse.ijserver.server

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.boulderse.ijserver.utils.MethodSignature

@Serializable
data class OpenFileParams(
    @SerialName("rel_file_path")
    val filePath: String,
    @SerialName("open_editor")
    val openEditor: Boolean = false,
)

@Serializable
data class OpenProjectParams(
    @SerialName("abs_project_path")
    val projectPath: String,
)

@Serializable
data class SnippetFinderParams(
    @SerialName("name")
    val name: String,
    @SerialName("line_num")
    val lineNum: Int? = null,
    @SerialName("code_element_type")
    val codeElementType: String? = null,
    @SerialName("file_path")
    val filePath: String? = null,
)

@Serializable
data class RenameParams(
    @SerialName("old_name")
    val oldName: String,
    @SerialName("new_name")
    val newName: String,
    @SerialName("line_num")
    val lineNum: Int? = null,
    @SerialName("code_element_type")
    val codeElementType: String? = null,
    @SerialName("start_line_comments")
    val startLineComments: Int? = null,
    @SerialName("resolved_file_path")
    val resolvedFilePath: String? = null,
    @SerialName("resolved_start_line")
    val resolvedStartLine: Int? = null,
    @SerialName("reason")
    val reason: String? = null,
)

@Serializable
data class ExtractMethodParams(
    @SerialName("start_line")
    val startLine: Int,
    @SerialName("end_line")
    val endLine: Int,
    @SerialName("new_method_name")
    val newName: String,
)

@Serializable
data class MoveMethodParams(
    @SerialName("method_name")
    val methodName: String,
    @SerialName("target_class")
    val targetClass: String,
)

@Serializable
enum class ExtractionType {
    @SerialName("interface")
    INTERFACE,

    @SerialName("super_class")
    SUPERCLASS,

    @SerialName("class")
    CLASS,

    @SerialName("enum")
    ENUM,
}

@Serializable
data class ExtractClassParams(
    @SerialName("extraction_type")
    val extractionType: ExtractionType = ExtractionType.CLASS, // class/enum/interface/superclass
    @SerialName("new_class_name")
    val newName: String,
    @SerialName("sub_class_name")
    val subClassName: String,
    @SerialName("members")
    val members: List<String>,
)

@Serializable
data class PushDownParams(
    @SerialName("members")
    val members: List<String>,
    @SerialName("keep_abstract")
    val keepAbstract: Boolean = true,
)

@Serializable
data class PullUpParams(
    @SerialName("super_class")
    val superClass: String,
    @SerialName("members")
    val members: List<String>,
    @SerialName("make_abstract")
    val makeAbstract: Boolean = true,
)

@Serializable
data class ChangeSignatureParams(
    @SerialName("method_name")
    val methodName: String,
    @SerialName("method_line_num")
    val lineNum: Int? = null,
    @SerialName("new_signature")
    val newSignature: MethodSignature,
)

@Serializable
data class IntroduceParamObjectParams(
    @SerialName("method_name")
    val methodName: String,
    @SerialName("method_line_num")
    val lineNum: Int? = null,
    @SerialName("parameter_names")
    val paramNames: List<String>,
    @SerialName("new_class_name")
    val newClassName: String,
)

@Serializable
data class ExtractFieldParams(
    @SerialName("new_field_name")
    val newFieldName: String,
    @SerialName("line_num")
    val lineNum: Int? = null,
    @SerialName("variable_name")
    val variableName: String,
    @SerialName("make_static")
    val makeStatic: Boolean,
)

@Serializable
data class ExtractFieldFromLiteralParams(
    @SerialName("new_field_name")
    val newFieldName: String,
    @SerialName("line_num")
    val lineNum: Int? = null,
    @SerialName("literal_value")
    val literalValue: String,
    @SerialName("make_static")
    val makeStatic: Boolean,
)

@Serializable
data class TypeChangeParams(
    @SerialName("variable_name")
    val variableName: String,
    @SerialName("line_num")
    val lineNum: Int? = null,
    @SerialName("new_type")
    val newType: String,
)

@Serializable
data class GetLinksParams(
    @SerialName("method_name")
    val methodName: String? = null,
    @SerialName("line_num")
    val lineNum: Int,
)

@Serializable
data class FindReplaceParams(
    @SerialName("find_text")
    val findText: String,
    @SerialName("replace_text")
    val replaceText: String,
    @SerialName("replace_in_comments_only")
    val replaceInComments: Boolean = true,
    @SerialName("line_num")
    val lineNum: Int? = null,
)

@Serializable
data class SymbolSearchParams(
    @SerialName("symbol")
    val symbol: String,
    @SerialName("parent_count")
    val parentCount: Int = 2,
)

@Serializable
data class RenamePairParams(
    @SerialName("old_name")
    val oldName: String,
    @SerialName("new_name")
    val newName: String,
)

@Serializable
data class ReviewScopeParams(
    @SerialName("pattern")
    val pattern: String,
    @SerialName("condition")
    val guard: String,
)

@Serializable
data class RenamesToReviewParams(
    @SerialName("count")
    val count: Int,
)

@Serializable
data class RenamesNoOpParams(
    @SerialName("status")
    val status: String? = null,
)
