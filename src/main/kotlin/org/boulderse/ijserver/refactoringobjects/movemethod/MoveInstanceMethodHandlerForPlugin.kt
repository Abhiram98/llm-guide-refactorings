package org.boulderse.ijserver.refactoringobjects.movemethod

import com.intellij.java.refactoring.JavaRefactoringBundle
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.NlsContexts.DialogMessage
import com.intellij.openapi.util.NlsContexts.DialogTitle
import com.intellij.psi.*
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.codeStyle.VariableKind
import com.intellij.psi.impl.source.jsp.jspJava.JspClass
import com.intellij.psi.search.searches.OverridingMethodsSearch
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.psi.util.PsiUtil
import com.intellij.refactoring.HelpID
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.makeStatic.MakeStaticHandler
import com.intellij.refactoring.move.MoveInstanceMembersUtil
import com.intellij.refactoring.move.moveInstanceMethod.MoveInstanceMethodDialog
import com.intellij.refactoring.move.moveInstanceMethod.MoveInstanceMethodHandler
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.util.containers.ContainerUtil

class MoveInstanceMethodHandlerForPlugin : MoveInstanceMethodHandler() {
    private val LOG =
        Logger.getInstance(
            MoveInstanceMethodHandler::class.java,
        )

    public val suitableVariablesToMove = mutableListOf<PsiVariable>()

    override fun invoke(
        project: Project,
        elements: Array<PsiElement>,
        dataContext: DataContext?,
    ) {
        if (elements.size != 1 || elements[0] !is PsiMethod) return
        val method = elements[0] as PsiMethod
        var message: String? = null
        if (!method.getManager().isInProject(method)) {
            message = JavaRefactoringBundle.message("move.method.is.not.supported.for.non.project.methods")
        } else if (method.isConstructor()) {
            message = JavaRefactoringBundle.message("move.method.is.not.supported.for.constructors")
        } else if (method.getLanguage() !== JavaLanguage.INSTANCE) {
            message =
                JavaRefactoringBundle.message(
                    "move.method.is.not.supported.for.0",
                    method.getLanguage().getDisplayName(),
                )
        } else {
            val containingClass: PsiClass? = method.getContainingClass()
            if (containingClass != null && mentionTypeParameters(method)) {
                message = JavaRefactoringBundle.message("move.method.is.not.supported.for.generic.classes")
            } else if (method.findSuperMethods().size > 0 ||
                OverridingMethodsSearch.search(method).toArray(PsiMethod.EMPTY_ARRAY).size > 0
            ) {
                message =
                    RefactoringBundle.message("move.method.is.not.supported.when.method.is.part.of.inheritance.hierarchy")
            } else {
                val classes: Set<PsiClass> = MoveInstanceMembersUtil.getThisClassesToMembers(method).keys
                for (aClass in classes) {
                    if (aClass is JspClass) {
                        message = JavaRefactoringBundle.message("synthetic.jsp.class.is.referenced.in.the.method")
//                        val editor = CommonDataKeys.EDITOR.getData(dataContext!!)
//                        CommonRefactoringUtil.showErrorHint(
//                            project,
//                            editor,
//                            message,
//                            getRefactoringName()!!,
//                            HelpID.MOVE_INSTANCE_METHOD
//                        )
                        break
                    }
                }
            }
        }
        if (message != null) {
//            showErrorHint(project, dataContext, message)
            return
        }

        val suitableVariables: MutableList<PsiVariable> = ArrayList()
        message = collectSuitableVariables(method, suitableVariables)
        if (message != null) {
            return
        }

        suitableVariablesToMove.addAll(suitableVariables)
    }

    private fun showErrorHint(
        project: Project,
        dataContext: DataContext?,
        message: @DialogMessage String?,
    ) {
        val editor = if (dataContext == null) null else CommonDataKeys.EDITOR.getData(dataContext)
        CommonRefactoringUtil.showErrorHint(
            project,
            editor,
            RefactoringBundle.getCannotRefactorMessage(message),
            getRefactoringName()!!,
            HelpID.MOVE_INSTANCE_METHOD,
        )
    }

    private fun collectSuitableVariables(
        method: PsiMethod,
        suitableVariables: MutableList<in PsiVariable>,
    ): @DialogMessage String? {
        val allVariables = mutableListOf<PsiVariable>()
        allVariables.addAll(method.parameterList.parameters)
        allVariables.addAll(method.containingClass!!.fields)
//        ContainerUtil.addAll<PsiParameter, List<PsiVariable>>(allVariables, *method.parameterList.parameters)
//        ContainerUtil.addAll<PsiField, List<PsiVariable>>(
//            allVariables, *method.containingClass!!
//                .fields
//        )
        var classTypesFound = false
        var resolvableClassesFound = false
        var classesInProjectFound = false
        for (variable in allVariables) {
            val type = variable.type
            if (type is PsiClassType && !type.hasParameters()) {
                classTypesFound = true
                val psiClass = type.resolve()
                if (psiClass != null && psiClass !is PsiTypeParameter) {
                    resolvableClassesFound = true
                    val inProject = method.manager.isInProject(psiClass)
                    if (inProject) {
                        classesInProjectFound = true
                        suitableVariables.add(variable)
                    }
                }
            }
        }

        if (suitableVariables.isEmpty()) {
            if (!classTypesFound) {
                return JavaRefactoringBundle.message("there.are.no.variables.that.have.reference.type")
            } else if (!resolvableClassesFound) {
                return JavaRefactoringBundle.message("all.candidate.variables.have.unknown.types")
            } else if (!classesInProjectFound) {
                return JavaRefactoringBundle.message("all.candidate.variables.have.types.not.in.project")
            }
        }
        return null
    }

//
//    fun suggestParameterNameForThisClass(thisClass: PsiClass): String {
//        val manager = thisClass.manager
//        val type: PsiType = JavaPsiFacade.getElementFactory(manager.project).createType(thisClass)
//        val suggestedNameInfo = JavaCodeStyleManager.getInstance(manager.project)
//            .suggestVariableName(VariableKind.PARAMETER, null, null, type)
//        return if (suggestedNameInfo.names.size > 0) suggestedNameInfo.names[0] else ""
//    }
//
//    fun suggestParameterNames(method: PsiMethod?, targetVariable: PsiVariable?): Map<PsiClass, String> {
//        val classesToMembers = MoveInstanceMembersUtil.getThisClassesToMembers(method)
//        val result: MutableMap<PsiClass, String> = LinkedHashMap()
//        for ((aClass, members) in classesToMembers) {
//            if (members.size == 1 && members.contains(targetVariable)) continue
//            result[aClass] = suggestParameterNameForThisClass(aClass)
//        }
//        return result
//    }
//
    private fun mentionTypeParameters(method: PsiMethod): Boolean {
        val containingClass = method.containingClass ?: return false
        val typeParameters: Set<PsiTypeParameter> =
            ContainerUtil.newHashSet(PsiUtil.typeParametersIterable(containingClass))
        for (parameter in method.parameterList.parameters) {
            if (PsiTypesUtil.mentionsTypeParameters(parameter.type, typeParameters)) return true
        }
        return PsiTypesUtil.mentionsTypeParameters(method.returnType, typeParameters)
    }

//
    fun getRefactoringName(): @DialogTitle String? = RefactoringBundle.message("move.instance.method.title")
}
