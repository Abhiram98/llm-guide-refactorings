package com.intellij.ml.llm.template.refactoringobjects

//abstract class AbstractRefactoring {
//    companion object{
//        fun something(){}
//    }
//
//    abstract fun performRefactoring()
//
//}


interface AbstractRefactoring{

    fun performRefactoring()

    /*
    Return true if the refactoring object can be applied to the code.
     */
    fun isValid(): Boolean

    /*
    Return the total lines of code covered by the refactoring object
     */
    fun sizeLoc(): Int{
        return endLoc-startLoc+1
    }

    /*
    Line numbers where refactoring is to be applied
     */
    val startLoc: Int
    val endLoc: Int

    /*
    Offsets where refactoring is to be applied(based on character index)
     */
//    val startOffset: Int
//    val endOffset: Int

    /*
    A logical name for the refactoring
     */
    fun getRefactoringName(): String
}