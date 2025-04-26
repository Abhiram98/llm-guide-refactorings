package com.intellij.ml.llm.template.server

import com.intellij.ml.llm.template.utils.MethodSignature
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenFileParams(
    @SerialName("rel_file_path")
    val filePath: String
)
@Serializable
data class OpenProjectParams(
    @SerialName("abs_project_path")
    val projectPath: String
)


@Serializable
data class RenameParams(
    @SerialName("old_name")
    val oldName: String,
    @SerialName("new_name")
    val newName: String,
    @SerialName("line_num")
    val lineNum: Int? = null
)

@Serializable
data class ExtractMethodParams(
    @SerialName("start_line")
    val startLine: Int,
    @SerialName("end_line")
    val endLine: Int,
    @SerialName("new_method_name")
    val newName: String
)

@Serializable
data class MoveMethodParams(
    @SerialName("method_name")
    val methodName: String,
    @SerialName("target_class")
    val targetClass: String
)


@Serializable
enum class ExtractionType(
){
    @SerialName("interface")
    INTERFACE,
    @SerialName("super_class")
    SUPERCLASS,
    @SerialName("class")
    CLASS,
    @SerialName("enum")
    ENUM
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
    val members: List<String>
)


@Serializable
data class PushDownParams(

    @SerialName("members")
    val members: List<String>,

    @SerialName("keep_abstract")
    val keepAbstract: Boolean = true
)

@Serializable
data class PullUpParams(

    @SerialName("super_class")
    val superClass: String,

    @SerialName("members")
    val members: List<String>,

    @SerialName("make_abstract")
    val makeAbstract: Boolean = true
)



@Serializable
data class ChangeSignatureParams(

    @SerialName("method_name")
    val methodName: String,

    @SerialName("method_line_num")
    val lineNum: Int? = null,

    @SerialName("new_signature")
    val newSignature: MethodSignature
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
    val newClassName: String

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
    val makeStatic: Boolean

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
    val makeStatic: Boolean

)


@Serializable
data class TypeChangeParams(

    @SerialName("variable_name")
    val variableName: String,

    @SerialName("line_num")
    val lineNum: Int? = null,

    @SerialName("new_type")
    val newType: String

)

@Serializable
data class GetLinksParams(

    @SerialName("method_name")
    val methodName: String,

    @SerialName("line_num")
    val lineNum: Int
)