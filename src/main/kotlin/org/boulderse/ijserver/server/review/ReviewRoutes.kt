package org.boulderse.ijserver.server.review

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
import org.boulderse.ijserver.server.RenameParams
import org.boulderse.ijserver.telemetry.EFTelemetryDataManager
import org.boulderse.ijserver.ui.showRefactoringOptionsPopup
import org.boulderse.ijserver.utils.CodeTransformer


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
             // todo: create the refactoring objects.
             val renameObjs: List<RenameVariable> = renamesToReview.map{ null }.filterNotNull().toList()
             val file = fileCallBack()
             val editor = editorCallBack()
             val project = projectCallBack()

             showRefactoringOptionsPopup(
                 project,
                 editor!!,
                 file!!,
                 renameObjs,
                 CodeTransformer(),
                 EFTelemetryDataManager(),
                 "Rename"
             )

             val reviewStatus = renamesToReview.map { true }
             call.respond(HttpStatusCode.OK,
                 message = Json.encodeToString(reviewStatus))
         }
    }
}