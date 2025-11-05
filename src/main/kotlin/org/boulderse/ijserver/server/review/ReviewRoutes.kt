package org.boulderse.ijserver.server.review

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.json.Json
import org.boulderse.ijserver.refactoringobjects.renamevariable.RenameVariable
import org.boulderse.ijserver.refactoringobjects.renamevariable.formRenameObject
import org.boulderse.ijserver.server.OpenFileParams
import org.boulderse.ijserver.server.RenameParams
import org.boulderse.ijserver.server.RenamesToReviewParams
import org.boulderse.ijserver.server.ReviewScopeParams
import org.boulderse.ijserver.toolwindow.logViewer
import org.boulderse.ijserver.ui.RefactoringSuggestionsPanel

class ReviewRoutes(
    private val routing: Routing,
    private val fileCallBack: () -> PsiFile?,
    private val editorCallBack: () -> Editor?,
    private val projectCallBack: () -> Project,
) {
    fun install() {
        routing.post("review/noop") {
            logViewer.setActionItem("Agent is thinking. Sit back and relax :)")
            call.respond(HttpStatusCode.OK)
        }

        routing.post("/review/renames") {
            // This API expects the renames to be valid. Invalid renames may cause unexpected behavior
            println("Received review for renames")
            val renamesToReview = call.receive<List<RenameParams>>()

            logViewer.setActionItem(
                "ACTION REQUIRED: \n" +
                    "Please review the following renames: \n",
            )
            renamesToReview.forEach { logViewer.appendLog(it.oldName + " -> " + it.newName) }
            val file = fileCallBack()
            val editor = editorCallBack()
            val project = projectCallBack()
            val renameObjs: List<RenameVariable> =
                renamesToReview
                    .map { formRenameObject(it, project, editor!!, file!!) as RenameVariable? }
                    .filterNotNull()
                    .toList()
            renameObjs.forEach {
                it.description =
                    "The rename ${it.oldName} to ${it.newName} should be implemented. The change fits the provided renaming scope."
            }

            val panel =
                RefactoringSuggestionsPanel(
                    project,
                    editor!!,
                    file!!,
                    renameObjs,
                    null,
                    "Accept",
                )

            invokeLater {
                panel.createAndShowPopup(logViewer.renameSuggestions)
            }
            panel.waitAndClose()

            val reviewStatus = renamesToReview.mapIndexed { index, params -> index in panel.completedIndices }
            println("Review status: $reviewStatus")
            call.respond(
                HttpStatusCode.OK,
                message = Json.encodeToString(reviewStatus),
            )
        }

        routing.get("/review/pattern") {
            call.respond(HttpStatusCode.OK, logViewer.getPatternText())
        }

        routing.get("/review/gaurds") {
            call.respond(HttpStatusCode.OK, logViewer.getGuardText())
        }

        routing.post("/review/reset_scope") {
            logViewer.resetScope()
            call.respond(HttpStatusCode.OK)
        }

        routing.post("/review/set_scope") {
            val params = call.receive<ReviewScopeParams>()
            logViewer.setPattern(params.pattern)
            logViewer.setGuard(params.guard)
            call.respond(HttpStatusCode.OK)
        }

        routing.post("/review/scope") {
            val params = call.receive<ReviewScopeParams>()
            logViewer.setPattern(params.pattern)
            logViewer.setGuard(params.guard)

            logViewer.setActionItem(
                "ACTION REQUIRED: \n" +
                    "Please confirm the renaming scope below, by clicking the 'Confirm' button. " +
                    "You are welcome to edit the scope as per your requirements.",
            )
            logViewer.resetConfirmationWait()
            logViewer.waitForConfirmation()

            call.respond(
                HttpStatusCode.OK,
                message = ReviewScopeParams(logViewer.getPatternText(), logViewer.getGuardText()),
            )
        }

        routing.post("/review/add_total_renames") {
            val total = call.receive<RenamesToReviewParams>()
            logViewer.setTotalRenames(total.count)
            call.respond(HttpStatusCode.OK)
        }

        routing.post("/review/add_rename_reviewed") {
            logViewer.incCompletedRenames()
            call.respond(HttpStatusCode.OK)
        }

        routing.post("/review/set_inspecting_file") {
            val params = call.receive<OpenFileParams>()
            logViewer.setFileInspect(params.filePath.split("/").last())
            call.respond(HttpStatusCode.OK)
        }

        routing.post("/review/inc_replication_files"){
            logViewer.incTotalFiles()
            call.respond(HttpStatusCode.OK)
        }

        routing.post("/review/inc_files_inspected"){
            logViewer.incCompletedFiles()
            call.respond(HttpStatusCode.OK)
        }
    }
}
