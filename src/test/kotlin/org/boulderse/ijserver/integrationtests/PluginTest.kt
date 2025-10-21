package org.boulderse.ijserver.integrationtests

import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.IdeProductProvider
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.plugins.PluginConfigurator
import com.intellij.ide.starter.project.NoProject
import com.intellij.ide.starter.runner.Starter
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

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
    fun simpleTestWithoutProject() {
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
}