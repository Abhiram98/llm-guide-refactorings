package org.boulderse.ijserver.integrationtests.dataflow

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
import org.boulderse.ijserver.refactoringobjects.dataflow.DataFlowAnalyser
import org.boulderse.ijserver.server.OpenFileParams
import org.boulderse.ijserver.server.RenameParams
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.minutes

class TestDataFlow {
    val flinkProject =
        GitHubProject.fromGithub(
            repoRelativeUrl = "apache/flink",
            branchName = "master",
        )

    fun dataFlowTest(case: DataFlowCase) {
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
                client.post("http://localhost:8082/data-flow") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        Json.encodeToString(
                            RenameParams(
                                oldName = case.name,
                                newName = case.name,
                                lineNum = case.lineNum,
                            ),
                        ),
                    )
                }
            println("Rename status: ${response.status}")
            println("Response body: ${response.bodyAsText()}")
            assert(response.status.value == 200)

            val dataFlowResult = Json.decodeFromString<List<DataFlowAnalyser.DataFlowAnalysisResult>>(response.bodyAsText())
            val filesChanges = dataFlowResult.map { it.file }.toSet()
            if (case.totalFiles != null) {
                assert(filesChanges.size == case.totalFiles)
            }
            if (case.files != null) {
                assert(filesChanges.containsAll(case.files))
            }
        }
    }

    @ParameterizedTest(name = "data flow {index}: {0}")
    @MethodSource("dataFlowCases")
    fun dataFlowTestParameterized(case: DataFlowCase) {
        Starter
            .newContext(
                testName = "Data flow test for - ${case.commitHash}",
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

                dataFlowTest(case)
            }
    }

    companion object {
        @JvmStatic
        fun dataFlowCases(): List<DataFlowCase> =
            listOf(
                DataFlowCase(
                    commitHash = "be25a140f011e6ff93a23f28b3826d376a1c0ba7",
                    filePath = "flink-runtime/src/main/java/org/apache/flink/runtime/checkpoint/StandaloneCompletedCheckpointStore.java",
                    name = "ioExecutor",
                    totalFiles = 13,
                    files =
                        listOf(
                            "flink-runtime/src/main/java/org/apache/flink/runtime/checkpoint/StandaloneCompletedCheckpointStore.java",
                            "flink-runtime/src/main/java/org/apache/flink/runtime/checkpoint/CheckpointsCleaner.java",
                            "flink-runtime/src/main/java/org/apache/flink/runtime/checkpoint/Checkpoint.java",
                            "flink-runtime/src/main/java/org/apache/flink/runtime/checkpoint/PendingCheckpoint.java",
                            "flink-runtime/src/test/java/org/apache/flink/runtime/checkpoint/CompletedCheckpointStoreTest.java",
                            "flink-runtime/src/main/java/org/apache/flink/runtime/checkpoint/CompletedCheckpoint.java",
                            "flink-core/src/main/java/org/apache/flink/util/concurrent/FutureUtils.java",
                            "flink-core/src/main/java/org/apache/flink/util/concurrent/Executors.java",
                            "flink-runtime/src/main/java/org/apache/flink/runtime/checkpoint/StandaloneCheckpointRecoveryFactory.java",
                            "flink-runtime/src/test/java/org/apache/flink/runtime/checkpoint/PerJobCheckpointRecoveryTest.java",
                            "flink-runtime/src/main/java/org/apache/flink/runtime/scheduler/SchedulerUtils.java",
                            "flink-runtime/src/main/java/org/apache/flink/runtime/dispatcher/cleanup/CheckpointResourcesCleanupRunner.java",
                            "flink-core/src/main/java/org/apache/flink/util/Preconditions.java",
                        ),
                ),
                DataFlowCase(
                    commitHash = "be25a140f011e6ff93a23f28b3826d376a1c0ba7",
                    filePath = "flink-runtime/src/main/java/org/apache/flink/runtime/checkpoint/Checkpoints.java",
                    name = "LOG",
                    totalFiles = 1,
                    files = listOf("flink-runtime/src/main/java/org/apache/flink/runtime/checkpoint/Checkpoints.java"),
                ),
                DataFlowCase(
                    commitHash = "be25a140f011e6ff93a23f28b3826d376a1c0ba7",
                    filePath = "flink-runtime/src/main/java/org/apache/flink/runtime/checkpoint/Checkpoints.java",
                    name = "Checkpoints",
                    totalFiles = null,
                    files =
                        listOf(
                            "flink-runtime/src/test/java/org/apache/flink/runtime/jobmaster/TestUtils.java",
                            "flink-runtime/src/test/java/org/apache/flink/runtime/dispatcher/DispatcherTest.java",
                            "flink-libraries/flink-state-processing-api/src/main/java/org/apache/flink/state/api/runtime/SavepointLoader.java",
                            "flink-runtime/src/test/java/org/apache/flink/runtime/checkpoint/CheckpointMetadataLoadingTest.java",
                            "flink-runtime/src/main/java/org/apache/flink/runtime/checkpoint/PendingCheckpoint.java",
                            "flink-runtime/src/test/java/org/apache/flink/runtime/checkpoint/CheckpointsTest.java",
                            "flink-libraries/flink-state-processing-api/src/main/java/org/apache/flink/state/api/output/SavepointOutputFormat.java",
                            "flink-test-utils-parent/flink-test-utils/src/main/java/org/apache/flink/test/util/TestUtils.java",
                            "flink-runtime/src/main/java/org/apache/flink/runtime/checkpoint/Checkpoints.java",
                            "flink-runtime/src/main/java/org/apache/flink/runtime/checkpoint/CheckpointCoordinator.java",
                            "flink-runtime/src/main/java/org/apache/flink/runtime/dispatcher/Dispatcher.java",
                            "flink-runtime/src/test/java/org/apache/flink/runtime/operators/coordination/OperatorCoordinatorSchedulerTest.java",
                        ),
                ),
                DataFlowCase(
                    commitHash = "be25a140f011e6ff93a23f28b3826d376a1c0ba7",
                    filePath = "flink-clients/src/main/java/org/apache/flink/client/cli/CliFrontendParser.java",
                    name = "restoreMode",
                    lineNum = 700,
                    totalFiles = 12,
                ),
            )
    }

    data class DataFlowCase(
        val commitHash: String,
        val filePath: String,
        val name: String,
        val totalFiles: Int? = null,
        val files: List<String>? = null,
        val lineNum: Int? = null,
    )
}
