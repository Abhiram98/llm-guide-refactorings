package org.boulderse.ijserver.refactoringobjects.pullup

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.refactoring.memberPushDown.PushDownProcessor
import com.intellij.refactoring.util.DocCommentPolicy
import com.intellij.refactoring.util.classMembers.MemberInfo
import org.boulderse.ijserver.refactoringobjects.AbstractRefactoring

class PushDownRefactoring(
    override val startLoc: Int,
    override val endLoc: Int,
    val sourceClass: PsiClass,
    val members: List<MemberInfo>,
) : AbstractRefactoring(null) {
    override fun performRefactoring(
        project: Project,
        editor: Editor,
        file: PsiFile,
    ) {
        super.performRefactoring(project, editor, file)

        val processor =
            PushDownProcessor(
                sourceClass,
                members,
                DocCommentPolicy(DocCommentPolicy.ASIS),
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
        fun fromMembers(
            psiClass: PsiClass,
            members: List<String>,
            keepAbstract: Boolean,
        ): PushDownRefactoring {
            val fields = psiClass.allFields.filter { it.name in members }
            val methods = psiClass.allMethods.filter { it.name in members }
            val refObj =
                PushDownRefactoring(
                    1,
                    1,
                    psiClass,
                    fields
                        .map {
                            val m = MemberInfo(it)
                            m.isToAbstract = keepAbstract
                            m
                        }.union(
                            methods.map {
                                val m = MemberInfo(it)
                                m.isToAbstract = keepAbstract
                                m
                            },
                        ).toList(),
                )
            return refObj
        }
    }
}
