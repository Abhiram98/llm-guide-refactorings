package org.boulderse.ijserver.refactoringobjects

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

//abstract class AbstractRefactoring {
//    companion object{
//        fun something(){}
//    }
//
//    abstract fun performRefactoring()
//
//}


abstract class AbstractRefactoring{
    var description: String = ""
    var applied: Boolean = false
    var isValid: Boolean? = null
    var undone: Boolean = false
    var reverseRefactoring: AbstractRefactoring? = null
    var userRating: String? = null
    var startedRefactoringFlow = false

//    companion object{
//        internal fun f(): Int{
//            return 1
//        }
//    }
    open fun performRefactoring(project: Project, editor: Editor, file: PsiFile){
        applied = true
        startedRefactoringFlow = true
    }


    /*
    Return true if the refactoring object can be applied to the code.
     */
    abstract fun isValid(project: Project, editor: Editor, file: PsiFile): Boolean

    /*
    Return the total lines of code covered by the refactoring object
     */
    fun sizeLoc(): Int{
        return endLoc-startLoc+1
    }

    /*
    Line numbers where refactoring is to be applied
     */
    abstract val startLoc: Int
    abstract val endLoc: Int

    /*
    Offsets where refactoring is to be applied(based on character index)
     */
//    val startOffset: Int
//    val endOffset: Int

    /*
    A logical name for the refactoring
     */
    abstract fun getRefactoringPreview(): String


    abstract fun getStartOffset(): Int

    abstract fun getEndOffset(): Int

    abstract fun getReverseRefactoringObject(
        project: Project, editor: Editor, file: PsiFile): AbstractRefactoring?

    abstract fun recalibrateRefactoring(
        project: Project, editor: Editor, file: PsiFile
    ): AbstractRefactoring?

    fun recalibrateFromPsiElement(psiElement: PsiElement){


    }

    fun recalibrateFromLineNumber(){}


}