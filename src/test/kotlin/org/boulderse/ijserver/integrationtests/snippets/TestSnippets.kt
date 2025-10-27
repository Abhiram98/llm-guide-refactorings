package org.boulderse.ijserver.integrationtests.snippets

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
import org.boulderse.ijserver.server.SnippetFinderParams
import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.minutes

class TestSnippets {
    val flinkProject =
        GitHubProject.fromGithub(
            repoRelativeUrl = "apache/flink",
            branchName = "master",
        )

    @Test
    fun codeSnippetTest() {
        Starter
            .newContext(
                testName = "Code Snippet test",
                TestCase(
                    IdeProductProvider.IC,
                    projectInfo = flinkProject.onCommit("afe4c79efa15902369d41ef5a6e73d79a2e7d525"),
                ).withVersion("2025.2"),
            ).apply {
                val pathToPlugin = System.getProperty("path.to.build.plugin")
                PluginConfigurator(this).installPluginFromPath(Path(pathToPlugin))
            }.runIdeWithDriver()
            .useDriverAndCloseIde {
                waitForIndicators(5.minutes)
                val client = HttpClient(CIO)

                runBlocking {
                    val response: HttpResponse =
                        client.post("http://localhost:8082/open-file") {
                            contentType(ContentType.Application.Json)
                            setBody(
                                Json.encodeToString(
                                    OpenFileParams(
                                        filePath = "flink-core/src/test/java/org/apache/flink/api/java/typeutils/runtime/PojoSerializerUpgradeTestSpecifications.java",
                                    ),
                                ),
                            )
                        }
                    println("File open status status: ${response.status}")
                    println("Response body: ${response.bodyAsText()}")
                    assert(response.status.value == 200)
                }

                runBlocking {
                    val response: HttpResponse =
                        client.post("http://localhost:8082/get_source_code_snippet") {
                            contentType(ContentType.Application.Json)
                            setBody(
                                Json.encodeToString(
                                    SnippetFinderParams(
                                        name = "testDataMatcher",
                                        lineNum = 102,
                                        codeElementType = "method",
                                        filePath = "flink-core/src/test/java/org/apache/flink/api/common/typeutils/TypeSerializerUpgradeTestBase.java",
                                    ),
                                ),
                            )
                        }
                    println("Snippet status: ${response.status}")
                    println("snippet response: ${response.bodyAsText()}")
                    assert(response.status.value == 200)
                }

                runBlocking {
                    val response: HttpResponse =
                        client.post("http://localhost:8082/get_source_code_snippet") {
                            contentType(ContentType.Application.Json)
                            setBody(
                                Json.encodeToString(
                                    SnippetFinderParams(
                                        name = "TypeSerializer",
                                        lineNum = 406,
                                        codeElementType = "class",
                                        filePath = "flink-core/src/test/java/org/apache/flink/api/common/typeutils/TypeSerializerUpgradeTestBase.java",
                                    ),
                                ),
                            )
                        }
                    println("Snippet status: ${response.status}")
                    println("snippet response: ${response.bodyAsText()}")
                    assert(response.status.value == 200)
                }

                runBlocking {
                    val response: HttpResponse =
                        client.post("http://localhost:8082/get_source_code_snippet") {
                            contentType(ContentType.Application.Json)
                            setBody(
                                Json.encodeToString(
                                    SnippetFinderParams(
                                        name = "setupClassloader",
                                        lineNum = 117,
                                        codeElementType = "field",
                                        filePath = "flink-core/src/test/java/org/apache/flink/api/common/typeutils/TypeSerializerUpgradeTestBase.java",
                                    ),
                                ),
                            )
                        }
                    println("Snippet status: ${response.status}")
                    println("snippet response: ${response.bodyAsText()}")
                    assert(response.status.value == 200)
                }
            }
    }
}
