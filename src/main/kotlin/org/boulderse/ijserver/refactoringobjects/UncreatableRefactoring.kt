package org.boulderse.ijserver.refactoringobjects

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class UncreatableRefactoring(
    override val startLoc: Int,
    override val endLoc: Int,
    val suggestionType: String
) : AbstractRefactoring() {
    init {
        isValid = false
    }
    override fun performRefactoring(project: Project, editor: Editor, file: PsiFile) {
        // do nothing.
        super.performRefactoring(project, editor, file)
    }

    override fun isValid(project: Project, editor: Editor, file: PsiFile): Boolean {
        isValid=false
        return false
    }

    override fun getRefactoringPreview(): String {
        return "Couldn't create refactoring object of $suggestionType"
    }

    override fun getStartOffset(): Int {
        return 0
    }

    override fun getEndOffset(): Int {
        return 0
    }

    override fun getReverseRefactoringObject(project: Project, editor: Editor, file: PsiFile): AbstractRefactoring? {
        return null
    }

    override fun recalibrateRefactoring(project: Project, editor: Editor, file: PsiFile): AbstractRefactoring? {
        return this
    }
}