package org.boulderse.ijserver.refactoringobjects.issues

import com.intellij.analysis.AnalysisScope
import com.intellij.analysis.problemsView.ProblemsCollector
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.actions.CodeInspectionAction
import com.intellij.codeInspection.ex.GlobalInspectionContextImpl
import com.intellij.codeInspection.ex.InspectionManagerEx
import com.intellij.codeInspection.ex.InspectionRVContentProviderImpl
import com.intellij.codeInspection.ui.InspectionResultsView
import com.intellij.codeInspection.ui.InspectionResultsViewUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.impl.source.PsiJavaFileImpl
import com.intellij.testFramework.LightPlatformCodeInsightTestCase

class IssueScannerTest : LightPlatformCodeInsightTestCase() {
    private var projectPath = "src/test"

    override fun getTestDataPath(): String = projectPath

    fun `test extract class`() {
        configureByFile("/testdata/ProblemClass.java")
        MyCodeInspectionAction(project, AnalysisScope(file as PsiJavaFileImpl)).doInspect()

        val collector = ProblemsCollector.getInstance(project)
//        ProblemDescriptorImpl()

        DaemonCodeAnalyzerEx.getInstance(project).restart(file)
//        DaemonCodeAnalyzerEx.processHighlights()
//        DaemonCodeAnalyzerEx.getInstanceEx(project).fileStatusMap
        println("Problem Files: $collector")

//        LocalInspectionToolWrapper()
//        val x = InspectionProfileWrapper()

        val inspectionManager = InspectionManager.getInstance(project)
        val inspectionSession = inspectionManager.createNewGlobalContext()
        val analysisScope = AnalysisScope(file)
        MyCodeInspectionAction(project, analysisScope)
//        val g = InspectionManagerEx.getInstance(project)
        val inspectionManagerEx = InspectionManager.getInstance(project) as InspectionManagerEx

//        val i = InspectionResultsView(
//            GlobalInspectionContextImpl(project, inspectionManagerEx.contentManager).initializeTools(),
//            InspectionRVContentProviderImpl())

//        InspectionResultsViewUtil.getPreviewIsNotAvailable()

//        i.update()
//        println(i.hasProblems())
//        i.tree.root
//        inspectionSession. = analysisScope
//        inspectionSession.setUseUniversalAnalysis(true)

//        InspectionEngine.runInspectionOnFile(
//            file,
//            inspectionSession
//        )
    }

    class MyCodeInspectionAction(
        val project: Project,
        val scope: AnalysisScope,
    ) : CodeInspectionAction() {
        fun doInspect() {
            super.runInspections(project, scope)
        }
    }
}
