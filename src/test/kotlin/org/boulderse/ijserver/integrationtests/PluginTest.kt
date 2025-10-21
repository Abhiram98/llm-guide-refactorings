package org.boulderse.ijserver.integrationtests

import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.IdeProductProvider
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.plugins.PluginConfigurator
import com.intellij.ide.starter.project.GitHubProject
import com.intellij.ide.starter.project.NoProject
import com.intellij.ide.starter.runner.Starter
import com.intellij.openapi.project.getOpenedProjects
import com.intellij.testFramework.StartupActivityTestUtil
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import java.net.URL
import kotlin.time.Duration.Companion.minutes


class PluginTest {

//    init {
//        di = DI {
//            extend(di)
//            bindSingleton<CIServer>(overrides = true) {
//                object : CIServer by NoCIServer {
//                    override fun reportTestFailure(testName: String, message: String, details: String, linkToLogs: String?) {
//                        fail { "$testName fails: $message. \n$details" }
//                    }
//                }
//            }
//        }
//    }

    @Test
    fun sanityLoadPlugin() {
        Starter.newContext(testName = "sanity",
            TestCase(
                IdeProductProvider.IC,
                projectInfo = NoProject,
//                    GitHubProject.fromGithub(branchName = "master", repoRelativeUrl = "JetBrains/ij-perf-report-aggregator")
            )
                .withVersion("2025.2")).apply {
            val pathToPlugin = System.getProperty("path.to.build.plugin")
            PluginConfigurator(this).installPluginFromPath(Path(pathToPlugin))
        }.runIdeWithDriver().useDriverAndCloseIde {
            print("Hello world!")
        }
    }

    @Test
    fun serverUpSanity() {
        Starter.newContext(testName = "sanity server is running",
            TestCase(
                IdeProductProvider.IC,
                projectInfo =
                    GitHubProject.fromGithub(branchName = "master", repoRelativeUrl = "apache/flink")
            )
                .withVersion("2025.2")).apply {
            val pathToPlugin = System.getProperty("path.to.build.plugin")
            PluginConfigurator(this).installPluginFromPath(Path(pathToPlugin))
        }.runIdeWithDriver().useDriverAndCloseIde {
            waitForIndicators(5.minutes)
            val client = OkHttpClient()
            val request = okhttp3.Request.Builder()
                .url("http://localhost:8082/")
                .build()

            client.newCall(request).execute().use { response ->
                println(response.body!!.string())
                println("Server is up!")
            }

        }
    }
}