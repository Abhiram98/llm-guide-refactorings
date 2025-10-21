package org.boulderse.ijserver.integrationtests.refactoring

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.waitForIndicators
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.boulderse.ijserver.server.OpenFileParams
import org.boulderse.ijserver.server.RenameParams
import kotlin.time.Duration.Companion.minutes

open class RenameTestsBase {
    fun Driver.renameTest(renameCase: RenameCase) {
        waitForIndicators(5.minutes)

        val client = HttpClient(CIO)

        runBlocking{
            val response: HttpResponse = client.post("http://localhost:8082/open-file") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(OpenFileParams(filePath = renameCase.filePath)))
            }
            println("File open status status: ${response.status}")
            println("Response body: ${response.bodyAsText()}")
            assert(response.status.value == 200)
        }

        runBlocking {
            val response: HttpResponse = client.post("http://localhost:8082/rename") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(
                    RenameParams(
                        oldName = renameCase.oldName,
                        newName = renameCase.newName,
                        lineNum = renameCase.lineNum,
                        codeElementType = renameCase.codeElementType
                    )))
            }
            println("Rename status: ${response.status}")
            println("Response body: ${response.bodyAsText()}")
            assert(response.status.value == 200)
        }
    }

    data class RenameCase(
        val commitHash: String,
        val oldName: String,
        val newName: String,
        val filePath: String,
        val lineNum: Int? = null,
        val codeElementType: String? = null
    )


}