package com.intellij.ml.llm.template.refactoringobjects

import com.intellij.analysis.AnalysisScope
import com.intellij.codeInspection.ProblemDescriptorBase
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.actions.CodeInspectionAction
import com.intellij.codeInspection.ex.GlobalInspectionContextImpl
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.ui.tree.TreePathUtil

class IdeInspection(val project: Project, val scope: AnalysisScope, val file: PsiFile): CodeInspectionAction(){

    var myGlobalInspectionContext: GlobalInspectionContextImpl? = null
    val problems: MutableList<String> = mutableListOf()
    fun doInspect(){
        super.runInspections(project, scope)
        // HACK to access private field. Ther should be a better way to do this.
        val f = this.javaClass.superclass.getDeclaredField("myGlobalInspectionContext")
        f.isAccessible = true
        myGlobalInspectionContext = f.get(this) as GlobalInspectionContextImpl
        println(myGlobalInspectionContext)
//                        myGlobalInspectionContext
//                        val view = super.myGlobalInspectionContext.view
//                        view.tree.root // traverse tree and find problems.
    }
    fun waitForCompletion(){
        if (myGlobalInspectionContext == null)
            return
        while(myGlobalInspectionContext!!.view == null){
            Thread.sleep(1000)
        }
        val p = myGlobalInspectionContext!!.view.tree.getSelectedDescriptorPacks(
            true, mutableSetOf(file.virtualFile),true, arrayOf(TreePathUtil.pathToTreeNode(myGlobalInspectionContext!!.view.tree.root))
        )
        val problemDescriptions = p.map { it.filter {
                it2 ->
            val problem = it2 as? ProblemDescriptorBase
//            problem?.highlightType == ProblemHighlightType.ERROR || problem?.highlightType == ProblemHighlightType.GENERIC_ERROR
            problem!=null
        }.map {
                it2 ->
            val problem = it2 as? ProblemDescriptorBase
            problem
        }.filterNotNull()
            .map {
                    it2 -> "${it2.highlightType} on line ${it2.lineNumber}: ${it2.toString()}"
            }
        }.reduce { acc, strings -> acc + strings }
        problems.addAll(problemDescriptions)

        println("got the view!")
    }
}