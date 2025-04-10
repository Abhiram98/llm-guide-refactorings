package com.intellij.ml.llm.template.refactoringobjects

import com.intellij.analysis.AnalysisScope
import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.codeInspection.ProblemDescriptorBase
import com.intellij.codeInspection.actions.CodeInspectionAction
import com.intellij.codeInspection.ex.GlobalInspectionContextImpl
import com.intellij.codeInspection.ui.InspectionTreeNode
import com.intellij.codeInspection.ui.ProblemDescriptionNode
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
        Thread.sleep(5000)
        if (myGlobalInspectionContext == null)
            return
        while(myGlobalInspectionContext!!.view == null){
            Thread.sleep(1000)
        }

        val root = myGlobalInspectionContext!!.view.tree.root
        val errors = getAllProblemChildren(root)
        val descriptions = errors.filter {
            val level = it.javaClass.getDeclaredField("myLevel")
            level.isAccessible = true
            (level.get(it) as HighlightDisplayLevel) == HighlightDisplayLevel.ERROR
        }.map{
            val desc = (it.descriptor as? ProblemDescriptorBase)
            "Error on line ${desc?.lineNumber}: ${it}"
        }
        problems.addAll(descriptions)

        println("got the view!")
    }

    private fun getAllProblemChildren(root: InspectionTreeNode): List<ProblemDescriptionNode>{
        val problemNodes = mutableListOf<ProblemDescriptionNode>()
        for (c in root.children){
            val problemDescriptionNode = c as? ProblemDescriptionNode
            if (problemDescriptionNode !=null)
                problemNodes.add(problemDescriptionNode)
            problemNodes.addAll(getAllProblemChildren(c))
        }
        return problemNodes
    }
}