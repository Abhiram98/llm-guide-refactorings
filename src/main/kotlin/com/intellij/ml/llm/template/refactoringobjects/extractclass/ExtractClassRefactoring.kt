package com.intellij.ml.llm.template.refactoringobjects.extractclass

import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.refactoringobjects.renamevariable.RenameVariableFactory
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiFile
import com.intellij.refactoring.RefactoringFactory
import com.intellij.refactoring.extractSuperclass.ExtractSuperClassProcessor
import com.intellij.refactoring.extractSuperclass.ExtractSuperClassUtil
import com.intellij.refactoring.util.DocCommentPolicy
import com.intellij.refactoring.util.classMembers.MemberInfo

class ExtractClassRefactoring(
    override val startLoc: Int,
    override val endLoc: Int,
    val superClassName: String,
    val classToExtract: PsiClass,
    val members: Array<MemberInfo>

) : AbstractRefactoring() {

    override fun performRefactoring(project: Project, editor: Editor, file: PsiFile) {
        super.performRefactoring(project, editor, file)

        val tempName = "${superClassName}Temp"
        val originalName = classToExtract.name!!
        val processor = ExtractSuperClassProcessor(
            project,
            file.containingDirectory,
            tempName,
            classToExtract,
            members,
            true,
            DocCommentPolicy<PsiComment>(DocCommentPolicy.ASIS)
        )
        processor.run()


        val rename1 = RefactoringFactory.getInstance(project).createRename(classToExtract.superClass!!, superClassName)
        val usages = rename1?.findUsages()
        rename1?.doRefactoring(usages)

        val rename2 = RefactoringFactory.getInstance(project).createRename(classToExtract, originalName)
        val usages2 = rename2?.findUsages()
        rename2?.doRefactoring(usages2)

//        val rename3 = RefactoringFactory.getInstance(project).createRename(classToExtract.superClass!!, superClassName)
//        val usages3 = rename3?.findUsages()
//        rename3?.doRefactoring(usages3)




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
        fun createFromMembers(
            psiClass: PsiClass,
            members: List<String>,
            interaceName: String): ExtractClassRefactoring{
            val fields = psiClass.allFields.filter { it.name in members}
            val methods = psiClass.allMethods.filter { it.name in members }

            return ExtractClassRefactoring(
                1,1,
                interaceName,
                psiClass,
                fields.map { MemberInfo(it) }
                    .union(methods.map { MemberInfo(it) })
                    .toTypedArray()
            )
        }
    }
}