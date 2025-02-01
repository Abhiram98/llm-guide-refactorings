package com.intellij.ml.llm.template.server

import com.intellij.ml.llm.template.refactoringobjects.renamevariable.RenameVariable
import com.intellij.ml.llm.template.refactoringobjects.renamevariable.RenameVariableFactory
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.serialization.*
import io.ktor.server.response.*
import io.ktor.server.engine.*
import io.ktor.server.request.*
import kotlinx.serialization.Serializable
import io.ktor.serialization.*
import io.ktor.server.request.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.*
import io.ktor.server.routing.*

class RefactoringServer(val project: Project, val editor: Editor, val file: PsiFile) {

    @Serializable
    data class RenameParams(
        val oldName: String,
        val newName: String
    )

    fun start(){
        println("Starting refactoring server")
        embeddedServer(Netty, 8082) {
            myApplicationModule()
        }.start(wait = true)
    }

    fun Application.myApplicationModule() {
        install(ContentNegotiation) {
            json()
        }
        routing {
            get("/") {
                call.respondText("Hello, world!", ContentType.Text.Html)
            }
            post("/rename") {
                println("got a request")
                try {
                    val params = call.receive<RenameParams>()
                    println("renaming ${params.oldName} -> ${params.newName}")

                    // Call IJ rename API here.
                    val renameObject = RenameVariableFactory.fromOldNewName(
                        project, editor, file, params.oldName, params.newName)
                    if (renameObject.isNotEmpty())
                        invokeLater { renameObject[0].performRefactoring(project, editor, file) }

                    call.respond(HttpStatusCode.NoContent)
                } catch (ex: IllegalStateException) {
                    print("failed to refactor")
                    call.respond(HttpStatusCode.BadRequest)
                } catch (ex: JsonConvertException) {
                    print("failed")
                    call.respond(HttpStatusCode.BadRequest)
                }
            }
        }
    }

    fun stop(){
        println("Stopping refactoring server.")
    }

}