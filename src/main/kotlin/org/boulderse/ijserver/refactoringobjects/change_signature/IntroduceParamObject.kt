package org.boulderse.ijserver.refactoringobjects.change_signature

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiJavaFileImpl
import com.intellij.refactoring.PackageWrapper
import com.intellij.refactoring.changeSignature.ParameterInfoImpl
import com.intellij.refactoring.introduceParameterObject.IntroduceParameterObjectProcessor
import com.intellij.refactoring.introduceparameterobject.JavaIntroduceParameterObjectClassDescriptor
import com.intellij.refactoring.move.moveClassesOrPackages.MultipleRootsMoveDestination
import org.boulderse.ijserver.refactoringobjects.AbstractRefactoring
import org.boulderse.ijserver.server.IntroduceParamObjectParams
import org.boulderse.ijserver.utils.PsiUtils
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments
import kotlin.math.abs

class IntroduceParamObject(
    override val startLoc: Int,
    override val endLoc: Int,
    val methodToChange: PsiMethod,
    val newClassName: String,
    val psiPackage: PsiPackage,
    val paramsToAbstract: List<ParameterInfoImpl>,
) : AbstractRefactoring(methodToChange) {
    override fun performRefactoring(
        project: Project,
        editor: Editor,
        file: PsiFile,
    ) {
        val useExisting = psiPackage.classes.filter { it.name == newClassName }.isNotEmpty()

        val processor =
            IntroduceParameterObjectProcessor(
                methodToChange,
                JavaIntroduceParameterObjectClassDescriptor(
                    newClassName,
                    psiPackage.qualifiedName,
                    MultipleRootsMoveDestination(PackageWrapper(psiPackage)),
                    useExisting,
                    false,
                    null,
                    paramsToAbstract.toTypedArray(),
                    methodToChange,
                    true,
                ),
                methodToChange.parameterList.parameters
                    .mapIndexed { index: Int, psiParameter: PsiParameter ->
                        ParameterInfoImpl(index, psiParameter.name, psiParameter.type)
                    },
                false,
            )
        processor.run()
    }

    override fun isValid(
        project: Project,
        editor: Editor,
        file: PsiFile,
    ): Boolean {
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

    override fun getReverseRefactoringObject(
        project: Project,
        editor: Editor,
        file: PsiFile,
    ): AbstractRefactoring? {
        TODO("Not yet implemented")
    }

    override fun recalibrateRefactoring(
        project: Project,
        editor: Editor,
        file: PsiFile,
    ): AbstractRefactoring? {
        TODO("Not yet implemented")
    }

    companion object {
        fun createFromParams(
            params: IntroduceParamObjectParams,
            file: PsiFile,
            editor: Editor,
            project: Project,
        ): IntroduceParamObject {
            val matchingMethods =
                PsiUtils.getAllMethodNameFromClass(
                    (file as PsiJavaFileImpl).classes[0],
                    params.methodName,
                )
            if (matchingMethods.isEmpty()) {
                throw Exception("No method in this class has the name ${params.methodName}.")
            }

            val methodsSorted =
                if (matchingMethods.size > 1 &&
                    params.lineNum != null
                ) {
                    matchingMethods.sortedBy { abs(editor.document.getLineNumber(it.startOffsetSkippingComments) - params.lineNum) }
                } else {
                    matchingMethods
                }

            val methodToChange = methodsSorted[0]

            val paramsToAbstract =
                methodToChange.parameterList.parameters
                    .filter { it.name in params.paramNames }
                    .mapIndexed { index, param ->
                        ParameterInfoImpl(index, param.name, param.type)
                    }
            val javaFile = file as PsiJavaFile
            val psiPackage =
                JavaPsiFacade
                    .getInstance(project)
                    .findPackage(javaFile.packageName)!!

            return IntroduceParamObject(
                1,
                1,
                methodToChange,
                params.newClassName,
                psiPackage,
                paramsToAbstract,
            )
        }
    }
}
