package com.intellij.ml.llm.template.refactoringobjects.extractclass

import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.refactoringobjects.MyRefactoringFactory
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiFile
import com.intellij.refactoring.RefactoringFactory
import com.intellij.refactoring.extractInterface.ExtractInterfaceProcessor
import com.intellij.refactoring.util.DocCommentPolicy
import com.intellij.refactoring.util.classMembers.MemberInfo

class ExtractInterfaceRefactoring(
    override val startLoc: Int,
    override val endLoc: Int,
    val interfaceName: String,
    val subClassName: String,
    val classToExtract: PsiClass,
    val members: Array<MemberInfo>
) : AbstractRefactoring() {


    override fun performRefactoring(project: Project, editor: Editor, file: PsiFile) {
        val originalName = classToExtract.name!!
//        val tempName = if (interfaceName == originalName) interfaceName else "${interfaceName}Temp"
        val tempName = "${interfaceName}Temp"

        val processor = ExtractInterfaceProcessor(
            project,
            true,
            file.containingDirectory,
            tempName,
            classToExtract,
            members,
            DocCommentPolicy<PsiComment>(DocCommentPolicy.ASIS)
        )
        processor.run()


        val matchingInterface = classToExtract.interfaces.filter { it.name==originalName }.first()
        val rename1 = RefactoringFactory.getInstance(project).createRename(matchingInterface, interfaceName)
        val usages = rename1?.findUsages()
        rename1?.doRefactoring(usages)

        val rename2 = RefactoringFactory.getInstance(project).createRename(classToExtract, subClassName)
        val usages2 = rename2?.findUsages()
        rename2?.doRefactoring(usages2)


        super.performRefactoring(project, editor, file)
    }

    override fun isValid(project: Project, editor: Editor, file: PsiFile): Boolean {
        return true
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

    companion object: MyRefactoringFactory{

        fun createFromMembers(
            psiClass: PsiClass,
            members: List<String>,
            interaceName: String,
            subClassName: String
        ): ExtractInterfaceRefactoring{
            val fields = psiClass.allFields.filter { it.name in members}
            val methods = psiClass.allMethods.filter { it.name in members }

            return ExtractInterfaceRefactoring(
                1,1,
                interaceName,
                subClassName,
                psiClass,
                fields.map { MemberInfo(it) }
                    .union(methods.map { MemberInfo(it) })
                    .toTypedArray()
            )
        }



        override fun createObjectsFromFuncCall(
            funcCall: String,
            project: Project,
            editor: Editor,
            file: PsiFile
        ): List<AbstractRefactoring> {
            TODO("Not yet implemented")
        }

        override val logicalName: String
            get() = "Extract Interface"
        override val apiFunctionName: String
            get() = "extract interface"
        override val APIDocumentation: String
            get() = TODO("Not yet implemented")

    }
}