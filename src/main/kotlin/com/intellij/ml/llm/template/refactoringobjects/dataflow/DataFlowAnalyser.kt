package com.intellij.ml.llm.template.refactoringobjects.dataflow

import com.intellij.analysis.AnalysisScope
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.slicer.*

class DataFlowAnalyser(val psiElement: PsiElement, val project: Project, val dataFlowTo: Boolean) {

    val files = mutableSetOf<String>()

    fun analyse(): List<String>{
        val params = SliceAnalysisParams()
        params.dataFlowToThis = dataFlowTo
        // todo: set the scope to global.
        params.scope = AnalysisScope(project)
        val rootNode = SliceRootNode(
            project, DuplicateMap(),
            LanguageSlicing.getProvider(psiElement)
                .createRootUsage(
                    psiElement, params
                )
        )
        runReadAction{ rootNode.children.forEach { recurse(it) } }

        return files.toList()
    }

    private fun processChildren(sliceUsage: SliceUsage) {
        ProgressManager.getInstance()
            .runProcessWithProgressSynchronously(
                kotlinx.coroutines.Runnable {
                    sliceUsage.processChildren {
                        files.add(it.file.path)
                        true
                    }
                },
                "Server: Data Flow Analysis",
                true,
                project
            )
    }

    private fun recurse(usage: SliceNode, pathFiles: MutableSet<String> = mutableSetOf(), depth: Int = 0){
//        if (depth >= 15) return

        val sliceUsage = usage.element?.value?: return

        val filePath = sliceUsage.file.path
        if (!filePath.contains(project.basePath.toString())) {
            print("returning because the usage path was not in the project.")
            return
        }

        val newPathFiles = pathFiles.toMutableSet()
        newPathFiles.add(filePath)
        files.add(filePath)

        if (newPathFiles.size >= 3) return

        usage.getChildren().forEach {
            recurse(it, newPathFiles, depth+1)
        }
    }


}