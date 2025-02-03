package com.intellij.ml.llm.template.server

import com.intellij.ml.llm.template.refactoringobjects.extractfunction.ExtractMethodFactory
import com.intellij.ml.llm.template.refactoringobjects.movemethod.MoveMethodFactory
import com.intellij.ml.llm.template.refactoringobjects.renamevariable.RenameVariable
import com.intellij.ml.llm.template.refactoringobjects.renamevariable.RenameVariableFactory
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
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
import javax.swing.SwingUtilities.invokeAndWait

class RefactoringServer(val project: Project, var editor: Editor, var file: PsiFile) {

    @Serializable
    data class RenameParams(
        val oldName: String,
        val newName: String
    )

    @Serializable
    data class ExtractMethodParams(
        val startLine: Int,
        val endLine: Int,
        val newName: String
    )

    @Serializable
    data class MoveMethodParams(
        val methodName: String,
        val methodSignature: String?,
        val targetClass: String
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
                        invokeAndWait { renameObject[0].performRefactoring(project, editor, file) }

                    call.respond(HttpStatusCode.NoContent)
                } catch (ex: IllegalStateException) {
                    print("failed to refactor")
                    call.respond(HttpStatusCode.BadRequest)
                } catch (ex: JsonConvertException) {
                    print("failed")
                    call.respond(HttpStatusCode.BadRequest)
                }
            }
            post("/extract-method"){
                try {
                    val params = call.receive<ExtractMethodParams>()
                    println("extracting lines ${params.startLine} -> ${params.endLine}: ${params.newName}")

                    // Call IJ rename API here.
                    val refObjs = ExtractMethodFactory.fromStartEndLine(editor, file, params.startLine, params.endLine, params.newName)
                    if (refObjs.isNotEmpty()) {
                        invokeAndWait{ refObjs[0].performRefactoring(project, editor, file) }
                    }

                    call.respond(HttpStatusCode.NoContent)
                } catch (ex: IllegalStateException) {
                    call.respond(HttpStatusCode.BadRequest)
                    ex.printStackTrace()
                } catch (ex: JsonConvertException) {
                    call.respond(HttpStatusCode.BadRequest)
                    ex.printStackTrace()
                }
            }
            post("/move-method"){
                try {
                    val params = call.receive<MoveMethodParams>()
                    println("attempting to move ${params.methodName} -> ${params.targetClass}")
                    println("method signature: ${params.methodSignature}")

                    val moveMethodObjects = MoveMethodFactory.createMoveMethodFromName(editor, file, project, params.methodName, params.targetClass)
                    if (moveMethodObjects.isNotEmpty()){
                        invokeAndWait{ moveMethodObjects[0].performRefactoring(project, editor, file) }
                    }

                    call.respond(HttpStatusCode.NoContent)
                } catch (ex: IllegalStateException) {
                    call.respond(HttpStatusCode.BadRequest)
                } catch (ex: JsonConvertException) {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }
            post("/edit-file") {
                val fileName = call.receive<String>()
                // TODO: find file name and change _file_ to point to the right file.
            }
        }
    }

    fun stop(){
        println("Stopping refactoring server.")
    }

}