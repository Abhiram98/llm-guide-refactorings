package com.intellij.ml.llm.template.refactoringobjects

import ai.grazie.utils.isCapitalized
import com.google.gson.annotations.SerializedName
import com.intellij.analysis.AnalysisScope
import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.codeInsight.daemon.impl.JavaReferenceImporter
import com.intellij.codeInspection.ProblemDescriptorBase
import com.intellij.codeInspection.actions.CodeInspectionAction
import com.intellij.codeInspection.ex.GlobalInspectionContextImpl
import com.intellij.codeInspection.ui.InspectionTreeNode
import com.intellij.codeInspection.ui.ProblemDescriptionNode
import com.intellij.ml.llm.template.utils.PsiUtils
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.startOffset
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.idea.gradleTooling.get
import javax.swing.SwingUtilities.invokeAndWait

class IdeInspection(val project: Project, val scope: AnalysisScope, val file: PsiFile, val editor: Editor): CodeInspectionAction(){

    var myGlobalInspectionContext: GlobalInspectionContextImpl? = null
    val problems: MutableList<MyProblem> = mutableListOf()
    val problemDescriptors: MutableList<ProblemDescriptionNode> = mutableListOf()


    @Serializable
    data class MyProblem(

        @SerialName("line_num")
        @SerializedName("line_num")
        val lineNum: Int,

        @SerialName("problem")
        val problem: String
    ){}
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
        var sleepTime = 0
        Thread.sleep(5000)
        if (myGlobalInspectionContext == null)
            return
        while(myGlobalInspectionContext!!.view == null && sleepTime < 60){
            Thread.sleep(1000)
            sleepTime += 1
        }
        if (myGlobalInspectionContext!!.view == null)
            return

        val root = myGlobalInspectionContext!!.view.tree.root
        val errors = getAllProblemChildren(root).filter {
            val level = it.javaClass.getDeclaredField("myLevel")
            level.isAccessible = true
            (level.get(it) as HighlightDisplayLevel) == HighlightDisplayLevel.ERROR
        }
        problemDescriptors.addAll(errors)
        val descriptions = errors.map{
            val desc = (it.descriptor as? ProblemDescriptorBase)
            MyProblem(desc?.lineNumber?.plus(1)?:0, it.toString())
        }
        problems.addAll(descriptions)

        println("got the view!")
    }

    fun fixIssues(){
        problems.removeIf { true } // remove all elements
        problemDescriptors.map {
            val descriptor = it.descriptor
            if (descriptor!=null && descriptor.fixes?.isNotEmpty() ?: false){
                invokeAndWait{
                    CommandProcessor.getInstance().runUndoTransparentAction {
                        descriptor.fixes!![0].applyFix(project, descriptor) // apply fix.
                    }
                }
            }
            else{
                if ("Cannot resolve symbol" in it.toString()){
                    // attempt to resolve import
                    val typeName = it.toString()
                        .split("Cannot resolve symbol ")
                        .last()
                        .removePrefix("'")
                        .removeSuffix("'")
                    if (typeName[0].isUpperCase()) { // it's likely a class and we can try to import it.
                        val desc = (descriptor as? ProblemDescriptorBase)
                        val vars = runReadAction {
                            PsiUtils.getElementsOfTypeOnLine(
                                file,
                                editor,
                                desc?.lineNumber?.plus(1) ?: 1,
                                PsiElement::class.java
                            )
                                .filter { it.text == typeName }
                        }
                        if (vars.isNotEmpty()) {
                            val importer = runReadAction {
                                JavaReferenceImporter().computeAutoImportAtOffset(
                                    editor,
                                    file,
                                    vars[0].startOffset,
                                    false
                                )
                            }
                            try {
                                invokeAndWait { println("Import status: " + importer.asBoolean) }
                            } catch (e: Exception) {
                                print("import failed? not sure.")
                            }
                            return@map
                        }
                    }
                }
                val desc = (it.descriptor as? ProblemDescriptorBase)
//                problems.add("Error on line ${desc?.lineNumber}: ${it}")
                problems.add(MyProblem(desc?.lineNumber?:0, it.toString()))
            }
        }
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

    companion object {
        fun importFromRange(textRange: TextRange, editor: Editor, file: PsiFile) {
            val importer = runReadAction {
                JavaReferenceImporter().computeAutoImportAtOffset(
                    editor,
                    file,
                    textRange.startOffset + 1,
                    false
                )
            }
            try {
                invokeAndWait { println("Import status: " + importer.asBoolean) }
            } catch (e: Exception) {
                print("import failed? not sure.")
            }
        }
    }
}