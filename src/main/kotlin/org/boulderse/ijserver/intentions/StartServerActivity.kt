package org.boulderse.ijserver.intentions

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.boulderse.ijserver.server.RefactoringServer

class StartServerActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        print("Starting server")
        // create an instance of the refactoring server
        // and point it to the right project.
        RefactoringServer.getInstance(project)
    }
}
