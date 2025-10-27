package org.boulderse.ijserver.integrationtests.search

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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import org.boulderse.ijserver.server.OpenFileParams
import org.boulderse.ijserver.server.SymbolSearchParams
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.minutes

class TestSearch {
    val flinkProject =
        GitHubProject.fromGithub(
            repoRelativeUrl = "apache/flink",
            branchName = "master",
        )

    fun searchTest(case: SearchCase) {
        val client = HttpClient(CIO)

        runBlocking {
            val response: HttpResponse =
                client.post("http://localhost:8082/open-file") {
                    contentType(ContentType.Application.Json)
                    setBody(Json.encodeToString(OpenFileParams(filePath = case.filePath)))
                }
            println("File open status status: ${response.status}")
            println("Response body: ${response.bodyAsText()}")
            assert(response.status.value == 200)
        }

        runBlocking {
            val response: HttpResponse =
                client.post("http://localhost:8082/search_symbol") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        Json.encodeToString(
                            SymbolSearchParams(
                                symbol = case.name,
                            ),
                        ),
                    )
                }
            println("Rename status: ${response.status}")
            println("Response body: ${response.bodyAsText()}")
            assert(response.status.value == 200)

            val searchResult = Json.decodeFromString<JsonObject>(response.bodyAsText())
            assert(searchResult.containsKey("files"))
            assert(searchResult.containsKey("hit_count"))

            if (case.totalFiles != null) {
                assert(searchResult.get("files")!!.jsonArray.size == case.totalFiles)
            }
        }
    }

    @ParameterizedTest(name = "search test {index}: {0}")
    @MethodSource("searchCases")
    fun searchTestParameterized(case: SearchCase) {
        Starter
            .newContext(
                testName = "Search test for - ${case.commitHash}",
                TestCase(
                    IdeProductProvider.IC,
                    projectInfo = flinkProject.onCommit(case.commitHash),
                ).withVersion("2025.2"),
            ).apply {
                val pathToPlugin = System.getProperty("path.to.build.plugin")
                PluginConfigurator(this).installPluginFromPath(Path(pathToPlugin))
            }.runIdeWithDriver()
            .useDriverAndCloseIde {
                waitForIndicators(5.minutes)

                searchTest(case)
            }
    }

    companion object {
        @JvmStatic
        fun searchCases(): List<SearchCase> =
            listOf(
                SearchCase(
                    commitHash = "afe4c79efa15902369d41ef5a6e73d79a2e7d525",
                    filePath = "flink-core/src/test/java/org/apache/flink/api/common/typeutils/TypeSerializerUpgradeTestBase.java",
                    name = "testDataMatcher",
                    totalFiles = null,
                ),
                SearchCase(
                    commitHash = "be25a140f011e6ff93a23f28b3826d376a1c0ba7",
                    filePath = "flink-runtime-web/src/main/java/org/apache/flink/runtime/webmonitor/handlers/JarRunRequestBody.java",
                    name = "restoreMode",
                    totalFiles = 2,
                ),
                SearchCase(
                    commitHash = "afe4c79efa15902369d41ef5a6e73d79a2e7d525",
                    filePath = "flink-core/src/main/java/org/apache/flink/api/java/typeutils/runtime/PojoSerializer.java",
                    name = "serializerConfig",
                    totalFiles = 2,
                ),
            )
    }

    data class SearchCase(
        val commitHash: String,
        val filePath: String,
        val name: String,
        val totalFiles: Int? = null,
    )
}
