package com.intellij.ml.llm.template.server

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
    val methodName: String,
    val targetClass: String
)

@Serializable
data class ExtractClassParams(

    @SerialName("extract_interface")
    val extractInterface: Boolean = false,

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
