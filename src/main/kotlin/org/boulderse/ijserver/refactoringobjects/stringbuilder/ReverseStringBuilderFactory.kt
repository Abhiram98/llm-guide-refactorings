package org.boulderse.ijserver.refactoringobjects.stringbuilder

import org.boulderse.ijserver.refactoringobjects.AbstractRefactoring
import org.boulderse.ijserver.refactoringobjects.MyRefactoringFactory
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.siyeh.ig.style.StringBufferReplaceableByStringInspection
import org.jetbrains.kotlin.idea.inspections.ReplaceWithStringBuilderAppendRangeInspection

class ReverseStringBuilderFactory {

    companion object: MyRefactoringFactory{
        override fun createObjectsFromFuncCall(
            funcCall: String,
            project: Project,
            editor: Editor,
            file: PsiFile
        ): List<AbstractRefactoring> {
            TODO("Not yet implemented")
            val inspection = StringBufferReplaceableByStringInspection()
        }

        override val logicalName: String
            get() = TODO("Not yet implemented")
        override val apiFunctionName: String
            get() = TODO("Not yet implemented")
        override val APIDocumentation: String
            get() = TODO("Not yet implemented")

    }

    class ReverseStringBuilder(override val startLoc: Int, override val endLoc: Int) : AbstractRefactoring() {
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

        override fun getReverseRefactoringObject(
            project: Project,
            editor: Editor,
            file: PsiFile
        ): AbstractRefactoring? {

            TODO("Not yet implemented")
        }

        override fun recalibrateRefactoring(project: Project, editor: Editor, file: PsiFile): AbstractRefactoring? {
            TODO("Not yet implemented")
        }

    }

}