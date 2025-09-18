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

    private fun recurse(usage: SliceNode){
        val sliceUsage = usage.element?.value

        if (sliceUsage!=null) {
            if (!sliceUsage.file.path.contains(project.basePath.toString())) {
                print("returning becuase the usage path was not in the project.")
                return
            }
            files.add(sliceUsage.path)
            usage.getChildren().forEach {
                recurse(it)
            }
        }
    }


}