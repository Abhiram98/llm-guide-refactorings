package org.boulderse.ijserver.refactoringobjects.movemethod

import com.intellij.java.refactoring.JavaRefactoringBundle
import com.intellij.openapi.ui.Messages
import com.intellij.psi.*
import com.intellij.refactoring.move.MoveCallback
import com.intellij.refactoring.move.moveInstanceMethod.MoveInstanceMethodDialog
import com.intellij.ui.EditorTextField

class MyMoveInstanceMethodDialog(method: PsiMethod,
                                 variables: Array<out PsiVariable>,
                                 val moveCallback: MoveCallback
) : MoveInstanceMethodDialog(method, variables) {
    var triggeredRefactoring = false

//    override fun doAction() {
//        triggeredRefactoring = true
//        super.doAction()
//    }

    override fun doAction() {
        val parameterNames: MutableMap<PsiClass, String> = LinkedHashMap()

        val myThisClassesMapField = this.javaClass.superclass.getDeclaredField("myThisClassesMap")
        myThisClassesMapField.setAccessible(true)
        val myThisClassesMap = myThisClassesMapField.get(this) as Map<PsiClass, Set<PsiMember>>

        val myOldClassParameterNameFieldsField = this.javaClass.superclass.getDeclaredField("myOldClassParameterNameFields")
        myOldClassParameterNameFieldsField.setAccessible(true)
        val myOldClassParameterNameFields = myOldClassParameterNameFieldsField.get(this) as Map<PsiClass, EditorTextField>
        for (aClass in myThisClassesMap.keys) {
            val field = myOldClassParameterNameFields[aClass]
            if (field!!.isEnabled) {
                val parameterName = field.text.trim { it <= ' ' }
                if (!PsiNameHelper.getInstance(myMethod.project).isIdentifier(parameterName)) {
                    Messages
                        .showErrorDialog(
                            project,
                            JavaRefactoringBundle.message("move.method.enter.a.valid.name.for.parameter"),
                            myRefactoringName
                        )
                    return
                }
                parameterNames[aClass] = parameterName
            }
        }

        val targetVariable = myList.selectedValue as PsiVariable ?: return
        val processor = MoveInstanceMethodProcessWithCallBack(
            myMethod.project,
            myMethod, targetVariable,
            myVisibilityPanel.visibility!!,
            isOpenInEditor,
            parameterNames,
            moveCallback
        )
        if (!verifyTargetClass(processor.targetClass)) return
        invokeRefactoring(processor)
    }


}