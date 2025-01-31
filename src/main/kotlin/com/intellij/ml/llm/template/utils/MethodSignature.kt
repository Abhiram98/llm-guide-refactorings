package com.intellij.ml.llm.template.utils

import com.github.javaparser.ast.body.CallableDeclaration
import com.github.javaparser.ast.type.Type
import java.util.regex.Pattern

data class Parameter(val name: String, val type: String)

data class MethodSignature(val methodName: String, val paramsList: List<Parameter>, val returnType:String, val modifier: String){
    companion object{
        fun getMethodSignatureParts(methodSignature: String): MethodSignature? {
            val methodSignatureRegex = Pattern.compile("(\\w+) (\\w+)(\\(.*\\)) : ((\\w(<\\w+>)?)+)")
            var m = methodSignatureRegex.matcher(methodSignature)
            if (m.find()) {
//            val methodInformation = methodSignature.strip().split(" ")
                val modifier = if (m.group(1)=="package") "packageLocal" else m.group(1)
//            val methodNameAndParams = methodInformation[1]
                val methodName = m.group(2)
                val methodParams = m.group(3)
                val returnType = m.group(4)
                return MethodSignature(methodName, getParamsList(methodParams), returnType, modifier)
            }
            return null
        }

        fun getParamsList(methodParams: String): List<Parameter>{
            if (methodParams=="()")
                return emptyList()
            return methodParams.substring(1, methodParams.length-1).split(", ")
                .map {
                    val nameAndType = it.trim().split(" ")
                    Parameter(nameAndType[0], nameAndType[1])
                }
        }
    }

    fun compare(signature: CallableDeclaration.Signature): Boolean{
        if (this.methodName != signature.name) return false
        if (signature.parameterTypes.isEmpty()) return this.paramsList.isEmpty()
        return signature.parameterTypes

                    .mapIndexed { index: Int, type: Type ->
                        this.paramsList.getOrNull(index)?.type == type.tokenRange.get().toString().replace("\\s".toRegex(), "") }
                    .reduce { acc, parameter -> acc && parameter  }
    }

}



