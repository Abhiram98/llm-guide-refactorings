package com.intellij.ml.llm.template.intentions

import com.intellij.ml.llm.template.server.RefactoringServer
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class StartServerActivity: ProjectActivity {
    override suspend fun execute(project: Project) {
        print("Starting server")
        RefactoringServer(project).start()
    }
}