package org.boulderse.ijserver.server.sourcecode

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import org.boulderse.ijserver.refactoringobjects.renamevariable.RenameVariable
import org.boulderse.ijserver.refactoringobjects.renamevariable.formRenameObject
import org.boulderse.ijserver.refactoringobjects.snippet.SnippetFinder
import org.boulderse.ijserver.server.CountIdentsParams
import org.boulderse.ijserver.server.OpenFileParams
import org.boulderse.ijserver.server.RenameParams
import org.boulderse.ijserver.server.SnippetFinderParams
import org.boulderse.ijserver.utils.PsiUtils

class SourceCodeRoutes(
    private val routing: Routing,
    private val fileCallBack: () -> PsiFile?,
    private val editorCallBack: () -> Editor?,
    private val projectCallBack: () -> Project,
) {
    fun install() {
        routing.get("/src/get_code") {
            val params = call.receive<OpenFileParams>()
            val project = projectCallBack()
            val vfile = LocalFileSystem.getInstance().refreshAndFindFileByPath(project.basePath + "/" + params.filePath)
            if (vfile == null) {
                call.respond(HttpStatusCode.BadRequest, message = "file not found.")
                return@get
            }
            val file = runReadAction { PsiManager.getInstance(project).findFile(vfile) }!!
            call.respond(HttpStatusCode.OK, message = file.text)
        }

        routing.get("/get_source_code") {
            call.respond(HttpStatusCode.OK, message = fileCallBack()!!.text)
        }

        routing.post("/get_source_code_snippet") {
            val params = call.receive<SnippetFinderParams>()
            val project = projectCallBack()
            val file = fileCallBack()
            val editor = editorCallBack()
            val matchedFile =
                if (params.filePath != null) {
                    val foundVfile =
                        LocalFileSystem
                            .getInstance()
                            .refreshAndFindFileByPath(project.basePath + "/" + params.filePath)!!
                    runReadAction { PsiManager.getInstance(project).findFile(foundVfile)!! }
                } else {
                    file!!
                }

            if (params.codeElementType == "file") {
                call.respond(HttpStatusCode.OK, message = matchedFile.text)
                return@post
            }

            val match =
                formRenameObject(
                    RenameParams(
                        oldName = params.name,
                        newName = params.name + "X",
                        lineNum = params.lineNum,
                        codeElementType = params.codeElementType,
                    ),
                    project,
                    editor!!,
                    matchedFile ?: file!!,
                )
            if (match != null) {
                call.respond(
                    HttpStatusCode.OK,
                    message =
                        SnippetFinder(
                            file = matchedFile,
                            psiElement = runReadAction { (match as RenameVariable).getResolvedElement() ?: match.oldVarPsi },
                        ).getSnippet(),
                )
                return@post
            }
            call.respond(HttpStatusCode.BadRequest)
        }

        routing.post("/count_identifiers") {
            call.respond(
                HttpStatusCode.OK,
                message = runReadAction { PsiUtils.countIdentifiers(fileCallBack()!!, null) }.toString(),
            )
        }

        routing.post("/count_identifiers_keyword") {
            val params = call.receive<CountIdentsParams>()
            call.respond(
                HttpStatusCode.OK,
                message = runReadAction { PsiUtils.countIdentifiers(fileCallBack()!!, params.keyword) }.toString(),
            )
        }
    }
}
