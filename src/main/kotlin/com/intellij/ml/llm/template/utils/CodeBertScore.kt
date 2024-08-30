package com.intellij.ml.llm.template.utils

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.IOException

class CodeBertScore {

    private data class CodeBertRequest(
        @SerializedName("text1")
        val text1: String,
        @SerializedName("text2")
        val text2: String
    )

    private data class CodeBertResponse(
        @SerializedName("score")
        val score: Double
    )

    companion object {
        fun computeCodeBertScore(psiMethod: PsiMethod, psiClass: PsiClass): Double {
            val methodBody = psiMethod.text
            val classBody = psiClass.text

            return computeCodeBertScore(methodBody, classBody)
        }

        fun computeCodeBertScore(text1: String, text2: String): Double {
            val client = OkHttpClient()

            val requestData = CodeBertRequest(text1, text2)
            val jsonBody = Gson().toJson(requestData)

            val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody)

            val request = Request.Builder()
                .url("https://240a-141-142-254-149.ngrok-free.app/compute_codebertscore")
                .post(requestBody)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    val responseBody = response.body?.string()
                    val codeBertResponse = Gson().fromJson(responseBody, CodeBertResponse::class.java)
                    return codeBertResponse.score
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return -1.0 // Return a default value or handle the error as needed
            }
        }
    }
}