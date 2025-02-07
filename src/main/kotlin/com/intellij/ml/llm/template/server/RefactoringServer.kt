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
import com.jetbrains.rd.generator.nova.fail
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
        val newName: String,
        val lineNum: Int?
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
                    println("renaming ${params.oldName}@${params.lineNum} -> ${params.newName}")

                    // Call IJ rename API here.
                    val renameObject = RenameVariableFactory.fromOldNewNameAll(
                        project, editor, file, params.oldName, params.newName)
                    if (renameObject.isNotEmpty()) {
                        val refObj = if (renameObject.size > 1){
                            if (params.lineNum == null)
                                throw Exception("too many matching variables/field. " +
                                        "Please choose a line number to identify the variable/field to be renamed.")
                            val objs = renameObject.filter { it.startLoc + 1 == params.lineNum }
                            if (objs.size > 1){
                                throw Exception("too many matching variables/field. " +
                                        "Please choose a line number to identify the variable/field to be renamed.")
                            }else if (objs.isEmpty()){
                                throw Exception("No matching variable/field at the given line number.")
                            } else{
                                objs[0]
                            }
                        }else {
                            renameObject[0]
                        }
                        invokeAndWait { refObj.performRefactoring(project, editor, file) }
                        call.respond(HttpStatusCode.OK)
                    }
                    call.respond(HttpStatusCode.NotImplemented, message="Could not rename ${params.oldName}")
                } catch (ex: IllegalStateException) {
                    print("failed to refactor")
                    call.respond(HttpStatusCode.BadRequest)
                } catch (ex: JsonConvertException) {
                    print("failed")
                    call.respond(HttpStatusCode.BadRequest)
                } catch (ex: Exception){
                    call.respond(HttpStatusCode.BadRequest, message = ex.message.toString())
                }
            }
            post("/extract-method"){
                try {
                    val params = call.receive<ExtractMethodParams>()
                    println("extracting lines ${params.startLine} -> ${params.endLine}: ${params.newName}")

                    // Call IJ rename API here.
                    val refObjs = ExtractMethodFactory.fromStartEndLine(editor, file, params.startLine, params.endLine, params.newName)
                    if (refObjs.isNotEmpty()) {
                        var failedException: Exception? = null
                        invokeAndWait{
                            try{ refObjs[0].performRefactoring(project, editor, file) }
                            catch(ex: Exception){
                                failedException = ex
                            }
                        }
                        if (failedException==null)
                            call.respond(HttpStatusCode.OK)
                        else
                            call.respond(message = failedException!!.message.toString(), status = HttpStatusCode.BadRequest)
                    }
                    else{
                        call.respond(HttpStatusCode.NoContent)
                    }


                } catch (ex: IllegalStateException) {
                    call.respond(HttpStatusCode.BadRequest)
                    ex.printStackTrace()
                } catch (ex: JsonConvertException) {
                    call.respond(HttpStatusCode.BadRequest)
                    ex.printStackTrace()
                } catch (ex: Exception){
                    call.respond(message = ex.message.toString(), status = HttpStatusCode.BadRequest)
                }
            }
            post("/move-method"){
                try {
                    val params = call.receive<MoveMethodParams>()
                    println("attempting to move ${params.methodName} -> ${params.targetClass}")

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