package org.boulderse.ijserver.refactoringobjects.pullup

import org.boulderse.ijserver.refactoringobjects.AbstractRefactoring
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiFile
import com.intellij.refactoring.memberPullUp.PullUpProcessor
import com.intellij.refactoring.util.DocCommentPolicy
import com.intellij.refactoring.util.classMembers.MemberInfo

class PullUpRefactoring(
    override val startLoc: Int,
    override val endLoc: Int,
    val sourceClass: PsiClass,
    val targetClass: PsiClass,
    val members: List<MemberInfo>
) : AbstractRefactoring() {

    override fun performRefactoring(project: Project, editor: Editor, file: PsiFile) {
        super.performRefactoring(project, editor, file)

        val processor = PullUpProcessor(sourceClass, targetClass,
            members.toTypedArray(), DocCommentPolicy(DocCommentPolicy.ASIS))
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

    companion object {
        fun fromMembers(sourceClass: PsiClass, targetClass: PsiClass, members:List<String>, keepAbstract: Boolean): PullUpRefactoring{
            val fields = sourceClass.allFields.filter { it.name in members}
            val methods = sourceClass.allMethods.filter { it.name in members }
            val refObj = PullUpRefactoring(
                1, 1,
                sourceClass,
                targetClass,
                fields.map {
                    val m = MemberInfo(it)
                    m.isToAbstract = keepAbstract
                    m
                }.union(methods.map {
                        val m = MemberInfo(it)
                        m.isToAbstract = keepAbstract
                        m
                    }).toList())
            return refObj
        }
    }

}