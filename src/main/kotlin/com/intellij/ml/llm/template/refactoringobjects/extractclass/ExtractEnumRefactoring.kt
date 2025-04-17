package com.intellij.ml.llm.template.refactoringobjects.extractclass

import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.refactoring.MoveDestination
import com.intellij.refactoring.PackageWrapper
import com.intellij.refactoring.RefactoringFactory
import com.intellij.refactoring.extractSuperclass.ExtractSuperClassProcessor
import com.intellij.refactoring.extractclass.ExtractClassProcessor
import com.intellij.refactoring.extractclass.ExtractEnumProcessor
import com.intellij.refactoring.move.moveClassesOrPackages.MultipleRootsMoveDestination
import com.intellij.refactoring.util.DocCommentPolicy
import com.intellij.refactoring.util.classMembers.MemberInfo

class ExtractEnumRefactoring(
    override val startLoc: Int,
    override val endLoc: Int,
    val className: String,
    val classToExtract: PsiClass,
    val fields: List<PsiField>,
) : AbstractRefactoring() {

    override fun performRefactoring(project: Project, editor: Editor, file: PsiFile) {
        super.performRefactoring(project, editor, file)
        val packageName = classToExtract.qualifiedName!!.removeSuffix(".${classToExtract.name}")
        val javaFile = classToExtract.containingFile as PsiJavaFile
        val psiPackage = JavaPsiFacade.getInstance(project)
            .findPackage(javaFile.packageName)!!

        val processor = ExtractClassProcessor(
            classToExtract,
            fields,
            emptyList(),
            emptyList(),
            packageName,
            MultipleRootsMoveDestination(PackageWrapper(psiPackage)),
            className,
            "public",
            false,
            fields.map {
                val v = MemberInfo(it)
                v },
            false
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
        fun createFromMembers(
            psiClass: PsiClass,
            members: List<String>,
            className: String,
//            subClassName: String
        ): ExtractEnumRefactoring{

            if (psiClass.interfaces.filter { it.name == className }.isNotEmpty() || psiClass.superClass?.name==className){
                throw Exception("${psiClass.name} already implements the $className interface. " +
                        "If you would like to move members into the interface, try performing a pull-up refactoring")
            }
            // TODO: Search for class with the same name as `className`, in the same package.

            val fields = psiClass.allFields.filter { it.name in members}

            return ExtractEnumRefactoring(
                1,1,
                className,
                psiClass,
                fields
            )
        }
    }
}