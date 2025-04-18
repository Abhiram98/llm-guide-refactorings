package com.intellij.ml.llm.template.refactoringobjects.change_signature

import ai.grazie.client.common.logging.qualifiedName
import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.server.ChangeSignatureParams
import com.intellij.ml.llm.template.utils.Parameter
import com.intellij.ml.llm.template.utils.PsiUtils
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.psi.impl.source.PsiJavaFileImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.refactoring.changeSignature.ChangeSignatureProcessor
import com.intellij.refactoring.changeSignature.ParameterInfoImpl
import com.jetbrains.rd.generator.nova.PredefinedType
import org.jetbrains.kotlin.idea.base.codeInsight.handlers.fixers.startLine
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments
import kotlin.math.abs

class ChangeSignatureRefactoring(
    override val startLoc: Int,
    override val endLoc: Int,
    val methodToChange: PsiMethod,
    val newModifier: String?,
    val newMethodName: String,
    val newReturnType: PsiType,
    val newParameters: Array<ParameterInfoImpl>
    ) : AbstractRefactoring() {


    override fun performRefactoring(project: Project, editor: Editor, file: PsiFile) {
        val processor = ChangeSignatureProcessor(project,
            methodToChange,
            false,
            newModifier,
            newMethodName,
            newReturnType,
            newParameters
        )
        processor.run()
    }

    override fun isValid(project: Project, editor: Editor, file: PsiFile): Boolean {
        TODO("Not yet implemented")
    }

    override fun getRefactoringPreview(): String {
        TODO("Not yet implemented")
    }

    override fun getStartOffset(): Int {
        TODO("Not yet implemented")
    }

    override fun getEndOffset(): Int {
        TODO("Not yet implemented")
    }

    override fun getReverseRefactoringObject(project: Project, editor: Editor, file: PsiFile): AbstractRefactoring? {
        TODO("Not yet implemented")
    }

    override fun recalibrateRefactoring(project: Project, editor: Editor, file: PsiFile): AbstractRefactoring? {
        TODO("Not yet implemented")
    }


    companion object{
        fun createFromParams(project: Project, editor: Editor, file: PsiFile, changeSignatureParams: ChangeSignatureParams): ChangeSignatureRefactoring{
            val matchingMethods = PsiUtils.getAllMethodNameFromClass(
                (file as PsiJavaFileImpl).classes[0],
                changeSignatureParams.methodName
            )
            if (matchingMethods.isEmpty())
                throw Exception("No method in this class has the name ${changeSignatureParams.methodName}.")

            val methodsSorted = if (matchingMethods.size > 1
                && changeSignatureParams.lineNum!=null){
                matchingMethods.sortedBy { abs(editor.document.getLineNumber(it.startOffsetSkippingComments)-changeSignatureParams.lineNum) }
            }else{
                matchingMethods
            }

            val methodToChange = methodsSorted[0]
            val newModifier = if (methodToChange.modifierList.hasModifierProperty(changeSignatureParams.newSignature.modifier)){
                null
            }else{
                changeSignatureParams.newSignature.modifier
            }
            val newReturnType = if (changeSignatureParams.newSignature.returnType==null){
                methodToChange.returnType
            }
            else if (
                methodToChange.returnType?.presentableText == changeSignatureParams.newSignature.returnType ||
                changeSignatureParams.newSignature.returnType.endsWith(methodToChange.returnType!!.presentableText)
                ){
                methodToChange.returnType
            }else{
                PsiType.getTypeByName(changeSignatureParams.newSignature.returnType, project, GlobalSearchScope.projectScope(project))
            }

            val newParameters =
                changeSignatureParams.newSignature.paramsList.mapIndexed {

                    index: Int, parameter: Parameter ->
                    val finalIndex = if (index>= methodToChange.parameterList.parametersCount){
                        -1
                    }else{
                        index
                    }

                    val newType = if (finalIndex!=-1 && methodToChange.parameterList.parameters[index].type.presentableText==parameter.type){
                        methodToChange.parameterList.parameters[index].type
                    }else{
                        PsiType.getTypeByName(parameter.type, project, GlobalSearchScope.projectScope(project))
                    }

                    if (finalIndex==-1){
                        val default = parameter.defaultValue?:""
                        ParameterInfoImpl(finalIndex, parameter.name, newType, default)
                    }else{
                        ParameterInfoImpl(finalIndex, parameter.name, newType)
                    }

                }.toTypedArray()

            return ChangeSignatureRefactoring(
                1,
                1,
                methodToChange,
                newModifier,
                changeSignatureParams.newSignature.methodName,
                newReturnType!!,
                newParameters
            )
        }
    }
}