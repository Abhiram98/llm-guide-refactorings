package com.intellij.ml.llm.template.refactoringobjects.pullup

import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiFile
import com.intellij.refactoring.memberPushDown.PushDownProcessor
import com.intellij.refactoring.util.DocCommentPolicy
import com.intellij.refactoring.util.classMembers.MemberInfo

class PushDownRefactoring(
    override val startLoc: Int,
    override val endLoc: Int,
    val sourceClass: PsiClass,
    val members: List<MemberInfo>
) : AbstractRefactoring() {

    override fun performRefactoring(project: Project, editor: Editor, file: PsiFile) {
        super.performRefactoring(project, editor, file)

        val processor = PushDownProcessor(
            sourceClass, members, DocCommentPolicy<PsiComment>(DocCommentPolicy.ASIS),
            false)
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

}