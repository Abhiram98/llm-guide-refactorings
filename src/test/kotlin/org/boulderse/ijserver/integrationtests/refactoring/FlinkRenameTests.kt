package org.boulderse.ijserver.integrationtests.refactoring

import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.IdeProductProvider
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.plugins.PluginConfigurator
import com.intellij.ide.starter.project.GitHubProject
import com.intellij.ide.starter.runner.Starter
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.minutes

class FlinkRenameTests: RenameTestsBase() {


    val flinkProject = GitHubProject.fromGithub(
        repoRelativeUrl = "apache/flink",
        branchName = "master"
    )

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
            ),
            RenameCase(
                commitHash = "afe4c79efa15902369d41ef5a6e73d79a2e7d525",
                filePath = "flink-core/src/test/java/org/apache/flink/api/common/typeutils/TypeSerializerUpgradeTestBase.java",
                oldName = "testDataMatcher",
                newName = "testDataMatcherRaihan",
                lineNum = 102,
                codeElementType = "method"
            ),
            RenameCase(
                commitHash = "afe4c79efa15902369d41ef5a6e73d79a2e7d525",
                filePath = "flink-core/src/test/java/org/apache/flink/api/common/typeutils/TypeSerializerUpgradeTestBase.java",
                oldName = "testDataMatcher",
                newName = "testDataMatcherRaihan",
                lineNum = 179,
                codeElementType = "method"
            )
        )
    }
}