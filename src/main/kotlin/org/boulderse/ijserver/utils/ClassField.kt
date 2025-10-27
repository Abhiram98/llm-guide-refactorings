package org.boulderse.ijserver.utils

import com.google.gson.annotations.SerializedName

data class ClassField(
    @SerializedName("field_name")
    val fieldName: String,
    @SerializedName("field_type")
    val fieldType: String,
    @SerializedName("field_declaration")
    val fieldDeclaration: String,
)
