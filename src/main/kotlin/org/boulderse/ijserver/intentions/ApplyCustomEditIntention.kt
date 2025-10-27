package org.boulderse.ijserver.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import org.boulderse.ijserver.LLMBundle

class ApplyCustomEditIntention : ApplyTransformationIntention() {
    override fun getInstruction(
        project: Project,
        editor: Editor,
    ): String? = Messages.showInputDialog(project, "Enter prompt:", "Codex", null)

    override fun getText(): String = LLMBundle.message("intentions.apply.custom.edit.name")

    override fun getFamilyName(): String = text
}
