package org.boulderse.ijserver.server.review

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.openapi.editor.Editor
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import kotlinx.serialization.json.Json
import org.boulderse.ijserver.refactoringobjects.renamevariable.RenameVariable
import org.boulderse.ijserver.refactoringobjects.renamevariable.formRenameObject
import org.boulderse.ijserver.server.RenameParams
import org.boulderse.ijserver.ui.RefactoringSuggestionsPanel


class ReviewRoutes(private val routing: Routing,
                   private val fileCallBack: () -> PsiFile?,
                   private val editorCallBack: () -> Editor?,
                   private val projectCallBack: () -> Project
) {
    fun install() {
         routing.post("/review/renames") {
             // This API expects the renames to be valid. Invalid renames may cause unexpected behavior
             println("Received review")
             val renamesToReview = call.receive<List<RenameParams>>()
             val file = fileCallBack()
             val editor = editorCallBack()
             val project = projectCallBack()
             val renameObjs: List<RenameVariable> = renamesToReview
                 .map{ formRenameObject(it, project, editor!!, file!!) as RenameVariable? }
                 .filterNotNull().toList()

             val panel = RefactoringSuggestionsPanel(
                 project,
                 editor!!,
                 file!!,
                 renameObjs,
                 null,
                 "Rename Identifier"
             )

             invokeLater {
                 panel.createAndShowPopup()
             }
             panel.waitForClose()

             val reviewStatus = renamesToReview.mapIndexed { index, params -> index in panel.completedIndices }
             call.respond(HttpStatusCode.OK,
                 message = Json.encodeToString(reviewStatus))
         }
    }
}