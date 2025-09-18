package com.intellij.ml.llm.template.refactoringobjects.dataflow

import com.intellij.analysis.AnalysisScope
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.slicer.DuplicateMap
import com.intellij.slicer.LanguageSlicing
import com.intellij.slicer.SliceAnalysisParams
import com.intellij.slicer.SliceRootNode
import org.jetbrains.kotlin.idea.base.util.projectScope

class DataFlowAnalyser(val psiElement: PsiElement, val project: Project) {

    val files = mutableListOf<String>()


//    val dfaRunner = StandardDataFlowRunner(project)
//    class Listener : JavaDfaListener {
//        override fun beforeAssignment(
//            source: DfaValue,
//            dest: DfaValue,
//            state: DfaMemoryState,
//            anchor: DfaAnchor?
//        ) {
//            super.beforeAssignment(source, dest, state, anchor)
//        }
//
//        override fun beforeValueReturn(
//            value: DfaValue,
//            expression: PsiExpression?,
//            context: PsiElement,
//            state: DfaMemoryState
//        ) {
//            super.beforeValueReturn(value, expression, context, state)
//        }
//
//        override fun beforeInstanceInitializerEnd(state: DfaMemoryState?) {
//            super.beforeInstanceInitializerEnd(state)
//        }
//
//        override fun beforeExpressionPush(
//            value: DfaValue,
//            expression: PsiExpression,
//            state: DfaMemoryState
//        ) {
//            super.beforeExpressionPush(value, expression, state)
//        }
//
//        override fun beforeMethodReferenceArgumentPush(
//            value: DfaValue,
//            expression: PsiMethodReferenceExpression,
//            state: DfaMemoryState
//        ) {
//            super.beforeMethodReferenceArgumentPush(value, expression, state)
//        }
//
//        override fun beforePush(
//            args: Array<out DfaValue>,
//            value: DfaValue,
//            anchor: DfaAnchor,
//            state: DfaMemoryState
//        ) {
//            super.beforePush(args, value, anchor, state)
//        }
//
//        override fun onCondition(
//            problem: UnsatisfiedConditionProblem,
//            value: DfaValue,
//            failed: ThreeState,
//            state: DfaMemoryState
//        ) {
//            super.onCondition(problem, value, failed, state)
//        }
//    }

    fun analyse(): List<String>{
        val params = SliceAnalysisParams()
        params.dataFlowToThis = false
        // todo: set the scope to global.
        params.scope = AnalysisScope(project)
        val rootNode = SliceRootNode(
            project, DuplicateMap(),
            LanguageSlicing.getProvider(psiElement)
                .createRootUsage(
                    psiElement, params
                )
        )
        ProgressManager.getInstance()
            .runProcessWithProgressSynchronously(
                kotlinx.coroutines.Runnable {
                    rootNode.rootUsage.processChildren {
                        files.add(it.file.path)
                        true
                    }
                },
                "Server: Data Flow Analysis",
                true,
                project
            )
        val children = rootNode.children
        children.forEach {
            it.element
        }

        return files
    }


}