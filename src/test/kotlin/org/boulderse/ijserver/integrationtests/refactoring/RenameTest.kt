package org.boulderse.ijserver.integrationtests.refactoring

import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.IdeProductProvider
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.plugins.PluginConfigurator
import com.intellij.ide.starter.project.GitHubProject
import com.intellij.ide.starter.runner.Starter
import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.minutes

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.boulderse.ijserver.server.OpenFileParams
import org.boulderse.ijserver.server.RenameParams

class RenameTest {


    @Test
    fun checkSimpleRename() {
        Starter.newContext(testName = "rename sanity",
            TestCase(
                IdeProductProvider.IC,
                projectInfo =
                    GitHubProject.fromGithub(commitHash = "9f64053dc0bb21a2d8714f4fca5ae58cbaef2a7d",
                        repoRelativeUrl = "ratpack/ratpack",
                        branchName = "master")
            )
                .withVersion("2025.2")).apply {
            val pathToPlugin = System.getProperty("path.to.build.plugin")
            PluginConfigurator(this).installPluginFromPath(Path(pathToPlugin))
        }.runIdeWithDriver().useDriverAndCloseIde {
            waitForIndicators(5.minutes)

            val client = HttpClient(CIO)

            runBlocking{
                val response: HttpResponse = client.post("http://localhost:8082/open-file") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        Json.encodeToString(
                            OpenFileParams(filePath = "ratpack-core/src/main/java/ratpack/override/UserRegistryOverrides.java")
                        )
                    )
                }
                println("Response status: ${response.status}")
                println("Response body: ${response.bodyAsText()}")
            }

            runBlocking {
                val response: HttpResponse = client.post("http://localhost:8082/rename") {
                    contentType(ContentType.Application.Json)
                    setBody(Json.encodeToString(RenameParams(oldName = "UserRegistryOverrides", newName = "UserRegistryImpositions")))
                }
                println("Response status: ${response.status}")
                println("Response body: ${response.bodyAsText()}")
            }
        }
    }
}