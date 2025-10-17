package org.boulderse.ijserver.refactoringobjects.dataflow

import com.intellij.analysis.AnalysisScope
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.slicer.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class DataFlowAnalyser(val psiElement: PsiElement, val project: Project, val dataFlowTo: Boolean) {

    @Serializable
    data class DataFlowAnalysisResult(
        @SerialName("file_path")
        val file: String,
        @SerialName("depth")
        val depth: Int
    )


    val results = mutableSetOf<DataFlowAnalysisResult>()

    private val TIME_LIMIT_MS = 30_000L // 30 seconds
    private var startTime: Long = 0

    fun analyse(): List<DataFlowAnalysisResult>{
        val params = SliceAnalysisParams()
        params.dataFlowToThis = dataFlowTo
        params.scope = AnalysisScope(project)
        val rootNode = runReadAction{
            SliceRootNode(
                project, DuplicateMap(),
                LanguageSlicing.getProvider(psiElement)
                    .createRootUsage(
                        psiElement, params
                    )
            )
        }
        startTime = System.currentTimeMillis()
        val latch = java.util.concurrent.CountDownLatch(1)

        val task = object : Task.Backgroundable(
            project, "Data flow analysis"
        ) {
            override fun run(indicator: ProgressIndicator) {
                runReadAction{ rootNode.children.forEach { recurse(it) } }
            }

            override fun onFinished() {
                print("Data flow analysis complete.")
                latch.countDown()
                super.onFinished()
            }

            override fun onCancel() {
                print("Data flow analysis was cancelled.")
                super.onCancel()
            }
        }

        val scheduler = Executors.newSingleThreadScheduledExecutor()
        val indicator = BackgroundableProcessIndicator(task)
        scheduler.schedule({
            indicator.cancel()
        }, TIME_LIMIT_MS, TimeUnit.MILLISECONDS)
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, indicator)
        latch.await()

        scheduler.shutdown()
        return results.toList()
    }
    private fun recurse(usage: SliceNode, pathFiles: MutableSet<String> = mutableSetOf(), depth: Int = 0){
        if (depth >= 15) return

        val sliceUsage = usage.element?.value?: return

        val filePath = sliceUsage.file.path
        if (!filePath.contains(project.basePath.toString())) {
            print("returning because the usage path was not in the project.")
            return
        }

        val newPathFiles = pathFiles.toMutableSet()
        newPathFiles.add(filePath)
        results.add(DataFlowAnalysisResult(
            file = filePath.removePrefix(project.basePath.toString()).removePrefix("/"),
            depth = depth))

        if (newPathFiles.size >= 3) return

        usage.getChildren().forEach {
            recurse(it, newPathFiles, depth+1)
        }
    }


}