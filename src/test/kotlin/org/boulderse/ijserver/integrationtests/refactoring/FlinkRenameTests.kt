package org.boulderse.ijserver.integrationtests.refactoring

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.IdeProductProvider
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.plugins.PluginConfigurator
import com.intellij.ide.starter.project.GitHubProject
import com.intellij.ide.starter.runner.Starter
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.minutes

class FlinkRenameTests {


    val flinkProject = GitHubProject.fromGithub(
        repoRelativeUrl = "apache/flink")

    

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


    @ParameterizedTest(name = "Flink rename case {index}: {0}")
    @MethodSource("renameCases")
    fun flinkRenameTestParameterized(case: RenameCase) {
        Starter.newContext(testName = "Suite to trigger renames in apache/flink - ${case.commitHash}",
            TestCase(
                IdeProductProvider.IC,
                projectInfo = flinkProject.onCommit(case.commitHash)
            )
                .withVersion("2025.2")).apply {
            val pathToPlugin = System.getProperty("path.to.build.plugin")
            PluginConfigurator(this).installPluginFromPath(Path(pathToPlugin))
        }.runIdeWithDriver().useDriverAndCloseIde {
            waitForIndicators(5.minutes)
            // call the extension on the active driver
            renameTest(case)
        }
    }

    companion object {
        @JvmStatic
        fun renameCases(): List<RenameCase> = listOf(
            RenameCase(
                commitHash = "21403e31f4761bdddf5e4e802e0e5eb9b4533202",
                filePath = "flink-runtime/src/main/java/org/apache/flink/runtime/state/filesystem/FsStateBackend.java",
                oldName = "createOperatorStateBackend",
                newName = "createOperatorStateBackend2",
                lineNum = null
            ),
            RenameCase(
                commitHash = "21403e31f4761bdddf5e4e802e0e5eb9b4533202",
                filePath = "flink-runtime/src/main/java/org/apache/flink/runtime/state/filesystem/FsStateBackend.java",
                oldName = "FsStateBackend",
                newName = "FsStateBackend2",
                lineNum = null
            ),
            RenameCase(
                commitHash = "32aa7973daaf70e097f247eaf5a3869b47d8e3e0",
                filePath = "flink-runtime/src/main/java/org/apache/flink/runtime/checkpoint/metadata/MetadataV3Serializer.java",
                oldName = "deserializeStreamStateHandle",
                newName = "deserializeStreamStateHandle2",
                lineNum = 265,
                codeElementType = "method"
            )
        )
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