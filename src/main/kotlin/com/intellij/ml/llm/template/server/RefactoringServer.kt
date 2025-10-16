package com.intellij.ml.llm.template.server

import com.google.gson.Gson
import com.intellij.analysis.AnalysisScope
import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.ProjectUtil
import com.intellij.ml.llm.template.agents.RefactoringTools
import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.refactoringobjects.inspection.IdeInspection
import com.intellij.ml.llm.template.refactoringobjects.change_signature.ChangeSignatureRefactoring
import com.intellij.ml.llm.template.refactoringobjects.change_signature.IntroduceParamObject
import com.intellij.ml.llm.template.refactoringobjects.change_signature.TypeChangeRefactoring
import com.intellij.ml.llm.template.refactoringobjects.dataflow.ClassReferenceFinder
import com.intellij.ml.llm.template.refactoringobjects.dataflow.DataFlowAnalyser
import com.intellij.ml.llm.template.refactoringobjects.extractclass.ExtractClassRefactoring
import com.intellij.ml.llm.template.refactoringobjects.extractclass.ExtractEnumRefactoring
import com.intellij.ml.llm.template.refactoringobjects.extractclass.ExtractSuperClassRefactoring
import com.intellij.ml.llm.template.refactoringobjects.extractclass.ExtractInterfaceRefactoring
import com.intellij.ml.llm.template.refactoringobjects.extractfunction.ExtractMethodFactory
import com.intellij.ml.llm.template.refactoringobjects.inspection.CustomCodeInspectionAction
import com.intellij.ml.llm.template.refactoringobjects.introduce.IntroduceFieldFromLiteral
import com.intellij.ml.llm.template.refactoringobjects.introduce.MyIntroduceFieldHandler
import com.intellij.ml.llm.template.refactoringobjects.movemethod.MoveMethodFactory
import com.intellij.ml.llm.template.refactoringobjects.pullup.PullUpRefactoring
import com.intellij.ml.llm.template.refactoringobjects.pullup.PushDownRefactoring
import com.intellij.ml.llm.template.refactoringobjects.reformat.ReformatFile
import com.intellij.ml.llm.template.refactoringobjects.renamevariable.RenameVariable
import com.intellij.ml.llm.template.refactoringobjects.renamevariable.RenameVariableFactory
import com.intellij.ml.llm.template.refactoringobjects.snippet.SnippetFinder
import com.intellij.ml.llm.template.testcuration.TestSelector
import com.intellij.ml.llm.template.utils.FileUtils
import com.intellij.ml.llm.template.utils.PsiUtils
import com.intellij.ml.llm.template.utils.openFileFromQualifiedName
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiJavaFileImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AllClassesSearch
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiLocalVariable
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.search.GlobalSearchScopesCore.DirectoryScope
import com.intellij.util.Processor
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import com.intellij.util.text.findTextRange
import io.ktor.http.*
import io.ktor.serialization.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.jetbrains.kotlin.idea.base.codeInsight.handlers.fixers.endLine
import org.jetbrains.kotlin.idea.base.codeInsight.handlers.fixers.startLine
import org.jetbrains.kotlin.idea.base.psi.getLineNumber
import org.jetbrains.kotlin.idea.core.util.toPsiDirectory
import java.nio.file.Path
import javax.swing.SwingUtilities
import javax.swing.SwingUtilities.invokeAndWait
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds
import javax.lang.model.SourceVersion


class RefactoringServer(var project: Project, var editor: Editor? = null, var file: PsiFile? = null) {
    var testSelector = TestSelector.createSelector(5, project)
    val projectListener = ProjectListener()
    val openProjectsMap = mutableMapOf<String, Project>()
    val SUCCESS_MSG = "success"

    companion object{
        var server : RefactoringServer? = null
        fun getInstance(project: Project): RefactoringServer{
           if (server == null) {
               server = RefactoringServer(project)
               server!!.openProjectsMap[project.basePath!!] = project
               server!!.projectListener.registerListeners(project)
               server!!.start() // This is a long-running process, which doesn't return. So we must do the above things.
           }
            else {
               server!!.project = project
               server!!.openProjectsMap[project.basePath!!] = project
               server!!.projectListener.registerListeners(project)
           }
            return server!!
        }
    }

    fun start(){
        println("Starting refactoring server")
        embeddedServer(Netty, 8082) {
            myApplicationModule()
        }.start(wait = true)
    }

    fun Route.withLogging(block: suspend Route.() -> Unit) {
        intercept(ApplicationCallPipeline.Plugins) { // Intercept the pipeline
            val request = call.request
            println("Incoming request: ${request.httpMethod.value} ${request.uri}")
            proceed() // Continue with the pipeline and execute the route handler
            println("Outgoing response: ${call.response.status()}")
        }
    }

    val ReloadFilePlugin = createRouteScopedPlugin("ReloadFilePlugin") {
        onCall {
            reloadFileIfNeeded()
        }
    }

    fun Application.myApplicationModule() {
        install(ContentNegotiation) {
            json()
        }
        routing {
            install(ReloadFilePlugin)

            get("/") {
                call.respondText("Hello, world!", ContentType.Text.Html)
            }
            get("/get_source_code"){
                call.respond(HttpStatusCode.OK, message = file!!.text)
            }

            post("/get_source_code_snippet"){
                val params = call.receive<SnippetFinderParams>()
                val matchedFile = if (params.filePath !=null) {
                    val foundVfile =
                        LocalFileSystem.getInstance().refreshAndFindFileByPath(project.basePath + "/" + params.filePath)!!
                    PsiManager.getInstance(project).findFile(foundVfile)!!
                }else{
                    file!!
                }

                if (params.codeElementType == "file"){
                    call.respond(HttpStatusCode.OK, message=matchedFile.text)
                    return@post
                }

                val match = formRenameObject(
                    RenameParams(oldName = params.name, newName = params.name+"X", lineNum = params.lineNum, codeElementType = params.codeElementType),
                    useFile = matchedFile
                )
                if (match!=null){

                    call.respond(HttpStatusCode.OK,
                        message = SnippetFinder(
                            file=matchedFile,
                            psiElement=(match as RenameVariable).getResolvedElement()?:match.oldVarPsi
                        ).getSnippet()
                    )
                    return@post
                }
                call.respond(HttpStatusCode.BadRequest)

            }
            get("/get_rel_file_path"){
                call.respond(HttpStatusCode.OK,
                    message = file!!.virtualFile.path
                        .removePrefix(project.basePath.toString())
                        .removePrefix("/"))
            }

            post("reset_waiting"){
                val dumbService = DumbService.getInstance(project)
                dumbService.runWhenSmart {
                    projectListener.reset()
                    println("reset complete!")
                }
                call.respond(HttpStatusCode.OK, message = "reset the project listener.")
            }

            post("wait_for_reload"){
                // TODO: Check that auto reload is on.

                // refresh files to load edits from git, or other.
                VfsUtil.markDirtyAndRefresh(false, true, true, project.baseDir)

                val response = runBlocking {  projectListener.waitForFinish(10000, 180.seconds) }
                if (response)
                    call.respond(HttpStatusCode.OK, message = "Successfully waited for finish")
                else
                    call.respond(HttpStatusCode.NoContent, message = "Unsure if reload finished properly.")
            }

            post("run_test_class"){
                print("running tests")
                val testReport = testSelector.runTests()
                call.respond(HttpStatusCode.OK, message = testReport)
            }

            post("curate_test_class"){
                print("Curate tests here")
                testSelector = TestSelector.createSelector(5, project)
                runReadAction {
                    testSelector.collectTestSamplesForCurrentFile(file!!.virtualFile, project)
                    testSelector.runAndKeepPassingTests()
                }
                call.respond(HttpStatusCode.OK)
            }


            post("/open-project"){
                val params = call.receive<OpenProjectParams>()
                val openedProject = openProjectsMap.get(params.projectPath)
                if (openedProject!=null && openedProject.isOpen) {
                    project = openedProject
//                    ProjectUtil.focusProjectWindow(project, false)
                    call.respond(HttpStatusCode.OK, message = "project was already open")
                    return@post
                }

                project = runBlocking {
                    // TODO: close the existing project before opening the new one.
                    ProjectUtil
                    .openOrImportAsync(
                        Path.of(params.projectPath),
                        options = OpenProjectTask(forceOpenInNewFrame = true, projectToClose = project)
                    ) }!!

                openProjectsMap[project.basePath!!] = project
                call.respond(HttpStatusCode.OK, message = "opened project")
            }

            post("/open-file"){
                println("got request to open a file")
                // open file and set the file and editor values
                val params = call.receive<OpenFileParams>()
                params.filePath
                val vfile = LocalFileSystem.getInstance().refreshAndFindFileByPath(project.basePath + "/" + params.filePath)
                if (vfile==null) {
                    call.respond(HttpStatusCode.NotFound, message = "file not found.")
                    return@post
                }
                invokeAndWait {
                    editor = FileEditorManager.getInstance(project).openTextEditor(
                        OpenFileDescriptor(
                            project,
                            vfile
                        ),
                        true // request focus to editor
                    )!!
                }
                file = PsiManager.getInstance(project).findFile(vfile)!!
                call.respond(HttpStatusCode.OK, message = "opened file!")
            }

            post("/try-open-file"){
                // open file and set the file and editor values
                println("got request to try open a file")
                val params = call.receive<OpenFileParams>()
                val reqFileName = params.filePath.split("/").last()
                if (reqFileName == file!!.name){
                    call.respond(HttpStatusCode.OK, message = "file already open!")
                    return@post
                }

                val vfile = LocalFileSystem.getInstance().refreshAndFindFileByPath(project.basePath + "/" + params.filePath)

                if (vfile==null) {
                    // try opening by qualified name.
                    val qualName = params.filePath.removeSuffix(".java").replace("/", ".")
                    var status = false
                    invokeAndWait {
                        try{
                            openFileFromQualifiedName(qualName, project, true)
                            status=true
                        } catch (e: Exception){
                            status=false
                        }
                    }
                    if (status == true) {
                        call.respond(HttpStatusCode.OK, message = "opened file!")
                        return@post
                    }
                    call.respond(HttpStatusCode.NotFound, message = "file not found.")
                } else {
                    invokeAndWait {
                        editor = FileEditorManager.getInstance(project).openTextEditor(
                            OpenFileDescriptor(
                                project,
                                vfile
                            ),
                            true // request focus to editor
                        )!!
                    }
                    file = PsiManager.getInstance(project).findFile(vfile)!!
                    call.respond(HttpStatusCode.OK, message = "opened file!")
                }
            }


            post("/create-file"){
                val params = call.receive<OpenFileParams>()

                try{
                    FileUtils.createFile(Path("${project.basePath}/${params.filePath}"))
                    call.respond(HttpStatusCode.OK, message = SUCCESS_MSG)
                }
                catch (e: FileAlreadyExistsException){
                    call.respond(HttpStatusCode.OK, message = "File already exists!")
                }
                catch (e: Exception){
                    call.respond(HttpStatusCode.BadRequest, message = "Couldn't create file: ${e.message}")
                }
            }

            post("/try-create-file"){
                val params = call.receive<OpenFileParams>()

                try{ FileUtils.createFile(Path("${project.basePath}/${params.filePath}")) }
                catch (e: Exception){
                    print("Couldn't create file. trying to create from package.")

                }


                val moduleParts = params.filePath.split("/")
                val fileName = moduleParts.last()
                val packaggePsi = JavaPsiFacade.getInstance(project)
                    .findPackage(moduleParts.subList(0, moduleParts.size-1).joinToString("."))
                if (packaggePsi == null)
                    call.respond(HttpStatusCode.BadRequest, message = "package not found")
                else{
                    val packageDir = packaggePsi.directories[0].virtualFile.path
                    FileUtils.createFile(Path("$packageDir/$fileName"))

                    val vfile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                        "$packageDir/$fileName"
                    )
                    if (vfile!=null) {
                        invokeAndWait {
                            editor = FileEditorManager.getInstance(project).openTextEditor(
                                OpenFileDescriptor(
                                    project,
                                    vfile
                                ),
                                true // request focus to editor
                            )!!
                        }
                        call.respond(HttpStatusCode.OK, message = "created file")
                        return@post
                    }
                    call.respond(HttpStatusCode.BadRequest, message = "couldn't create file.")

                }
            }

            post("/rename-old") {
                println("got a request")
                try {
                    val params = call.receive<RenameParams>()
                    println("renaming ${params.oldName}@${params.lineNum} -> ${params.newName}")

                    if (sanityChecks(params)) return@post
                    // Call IJ rename API here.
                    val renameObjectRaw = RenameVariableFactory.fromOldNewNameAll(
                        project, editor!!, file!!, params.oldName, params.newName)
//                        .filter {
//                            if (params.lineNum == null) {
//                                true
//                            } else {
//                                if (params.codeElementType == "method"  || params.codeElementType == "parameter") {
//                                    // For methods, check if line number is within the method's range
//                                    params.lineNum >= it.startLoc && params.lineNum <= it.endLoc + 1
//                                } else {
//                                    // For other elements, check exact line match
//                                    it.startLoc + 1 == params.lineNum
//                                }
//                            }
//                        }
                    val renameObject =
                        if (renameObjectRaw.size<=1) {
                            renameObjectRaw
                        }
                        else if (params.codeElementType!=null){
                            val filtered = renameObjectRaw.filter {
                                PsiUtils.isCodeElementType(
                                    (it as RenameVariable).oldVarPsi, params.codeElementType
                                )
                            }
                            filtered.ifEmpty { renameObjectRaw }
                        }else{
                            renameObjectRaw
                        }
                    if (renameObject.isEmpty()){
                        val reverse = RenameVariableFactory.fromOldNewNameAll(project, editor!!, file!!, params.newName, params.oldName)
                        if (reverse.isNotEmpty()){
                            call.respond(HttpStatusCode.BadRequest, "The rename has already been performed. " +
                                    "Variable ${params.newName} exists in this file. Variable ${params.oldName} does not exist.")
                        }
                        else if (params.lineNum != null){
                            call.respond(HttpStatusCode.BadRequest, "No matching variable/field at the given line number.")
                        }
                        else{
                            call.respond(
                                HttpStatusCode.BadRequest, "Could not rename ${params.oldName}. " +
                                        "If the old_name is from an external library, it cannot be renamed."
                            )
                        }
                        return@post
                    }
                    renameObject.map {
                        it.performRefactoring(project, editor!!, file!!)
                    }
                    try{
                        invokeAndWait {
                            FileDocumentManager.getInstance().saveAllDocuments() // save changes to local filesystem
                        }
                    } catch (ex: Exception){
                        println("Failed to save all documents :/")
                    }
                    call.respond(HttpStatusCode.OK, message=SUCCESS_MSG)
                } catch (ex: IllegalStateException) {
                    ex.printStackTrace()
                    print("failed to refactor")
                    call.respond(HttpStatusCode.BadRequest)
                } catch (ex: JsonConvertException) {
                    ex.printStackTrace()
                    print("failed")
                    call.respond(HttpStatusCode.BadRequest)
                } catch (ex: Exception){
                    ex.printStackTrace()
                    call.respond(HttpStatusCode.BadRequest, message = ex.message.toString())
                } finally {
                    try{
                        invokeAndWait {
                            FileDocumentManager.getInstance().saveAllDocuments() // save changes to local filesystem
                        }
                    } catch (ex: Exception){
                        println("Failed to save all documents :/")
                    }
                }
            }


            post("/rename") {
                println("got a request")
                try {
                    val params = call.receive<RenameParams>()
                    println("renaming ${params.oldName}@${params.lineNum} -> ${params.newName}")

                    if (sanityChecks(params)) return@post

                    val renameObject = formRenameObject(params)


                    if (renameObject==null){
                        responseToRenameWithMessage(params)
                        return@post
                    }

                    renameObject.performRefactoring(project, editor!!, file!!)
                    try{
                        invokeAndWait {
                            FileDocumentManager.getInstance().saveAllDocuments() // save changes to local filesystem
                        }
                    } catch (ex: Exception){
                        println("Failed to save all documents :/")
                    }
                    call.respond(HttpStatusCode.OK, message=SUCCESS_MSG)
                } catch (ex: IllegalStateException) {
                    ex.printStackTrace()
                    print("failed to refactor")
                    call.respond(HttpStatusCode.BadRequest)
                } catch (ex: JsonConvertException) {
                    ex.printStackTrace()
                    print("failed")
                    call.respond(HttpStatusCode.BadRequest)
                } catch (ex: Exception){
                    ex.printStackTrace()
                    call.respond(HttpStatusCode.BadRequest, message = ex.message.toString())
                } finally {
                    try{
                        invokeAndWait {
                            FileDocumentManager.getInstance().saveAllDocuments() // save changes to local filesystem
                        }
                    } catch (ex: Exception){
                        println("Failed to save all documents :/")
                    }
                }
            }


            post("/form-rename-object") {
                println("got a request")
                try {
                    val params = call.receive<RenameParams>()
                    println("renaming ${params.oldName}@${params.lineNum} -> ${params.newName}")

                    if (sanityChecks(params)) return@post
                    // Call IJ rename API here.
                    val renameObject = formRenameObject(params)
                    if (renameObject==null){
                        responseToRenameWithMessage(params)
                        return@post
                    }

                    // There is at least one valid rename object. Find the best match and return it
                    call.respond(HttpStatusCode.OK, RenameParams(
                        params.oldName,
                        params.newName,
                        renameObject.startLoc,
                        params.codeElementType, // todo: fetch the code element type from the rename object.
                        startLineComments = editor?.let {
                            (renameObject as? RenameVariable)?.startLineWithComments(it)?.plus(1) },
                        resolvedFilePath = (renameObject as? RenameVariable)?.getResolvedFilePath()?.removePrefix("${project.basePath}/"),
                        resolvedStartLine = (renameObject as? RenameVariable)?.getResolvedStartLine()?.plus(1),
                    ))


                } catch (ex: IllegalStateException) {
                    ex.printStackTrace()
                    print("failed to refactor")
                    call.respond(HttpStatusCode.BadRequest)
                } catch (ex: JsonConvertException) {
                    ex.printStackTrace()
                    print("failed")
                    call.respond(HttpStatusCode.BadRequest)
                } catch (ex: Exception){
                    ex.printStackTrace()
                    call.respond(HttpStatusCode.BadRequest, message = ex.message.toString())
                } finally {

                }
            }

            post("/form-rename-object-all") {
                println("got a request")
                try {
                    val params = call.receive<RenameParams>()
                    println("renaming ${params.oldName}@${params.lineNum} -> ${params.newName}")

                    if (sanityChecks(params)) return@post
                    // Call IJ rename API here.
                    val allRenameObjects = RenameVariableFactory.fromOldNewNameAll(
                        project, editor!!, file!!, params.oldName, params.newName
                    )
                    if (allRenameObjects.size==0){
                        responseToRenameWithMessage(params)
                        return@post
                    }

                    // There is at least one valid rename object. Return all matches.
                    call.respond(HttpStatusCode.OK, allRenameObjects.map {
                        renameObject ->
                            val rv = renameObject as RenameVariable
                            RenameParams(
                                params.oldName,
                                params.newName,
                                renameObject.startLoc,
                                PsiUtils.getElementTypeStr(rv.oldVarPsi)?: rv.getResolvedElement()
                                    ?.let { PsiUtils.getElementTypeStr(it) },
                                startLineComments = editor?.let {
                                    (renameObject as? RenameVariable)?.startLineWithComments(it)?.plus(1)
                                },
                                resolvedFilePath = (renameObject as? RenameVariable)?.getResolvedFilePath()
                                    ?.removePrefix("${project.basePath}/"),
                                resolvedStartLine = (renameObject as? RenameVariable)?.getResolvedStartLine()?.plus(1),
                            )
                        }.toList()
                    )


                } catch (ex: IllegalStateException) {
                    ex.printStackTrace()
                    print("failed to refactor")
                    call.respond(HttpStatusCode.BadRequest)
                } catch (ex: JsonConvertException) {
                    ex.printStackTrace()
                    print("failed")
                    call.respond(HttpStatusCode.BadRequest)
                } catch (ex: Exception){
                    ex.printStackTrace()
                    call.respond(HttpStatusCode.BadRequest, message = ex.message.toString())
                } finally {

                }
            }

            post("/form-rename-object-all-matching") {
                println("got a request")
                try {
                    val params = call.receive<RenameParams>()
                    println("renaming ${params.oldName}@${params.lineNum} -> ${params.newName}")

                    if (sanityChecks(params)) return@post
                    // Call IJ rename API here.
                    val allRenameObjects = RenameVariableFactory.fromOldNewNameAllMatching(
                        project, editor!!, file!!, params.oldName, params.newName
                    )
                    if (allRenameObjects.size==0){
                        responseToRenameWithMessage(params)
                        return@post
                    }

                    // There is at least one valid rename object. Return all matches.
                    call.respond(HttpStatusCode.OK, allRenameObjects.map {
                            renameObject ->
                        val rv = renameObject as RenameVariable
                        RenameParams(
                            params.oldName,
                            params.newName,
                            renameObject.startLoc,
                            PsiUtils.getElementTypeStr(rv.oldVarPsi)?: rv.getResolvedElement()
                                ?.let { PsiUtils.getElementTypeStr(it) },
                            startLineComments = editor?.let {
                                (renameObject as? RenameVariable)?.startLineWithComments(it)?.plus(1)
                            },
                            resolvedFilePath = (renameObject as? RenameVariable)?.getResolvedFilePath()
                                ?.removePrefix("${project.basePath}/"),
                            resolvedStartLine = (renameObject as? RenameVariable)?.getResolvedStartLine()?.plus(1),
                        )
                    }.toList()
                    )


                } catch (ex: IllegalStateException) {
                    ex.printStackTrace()
                    print("failed to refactor")
                    call.respond(HttpStatusCode.BadRequest)
                } catch (ex: JsonConvertException) {
                    ex.printStackTrace()
                    print("failed")
                    call.respond(HttpStatusCode.BadRequest)
                } catch (ex: Exception){
                    ex.printStackTrace()
                    call.respond(HttpStatusCode.BadRequest, message = ex.message.toString())
                } finally {

                }
            }
            post("/extract_method"){
                try {
                    val params = call.receive<ExtractMethodParams>()
                    println("extracting lines ${params.startLine} -> ${params.endLine}: ${params.newName}")

                    // Call IJ rename API here.
                    val refObjs = ExtractMethodFactory.fromStartEndLine(editor!!, file!!, params.startLine, params.endLine, params.newName)
                    if (refObjs.isNotEmpty()) {
                        var failedException: Exception? = null
//                        invokeAndWait{
                        try{ refObjs[0].performRefactoring(project, editor!!, file!!) }
                        catch(ex: Exception){
                            failedException = ex
                            ex.printStackTrace()
                        }
//                        }
                        if (failedException==null)
                            call.respond(HttpStatusCode.OK, message = SUCCESS_MSG)
                        else
                            call.respond(message = failedException!!.message.toString()+
                                    " Make sure your selection is entirely within the body of the method",
                                status = HttpStatusCode.BadRequest)
                    }
                    else{
                        call.respond(HttpStatusCode.NoContent, message = "couldn't create a refactoring object. " +
                                "Please check that you have not selected the entire body of a method")
                    }


                } catch (ex: IllegalStateException) {
                    call.respond(HttpStatusCode.BadRequest, message = "invalid request body.")
                    ex.printStackTrace()
                } catch (ex: JsonConvertException) {
                    call.respond(HttpStatusCode.BadRequest, message = "invalid request")
                    ex.printStackTrace()
                } catch (ex: Exception){
                    call.respond(message = ex.message.toString(), status = HttpStatusCode.BadRequest)
                }
            }
            post("/move-method"){
                try {
                    val params = call.receive<MoveMethodParams>()
                    println("attempting to move ${params.methodName} -> ${params.targetClass}")

                    val moveMethodObjects = try{
                        runReadAction{
                            MoveMethodFactory.createMoveMethodFromName(
                                editor!!,
                                file!!,
                                project,
                                params.methodName,
                                params.targetClass,
                                true
                            )
                        }
                    } catch (e: Exception){
                        call.respond(HttpStatusCode.BadRequest, message = "${e.cause}. ${e.message}")
                        return@post
                    }
                    if (moveMethodObjects.isNotEmpty()){
                        invokeAndWait{ moveMethodObjects[0].performRefactoring(project, editor!!, file!!) }
                        call.respond(HttpStatusCode.OK, message = SUCCESS_MSG)
                        return@post
                    }
                    else {
                        call.respond(
                            HttpStatusCode.NoContent,
                            message = "could not create a refactoring object. Please check that the target class exists."
                        )
                        return@post
                    }
                } catch (ex: IllegalStateException) {
                    call.respond(HttpStatusCode.BadRequest, message = "invalid parameters.")
                } catch (ex: JsonConvertException) {
                    call.respond(HttpStatusCode.BadRequest, message = "invalid parameters.")
                }
            }
            post("extract-class"){
                val params = call.receive<ExtractClassParams>()
                val psiClass = (file as PsiJavaFileImpl).classes[0]
                val clazz = PsiUtils.getClassesByName(project, params.newName)
                if (clazz.isNotEmpty() &&  params.subClassName==psiClass.name){
                    call.respond(HttpStatusCode.BadRequest, message = "Cannot perform refactoring because the class exists. " +
                            "If you would like to use the class, consider using a type change refactoring. " +
                            "If you would like to move members into the class, use a move refactoring.")
                    return@post
                }

                val refObj = try{
                    if (params.extractionType == ExtractionType.INTERFACE)
                        ExtractInterfaceRefactoring.createFromMembers(
                            psiClass,
                            params.members,
                            params.newName,
                            params.subClassName
                        )
                    else if (params.extractionType == ExtractionType.SUPERCLASS){
                        ExtractSuperClassRefactoring.createFromMembers(
                            psiClass,
                            params.members,
                            params.newName,
                            params.subClassName
                        )
                    }
                    else if (params.extractionType == ExtractionType.CLASS){
                        ExtractClassRefactoring.createFromMembers(
                            psiClass,
                            params.members,
                            params.newName
                        )
                    } else if (params.extractionType == ExtractionType.ENUM){
                        ExtractEnumRefactoring.createFromMembers(
                            psiClass,
                            params.members,
                            params.newName
                        )
                    }else{
                        throw Exception("Unknown extraction type ${params.extractionType}")
                    }
                } catch (e: Exception){
                    call.respond(HttpStatusCode.BadRequest, message = e.message.toString())
                    return@post
                }
                try{
                    invokeAndWait {
                            refObj.performRefactoring(project, editor!!, file!!)
                    }
                    call.respond(HttpStatusCode.OK, message = SUCCESS_MSG)
                } catch (e: Exception){
                    e.printStackTrace()
                    call.respond(HttpStatusCode.BadRequest, message = "failed to perform refactoring ${e.cause}. ${e.message}")
                }

            }

            post("push-down"){
                val params = call.receive<PushDownParams>()
                val psiClass = (file as PsiJavaFileImpl).classes[0]

                val refObj = PushDownRefactoring.fromMembers(
                    psiClass, params.members, params.keepAbstract)

                try{
                    invokeAndWait {
                        refObj.performRefactoring(project, editor!!, file!!)
                    }
                    call.respond(HttpStatusCode.OK, message = SUCCESS_MSG)
                } catch (e: Exception){
                    e.printStackTrace()
                    call.respond(HttpStatusCode.BadRequest, message = "failed to perform refactoring ${e.cause}. ${e.message}")
                }

            }

            post("pull-up"){
                val params = call.receive<PullUpParams>()
                val psiClass = (file as PsiJavaFileImpl).classes[0]
                val targetClasses = psiClass.interfaces.filter { it.name==params.superClass }.toMutableList()
                targetClasses.add(psiClass.superClass)

                if (targetClasses.isEmpty()){
                    call.respond(HttpStatusCode.BadRequest, message = "No such super class/interface")
                    return@post
                }
                val targetClass = targetClasses[0]

                val refObj = PullUpRefactoring.fromMembers(
                    psiClass, targetClass, params.members, params.makeAbstract)

                try{
                    invokeAndWait {
                        refObj.performRefactoring(project, editor!!, file!!)
                    }
                    call.respond(HttpStatusCode.OK, message = SUCCESS_MSG)
                } catch (e: Exception){
                    e.printStackTrace()
                    call.respond(HttpStatusCode.BadRequest, message = "failed to perform refactoring ${e.cause}. ${e.message}")
                }

            }

            post(RefactoringTools.ReplaceFile.NAME){
                try {
                    val params = call.receive<RefactoringTools.ReplaceFile.CallParams>()
                    println("attempting to replace file contents of ${params.filePath}")

                    val oldContents = Path(file!!.virtualFile.path).readText()
                    FileUtils.replaceFileContents(
                        Path(file!!.virtualFile.path),
                        params.newContent
                    )
                    VfsUtil.markDirtyAndRefresh(false, true, true, project.baseDir)
                    // Run IJ linter
                    reloadFileIfNeeded()
                    SwingUtilities.invokeAndWait { ReformatFile.doReformat(file!!, file!!.startOffset, file!!.endOffset) }
                    Thread.sleep(5000) // wait for reformat to complete.
                    SwingUtilities.invokeAndWait {
                        FileDocumentManager.getInstance().saveDocument(editor!!.document) // save changes to local filesystem
                    }

                    val testReport = testSelector.runTests()
                    val message = revertIfTestsFailed(testReport, oldContents)
                    if (message==SUCCESS_MSG)
                        call.respond(HttpStatusCode.OK, message = message)
                    else
                        call.respond(HttpStatusCode.BadRequest, message = message)
                } catch (ex: IllegalStateException) {
                    call.respond(HttpStatusCode.BadRequest)
                } catch (ex: JsonConvertException) {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }

            post(RefactoringTools.ReplaceMethod.NAME){
                try {
                    val params =
                        call.receive<RefactoringTools.ReplaceMethod.CallParams>()
                    val matches = PsiUtils.getAllMethodNameFromClass(file, params.methodName)!!


                    if (matches.size == 0)
                        call.respond(HttpStatusCode.NotFound,
                            message = "no method with the name `${params.methodName}` was found.")
                    else if (matches.size > 1 && params.lineNum==null)
                        call.respond(HttpStatusCode.BadRequest,
                            message = "Too many methods (${matches.size}) have that name. " +
                                "Please identify the method from it's line number.")

                    val methodPsi = matches.sortedBy { abs(it.startLine(editor!!.document) - params.lineNum!!) }.first()

                    val oldContents = runReadAction{ editor!!.document.text }


                    val regex = Regex("""\b([a-zA-Z_][a-zA-Z0-9_]*)\s*\(""")
                    val matchResult = regex.find(params.newContent)?.groups?.get(1)?.value
                    val startOffset =
                        if (matchResult == null){
                            methodPsi.body!!.startOffset
                        }else{
                            methodPsi.startOffset
                        }
                    val endOffset = if (matchResult == null) methodPsi.body!!.endOffset else methodPsi.endOffset


                    FileUtils.replaceFileContentsInRange(
                        Path(file!!.virtualFile.path),
                        startOffset, endOffset,
                        params.newContent
                    )
                    VfsUtil.markDirtyAndRefresh(false, true, true, project.baseDir)
                    SwingUtilities.invokeAndWait {
                        ReformatFile.doReformat(
                            file!!,
                            startOffset,
                            startOffset + params.newContent.length
                        )
                    }
                    Thread.sleep(5000) // sleep five seconds to allow the reformatting to complete.
                    SwingUtilities.invokeAndWait {
                        FileDocumentManager.getInstance().saveDocument(editor!!.document) // save changes to local filesystem
                    }
                    val testReport = testSelector.runTests()
                    val message = revertIfTestsFailed(testReport, oldContents)
                    if (message==SUCCESS_MSG)
                        call.respond(HttpStatusCode.OK, message = message)
                    else
                        call.respond(HttpStatusCode.BadRequest, message = message)
                } catch (ex: IllegalStateException) {
                    call.respond(HttpStatusCode.BadRequest)
                } catch (ex: JsonConvertException) {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }

            post("run_code_inspection_old"){
                val inspection = IdeInspection(project, AnalysisScope(file!!), file!!, editor!!)
                invokeAndWait{ inspection.doInspect() }
                try{ inspection.waitForCompletion() }
                catch (e: Exception){
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, message = e.message.toString())
                    return@post
                }
                call.respond(HttpStatusCode.OK, message = Gson().toJson(inspection.problems).toString())

            }

            post("run_code_inspection"){
                val response = runBlocking {  projectListener.waitForFinish(1500, 30.seconds) }
                val inspection = CustomCodeInspectionAction()
                try {
                    invokeAndWait { inspection.doAnalysis(project, AnalysisScope(file!!)) }
                    inspection.waitForCompletion()
                }
                catch (e: Exception){
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, message = e.message.toString())
                    inspection.cleanup()
                    return@post
                }
                call.respond(HttpStatusCode.OK, message = Gson().toJson(inspection.problems).toString())

            }

            post("import_symbol"){
                val symbolName = call.receiveText()
                val oldContents = Path(file!!.virtualFile.path).readText()
                val textRange = oldContents.findTextRange(symbolName)
                if (textRange!=null)
                    IdeInspection.importFromRange(textRange, editor!!, file!!)

            }

            post("save_all_changes"){
                invokeAndWait{
                    FileDocumentManager.getInstance().saveAllDocuments() // save changes to local filesystem
                }
                Thread.sleep(2000)
                call.respond(HttpStatusCode.OK, message = "success")
            }

            post("change_signature"){
                print("got change signature")
                val params = call.receive<ChangeSignatureParams>()
                val refObj = runReadAction {
                    ChangeSignatureRefactoring.createFromParams(
                        project,
                        editor!!,
                        file!!,
                        params
                    )
                }
                try{ invokeAndWait { refObj.performRefactoring(project, editor!!, file!!) } }
                catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(HttpStatusCode.BadRequest,
                        message = "failed to perform refactoring ${e.cause}. ${e.message}")
                    return@post
                }
                call.respond(HttpStatusCode.OK, SUCCESS_MSG)
            }

            post("introduce_param_object"){
                val params = call.receive<IntroduceParamObjectParams>()
                val refObj = runReadAction{
                    IntroduceParamObject.createFromParams(params, file!!, editor!!, project)
                }
                try{ invokeAndWait { refObj.performRefactoring(project, editor!!, file!!) } }
                catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(HttpStatusCode.BadRequest,
                        message = "failed to perform refactoring ${e.cause}. ${e.message}")
                    return@post
                }
                call.respond(HttpStatusCode.OK, SUCCESS_MSG)
            }

            post("type_change"){
                val params = call.receive<TypeChangeParams>()

                try{
                    val refObj = runReadAction{
                        TypeChangeRefactoring.createFromParams(params, project = project, editor = editor!!, file = file!!)
                    }
                    invokeAndWait { refObj.doChange() }
                }
                catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(HttpStatusCode.BadRequest,
                        message = "failed to perform refactoring ${e.cause}. ${e.message}")
                    return@post
                }
                call.respond(HttpStatusCode.OK, SUCCESS_MSG)
            }

            post("extract_field"){

                val params = call.receive<ExtractFieldParams>()
                val refObj = runReadAction{
                    MyIntroduceFieldHandler.fromVariable(params, project, editor!!, file!!)
                }
                try{ invokeAndWait { refObj() } }
                catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(HttpStatusCode.BadRequest,
                        message = "failed to perform refactoring ${e.cause}. ${e.message}")
                    return@post
                }
                call.respond(HttpStatusCode.OK, SUCCESS_MSG)
            }
            post("extract_field_from_literal"){

                val params = call.receive<ExtractFieldFromLiteralParams>()

                try{
                    val refObj = runReadAction{
                        IntroduceFieldFromLiteral.fromVariable(params, project, editor!!, file!!)
                    }
                    invokeAndWait { refObj() }
                }
                catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(HttpStatusCode.BadRequest,
                        message = "failed to perform refactoring ${e.cause}. ${e.message}")
                    return@post
                }
                call.respond(HttpStatusCode.OK, SUCCESS_MSG)
            }

            get("get_linked_files"){
                val javaClass = (file as PsiJavaFile).classes[0]
                val linkedClasses = runReadAction{ PsiUtils.getLinkedClasses(javaClass, project) }
                val linkedFiles = linkedClasses.map {
                    it.containingFile.virtualFile?.path?.removePrefix("${project.basePath}/")
                }.filterNotNull()
                call.respond(HttpStatusCode.OK, message = Gson().toJson(linkedFiles))
            }

            post("get_links_from_method"){
                val params = call.receive<GetLinksParams>()
                val psiMethod = (file as PsiJavaFile).classes[0].methods.filter { it.name==params.methodName }.first()
                val linkedClasses = runReadAction{ PsiUtils.getLinkedClasses(psiMethod, project) }
                val linkedFiles = linkedClasses.map {
                    it.containingFile.virtualFile?.path?.removePrefix("${project.basePath}/")
                }.filterNotNull()
                    .filter { it.endsWith(".java") }
                call.respond(HttpStatusCode.OK, message = Gson().toJson(linkedFiles))
            }

            post("get_linked_elements"){
                val params = call.receive<GetLinksParams>()
                val psiMethodList = (file as PsiJavaFile).classes[0].methods.filter {
                    editor!!.document.getLineNumber(it.startOffset) <= params.lineNum &&
                            params.lineNum <= editor!!.document.getLineNumber(it.endOffset)
                }
                val methodsToLink = if (psiMethodList.isEmpty()){
                    // it was probably a field that was on the line.
                    // using the constructors to reference other classes.
                    (file as PsiJavaFile).classes[0].constructors.toList()
                }else{
                    psiMethodList
                }

                val linkedElements = mutableListOf<PsiElement>()
                methodsToLink.forEach {
                    psiMethod -> linkedElements.addAll(
                     runReadAction{ PsiUtils.getLinkedElements(psiMethod, project) }
                    )
                }
                val linkedFiles = runReadAction{
                    linkedElements.map {
                        val path = it.containingFile.virtualFile?.path?.removePrefix("${project.basePath}/")
                        if (path!=null && path.endsWith(".java"))
                            buildJsonObject {
                                put("file_path", it.containingFile.virtualFile?.path?.removePrefix("${project.basePath}/"))
                                put("line_num", it.getLineNumber() + 1)
                            }
                        else
                            null
                    }.filterNotNull()
                }

                call.respond(HttpStatusCode.OK, message = linkedFiles.toString())
            }

            post("find_replace"){
                val params = call.receive<FindReplaceParams>()
                var found = false
                if (params.replaceInComments){
                    // TODO: Replace the string only in comments.
                    print("TODO: Replace in comments.")
                }
                else {
                    val matchingElements = runReadAction{
                        PsiUtils
                            .getElementMatchingTextNoWhiteSpace(file!!, params.findText)
                    }

                    if (matchingElements.isEmpty()){

                        val status = FileUtils.textBasedFindReplace(params, editor!!, file!!)
                        if (status) {
                            VfsUtil.markDirtyAndRefresh(false, true, true, project.baseDir)
                            call.respond(HttpStatusCode.OK, message = SUCCESS_MSG)
                            return@post
                        }

                        call.respond(HttpStatusCode.NotFound,
                            message = "The search text was not found in the file. " +
                                    "Did not replace any text")
                        return@post
                    }

                    val elementsToReplace = if (params.lineNum!=null){
                        matchingElements.filter { it.startLine(editor!!.document)+1==params.lineNum }
                    }else{matchingElements}

                    if (elementsToReplace.isEmpty()){
                            call.respond(HttpStatusCode.NotFound,
                                message = "The `find text` was not found on line number ${params.lineNum} ")
                        return@post
                    }

                    elementsToReplace.forEach {
                        WriteCommandAction.runWriteCommandAction(project) {
                            val newElement = PsiUtils.createPsiElementFromText(params.replaceText, project)
                            if (newElement!=null)
                                it.replace(newElement)
                            else{
                                FileUtils.replaceFileContentsInRange(
                                    Path(file!!.virtualFile.path), it.startOffset, it.endOffset, params.replaceText
                                )
                                VfsUtil.markDirtyAndRefresh(false, true, true, project.baseDir)
                            }
                        }
                    }

                }
                    call.respond(HttpStatusCode.OK, message = SUCCESS_MSG)
            }

            post("update_comment"){
                val params = call.receive<FindReplaceParams>()
                val comments = runReadAction{ PsiUtils.getAllComments(file!!) }
                val matchingComments = comments.filter {
                    it.startLine(editor!!.document) + 1 <= params.lineNum!! && params.lineNum <= it.endLine(editor!!.document)+1
                }

                if (matchingComments.isEmpty()) {
                    call.respond(HttpStatusCode.NotFound, message = "No comment found on line ${params.lineNum}.")
                    return@post
                }
                FileUtils.textBasedFindReplace(params, editor!!, file!!)
                Thread.sleep(500) // sleep to allow file system changes to take effect
                VfsUtil.markDirtyAndRefresh(false, true, true, project.baseDir) // works most of the time.
                reloadFileIfNeeded()
                call.respond(HttpStatusCode.OK, message = SUCCESS_MSG)

            }

            get("reload_from_vfs"){
                VfsUtil.markDirtyAndRefresh(false, true, true, project.baseDir)
                reloadFileIfNeeded()
                Thread.sleep(3000)
                call.respond(HttpStatusCode.OK)
            }


            post("search_keyword") {
                try {
                    println("Debug: search_keyword endpoint called")
                    val params = call.receive<SymbolSearchParams>()
                    val keyword = params.symbol
                    println("Debug: Received keyword: '$keyword'")

                    val results = runReadAction {
                        try {
                            val foundOccurrences = mutableListOf<JsonObject>()
                            
                            // Use IntelliJ's text search functionality similar to "Search Everywhere"
                            val searchScope = GlobalSearchScope.allScope(project)
                            val searchHelper = PsiSearchHelper.getInstance(project)
                            
                            println("Debug: Searching for keyword '$keyword' across all Java files...")
                            
                            // Search for keyword occurrences in all contexts
                            searchHelper.processElementsWithWord(
                                { element, offsetInElement ->
                                    try {
                                        val file = element.containingFile?.virtualFile
                                        if (file != null && file.extension == "java") {
                                            val relativePath = file.path.removePrefix("${project.basePath}/")
                                            val document = PsiDocumentManager.getInstance(project).getDocument(element.containingFile)
                                            val lineNum = document?.getLineNumber(element.textOffset + offsetInElement)?.plus(1) ?: -1
                                            
                                            // Get the actual line content for context
                                            val lineContent = try {
                                                document?.let { doc ->
                                                    if (lineNum > 0 && lineNum <= doc.lineCount) {
                                                        val lineStartOffset = doc.getLineStartOffset(lineNum - 1)
                                                        val lineEndOffset = doc.getLineEndOffset(lineNum - 1)
                                                        doc.getText().substring(lineStartOffset, lineEndOffset).trim()
                                                    } else ""
                                                } ?: ""
                                            } catch (e: Exception) {
                                                println("Debug: Error getting line content: ${e.message}")
                                                ""
                                            }
                                            
                                            // Determine the type of occurrence
                                            val occurrenceType = when {
                                                lineContent.contains("import") && lineContent.contains(keyword) -> "import"
                                                lineContent.contains("public") && lineContent.contains("class") -> "class_declaration"
                                                lineContent.contains("public") && lineContent.contains("(") -> "method_declaration"
                                                lineContent.contains("private") || lineContent.contains("protected") -> "field_or_method"
                                                lineContent.startsWith("//") || lineContent.contains("/*") -> "comment"
                                                lineContent.contains("\"") && lineContent.contains(keyword) -> "string_literal"
                                                else -> "code_usage"
                                            }
                                            
                                            foundOccurrences.add(buildJsonObject {
                                                put("file_path", relativePath)
                                                put("line_num", lineNum)
                                                put("line_content", lineContent)
                                                put("occurrence_type", occurrenceType)
                                                put("is_test_file", file.path.contains("/test/"))
                                            })
                                            
                                            if (file.path.contains("/test/")) {
                                                println("Debug: Found '$keyword' in test file: $relativePath:$lineNum")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        println("Debug: Error processing element: ${e.message}")
                                    }
                                    true
                                },
                                searchScope,
                                keyword,
                                UsageSearchContext.ANY,
                                false // case-insensitive for broader search
                            )
                            
                            println("Debug: Found ${foundOccurrences.size} occurrences of '$keyword'")
                            
                            // Get unique files only (first occurrence per file)
                            foundOccurrences
                                .distinctBy { it["file_path"].toString() }
                                .sortedBy { it["file_path"].toString() }
                        } catch (e: Exception) {
                            println("Debug: Error in runReadAction: ${e.message}")
                            e.printStackTrace()
                            emptyList<JsonObject>()
                        }
                    }

                    println("Debug: Returning ${results.size} results")
                    call.respond(HttpStatusCode.OK, results)
                } catch (e: Exception) {
                    println("Debug: Error in search_keyword endpoint: ${e.message}")
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, "Error: ${e.message}")
                }
            }

            post("search_symbol") {
                val params = call.receive<SymbolSearchParams>()
                val symbolName = params.symbol
                println("Debug: Searching for symbol: '$symbolName'")

                val (fileHits, totalHits) = runReadAction {
                    val results = mutableListOf<PsiElement>()
                    val searchDirParent = file!!.containingDirectory.parentDirectory!!
                    val searchDir = if (params.parentCount > 1){
                        var newSearchDir = searchDirParent
                        for (count in 1 .. params.parentCount){
                            newSearchDir = newSearchDir.parentDirectory?:newSearchDir
                        }
                        newSearchDir
                    }else{
                        searchDirParent
                    }
                    val complementaryDir: PsiDirectory? = if ("test/" in searchDir.virtualFile.path){
                        val vfile = LocalFileSystem.getInstance().refreshAndFindFileByPath(searchDir.virtualFile.path.replace("test/", "main/"))
                        vfile?.toPsiDirectory(project)
                    } else if ("main/" in searchDir.virtualFile.path){
                        val vfile = LocalFileSystem.getInstance().refreshAndFindFileByPath(searchDir.virtualFile.path.replace("main/", "test/"))
                        vfile?.toPsiDirectory(project)
                    }else{
                        null
                    }
                    val scope = DirectoryScope(project, file!!.containingDirectory.parentDirectory!!.virtualFile, true)

                    file!!.containingDirectory.parentDirectory!!.virtualFile

                    // Search all classes in the project
                    println("Debug: Starting AllClassesSearch...")
                    AllClassesSearch.search(scope, project).allowParallelProcessing()
                        .forEach(Processor<PsiClass> { psiClass ->
                            searchInClass(psiClass, symbolName, results)
                            true
                        })

                    if (complementaryDir!=null){
                        val complementaryScope = DirectoryScope(project, complementaryDir.virtualFile, true)
                        AllClassesSearch.search(complementaryScope, project).allowParallelProcessing()
                            .forEach(Processor<PsiClass> { psiClass ->
                                searchInClass(psiClass, symbolName, results)
                                true
                            })
                    }

                    println("Debug: Total results found: ${results.size}")

                    // Group results by file and collect line numbers
                    val grouped = results.groupBy { element ->
                        element.containingFile?.virtualFile?.path?.removePrefix("${project.basePath}/") ?: "unknown"
                    }

                    val fileHits = grouped.map { (path, elements) ->
                        val lineNumbers = elements.map { element ->
                            val offset = element.textOffset
                            val doc = PsiDocumentManager.getInstance(project).getDocument(element.containingFile)
                            doc?.getLineNumber(offset)?.plus(1) ?: -1
                        }.sorted()

                        buildJsonObject {
                            put("file_path", path)
                            put("hit_count", elements.size)
                            put("line_nums", buildJsonArray { lineNumbers.forEach { add(it) } })
                        }
                    }

                    fileHits to results.size
                }

                // Respond with overall and per-file counts
                val response = buildJsonObject {
                    put("hit_count", totalHits)
                    put("files", JsonArray(fileHits))
                }

                call.respond(HttpStatusCode.OK, response)
            }


            post("search_symbol_changed") {
                val params = call.receive<RenamePairParams>()
                val oldName = params.oldName
                val newName = params.newName
                
                // Extract the changed part from the rename pair
                val changedPart = extractChangedPart(oldName, newName)
                println("Debug: Rename pair: $oldName → $newName")
                println("Debug: Extracted changed part: '$changedPart'")
                
                // Use the same logic as search_symbol but with the changed part
                val linkedFiles = runReadAction {
                    val results = mutableListOf<PsiElement>()

                    val scope = GlobalSearchScope.allScope(project)

                    // Search all classes in the project (with result limiting)
                    val maxResults = 5000
                    var resultCount = 0
                    
                    AllClassesSearch.search(scope, project).allowParallelProcessing()
                            .forEach(Processor<PsiClass> { psiClass ->
                                if (resultCount < maxResults) {
                                    searchInClassContainsLimited(psiClass, changedPart, results, maxResults) { count ->
                                        resultCount = count
                                    }
                                }
                                true
                            })

                    // Also walk source roots (to catch test files / special dirs)
                    if (resultCount < maxResults) {
                        val sourceRoots = ProjectRootManager.getInstance(project).contentSourceRoots
                        sourceRoots.forEach { sourceRoot ->
                            if (resultCount < maxResults) {
                                searchInDirectoryContainsLimited(sourceRoot, changedPart, results, maxResults) { count ->
                                    resultCount = count
                                }
                            }
                        }
                    }
                    
                    println("Debug: Found $resultCount results (limited to $maxResults)")

                    // 🔹 Map results to JSON { file_path, line_num } - unique files only
                    val mappedResults = results.mapNotNull { element ->
                        val vFile = element.containingFile?.virtualFile ?: return@mapNotNull null
                        val path = vFile.path.removePrefix("${project.basePath}/")
                        val lineNum = element.textOffset.let { offset ->
                            val doc = PsiDocumentManager.getInstance(project).getDocument(element.containingFile)
                            doc?.getLineNumber(offset)?.plus(1) ?: -1
                        }
                        buildJsonObject {
                            put("file_path", path)
                            put("line_num", lineNum)
                        }
                    }.distinctBy { it["file_path"].toString() }

                    mappedResults
                }

                call.respond(HttpStatusCode.OK, linkedFiles)
            }

            post("/data-flow"){
                print("trying to do data flow")
                val params = call.receive<RenameParams>()
                println("renaming ${params.oldName}@${params.lineNum} -> ${params.newName}")
                val renameObj = formRenameObject(params)
                if (renameObj==null){
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }
                val renameVar = (renameObj as RenameVariable)
                val oldClass = if (renameVar.getResolvedElement() is PsiClass){
                    renameVar.getResolvedElement() as PsiClass
                } else if (renameVar.oldVarPsi is PsiClass){
                    renameVar.oldVarPsi as PsiClass
                } else{
                    null
                }

                val result = if (oldClass!=null){
                    ClassReferenceFinder(oldClass, project)
                        .find()
                        .map {
                            DataFlowAnalyser.DataFlowAnalysisResult(file = it, depth = 1)
                        }
                } else {
                    val files = mutableSetOf<DataFlowAnalyser.DataFlowAnalysisResult>()
                    files.addAll(
                        DataFlowAnalyser(renameVar.oldVarPsi, project, false).analyse()
                    )
                    files.addAll(
                        DataFlowAnalyser(renameVar.oldVarPsi, project, true).analyse()
                    )
                    files.toList()
                }
                call.respond(HttpStatusCode.OK, result)

            }


        }
    }

    private suspend fun RoutingContext.responseToRenameWithMessage(params: RenameParams): Boolean {
        val reverse = RenameVariableFactory.fromOldNewNameAll(project, editor!!, file!!, params.newName, params.oldName)
        if (reverse.isNotEmpty()) {
            call.respond(
                HttpStatusCode.BadRequest, "The rename has already been performed. " +
                        "Variable ${params.newName} exists in this file. Variable ${params.oldName} does not exist."
            )
        } else if (params.lineNum != null) {
            call.respond(HttpStatusCode.BadRequest, "No matching variable/field at the given line number.")
        } else {
            call.respond(
                HttpStatusCode.BadRequest, "Could not rename ${params.oldName}. " +
                        "If the old_name is from an external library, it cannot be renamed."
            )
        }
        return true
    }

    private fun formRenameObject(params: RenameParams, useFile:PsiFile?=null): AbstractRefactoring? {
        val renameObjectRaw = RenameVariableFactory.fromOldNewNameAll(
            project, editor!!, useFile?:file!!, params.oldName, params.newName
        )
        if (renameObjectRaw.size==1)
            return renameObjectRaw[0]
        if (renameObjectRaw.isEmpty())
            return null

        // there are multiple elements which match that name in the file, need to find the right one.
        // filter by element type
        val elementsToInspect = if (params.codeElementType!=null) {
            val filtered = renameObjectRaw.filter {
                PsiUtils.isCodeElementType(
                    (it as RenameVariable).oldVarPsi, params.codeElementType
                )
            }
            if (filtered.size==1)
                return filtered[0]
            filtered.ifEmpty {
                renameObjectRaw
            }
        } else{renameObjectRaw}

        // filter by line number.
        if(params.lineNum!=null){
            val filtered = elementsToInspect.filter { it.startLoc == params.lineNum }
            return if(filtered.size==1) {
                filtered[0]
            } else {
                // return the best match
                elementsToInspect.sortedBy { abs(it.startLoc-params.lineNum) }[0]
            }
        }

        // return the first one because we have no line number
        return elementsToInspect[0]
    }

    private suspend fun RoutingContext.sanityChecks(params: RenameParams): Boolean {
        if (!SourceVersion.isName(params.oldName)) {
            call.respond(HttpStatusCode.BadRequest, message = "old name is not a valid identifier.")
            return true
        }

        if (!SourceVersion.isName(params.newName)) {
            call.respond(HttpStatusCode.BadRequest, message = "new name is not a valid identifier.")
            return true
        }

        if (params.oldName == params.newName) {
            call.respond(
                HttpStatusCode.OK,
                message = "No rename was performed. " +
                        "The old and new name in the request are the same.",
            )
            return true
        }
        return false
    }

    private fun reloadFileIfNeeded() {
        try{
            if (file == null)
                return
            else if (!file!!.isValid) // if the psi file got invalidated because of a rewrite, reload it's contents.
                file = PsiManager.getInstance(project).findFile(file!!.virtualFile)!!
        } catch (e: Exception){
            e.printStackTrace()
            println("Failed to reload file")
        }
    }

    private fun revertIfTestsFailed(testReport: String, oldContents: @NlsSafe String): String {
        val message = if (testReport != SUCCESS_MSG) {
            // Tests failed. need to roll back edits.
            FileUtils.replaceFileContents(
                Path(file!!.virtualFile.path),
                oldContents
            )
            VfsUtil.markDirtyAndRefresh(false, true, true, project.baseDir)
            "Your changes broke the semantics of the code. Tests failed. Please the report and fix your errors: $testReport"
        } else
            SUCCESS_MSG
        return message
    }

    fun stop(){
        println("Stopping refactoring server.")
    }

//    private fun searchInClass(psiClass: PsiClass, symbolName: String, results: MutableList<PsiElement>) {
//        // Check if the class itself matches
//        if (psiClass.name == symbolName) {
//            results.add(psiClass)
//        }
//
//        // Check methods in this class
//        psiClass.methods.forEach { method ->
//            if (method.name == symbolName) {
//                results.add(method)
//            }
//        }
//
//        // Check fields in this class
//        psiClass.fields.forEach { field ->
//            if (field.name == symbolName) {
//                results.add(field)
//            }
//        }
//    }

    // private fun searchInDirectory(directory: VirtualFile, symbolName: String, results: MutableList<PsiElement>) {
    //     try {
    //         directory.children.forEach { child ->
    //             if (child.isDirectory) {
    //                 searchInDirectory(child, symbolName, results)
    //             } else if (child.extension == "java") {
    //                 val psiFile = PsiManager.getInstance(project).findFile(child)
    //                 if (psiFile is PsiJavaFile) {
    //                     psiFile.classes.forEach { psiClass ->
    //                         searchInClass(psiClass, symbolName, results)
    //                     }
    //                 }
    //             }
    //         }
    //     } catch (e: Exception) {
    //         // Ignore errors for individual files/directories
    //     }
    // }
    
        private fun searchInClass(
        psiClass: PsiClass,
        symbolName: String,
        results: MutableList<PsiElement>
    ) {
        val project = psiClass.project
        val scope = GlobalSearchScope.allScope(project)

        // Class declaration + usages (case-insensitive)
        if (psiClass.name?.equals(symbolName, ignoreCase = true) == true) {
            results.add(psiClass)
            ReferencesSearch.search(psiClass, scope).allowParallelProcessing()
                .forEach(Processor<PsiReference> { ref ->
                    results.add(ref.element)
                    true
                })
        }

        // Method declaration + usages (case-insensitive)
        psiClass.methods.forEach { method ->
            if (method.name?.equals(symbolName, ignoreCase = true) == true) {
                results.add(method)
                ReferencesSearch.search(method, scope).allowParallelProcessing()
                    .forEach(Processor<PsiReference> { ref ->
                        results.add(ref.element)
                        true
                    })
            }
            
            // Search for local variables and method calls within this method
            searchInMethod(method, symbolName, results)
        }

        // Field declaration + usages (case-insensitive)
        psiClass.fields.forEach { field ->
            if (field.name?.equals(symbolName, ignoreCase = true) == true) {
                results.add(field)
                ReferencesSearch.search(field, scope).allowParallelProcessing()
                    .forEach(Processor<PsiReference> { ref ->
                        results.add(ref.element)
                        true
                    })
            }
        }
    }
    
    private fun extractChangedPart(oldName: String, newName: String): String {
        println("Debug: Comparing '$oldName' → '$newName'")
        
        // Find the longest common prefix
        val commonPrefix = findCommonPrefix(oldName, newName)
        println("Debug: Common prefix: '$commonPrefix'")
        
        // Find the longest common suffix
        val commonSuffix = findCommonSuffix(oldName, newName)
        println("Debug: Common suffix: '$commonSuffix'")
        
        // Extract the changed part (what's between prefix and suffix in old name)
        val changedPart = if (commonPrefix.length + commonSuffix.length < oldName.length) {
            val start = commonPrefix.length
            val end = oldName.length - commonSuffix.length
            oldName.substring(start, end)
        } else {
            // If no clear pattern, return the entire old name
            oldName
        }
        
        // If the changed part is too short or common, use a more specific part
        val finalChangedPart = if (changedPart.length < 6 || isCommonWord(changedPart)) {
            // Use a longer, more specific part
            if (commonSuffix.isNotEmpty()) {
                // Include part of the suffix to make it more specific
                val specificPart = changedPart + commonSuffix.take(3)
                println("Debug: Using more specific part: '$specificPart' (original was too common)")
                specificPart
            } else {
                // If no suffix, use the entire old name
                println("Debug: Using entire old name: '$oldName' (changed part was too common)")
                oldName
            }
        } else {
            changedPart
        }
        
        println("Debug: Final changed part: '$finalChangedPart'")
        return finalChangedPart.lowercase() // Return lowercase for case-insensitive search
    }

    private fun isCommonWord(word: String): Boolean {
        val commonWords = setOf(
                "execution", "config", "user", "data", "test", "manager", "service",
                "util", "helper", "factory", "builder", "handler", "processor",
                "controller", "repository", "entity", "model", "view", "form",
                "request", "response", "client", "server", "api", "impl"
        )
        return commonWords.contains(word.lowercase())
    }
    
    private fun findCommonPrefix(str1: String, str2: String): String {
        val minLength = minOf(str1.length, str2.length)
        for (i in 0 until minLength) {
            if (str1[i] != str2[i]) {
                return str1.substring(0, i)
            }
        }
        return str1.substring(0, minLength)
    }
    
    private fun findCommonSuffix(str1: String, str2: String): String {
        val minLength = minOf(str1.length, str2.length)
        for (i in 1..minLength) {
            if (str1[str1.length - i] != str2[str2.length - i]) {
                return str1.substring(str1.length - i + 1)
            }
        }
        return str1.substring(str1.length - minLength)
    }
    
    private fun searchInClassContains(
        psiClass: PsiClass,
        changedPart: String,
        results: MutableList<PsiElement>
    ) {
        val project = psiClass.project
        val scope = GlobalSearchScope.allScope(project)

        // Class declaration + usages (contains matching)
        if (psiClass.name?.contains(changedPart, ignoreCase = true) == true) {
            results.add(psiClass)
            ReferencesSearch.search(psiClass, scope).allowParallelProcessing()
                .forEach(Processor<PsiReference> { ref ->
                    results.add(ref.element)
                    true
                })
        }

        // Method declaration + usages (contains matching)
        psiClass.methods.forEach { method ->
            if (method.name.contains(changedPart, ignoreCase = true)) {
                results.add(method)
                ReferencesSearch.search(method, scope).allowParallelProcessing()
                    .forEach(Processor<PsiReference> { ref ->
                        results.add(ref.element)
                        true
                    })
            }
            
            // Search for local variables and method calls within this method (contains matching)
            searchInMethodContains(method, changedPart, results)
        }

        // Field declaration + usages (contains matching)
        psiClass.fields.forEach { field ->
            if (field.name.contains(changedPart, ignoreCase = true)) {
                results.add(field)
                ReferencesSearch.search(field, scope).allowParallelProcessing()
                    .forEach(Processor<PsiReference> { ref ->
                        results.add(ref.element)
                        true
                    })
            }
        }
    }
    
    private fun searchInMethodContains(
        method: PsiMethod,
        changedPart: String,
        results: MutableList<PsiElement>
    ) {
        try {
            // Use JavaRecursiveElementVisitor to traverse all elements in the method
            method.accept(object : JavaRecursiveElementVisitor() {
                override fun visitLocalVariable(localVariable: PsiLocalVariable) {
                    super.visitLocalVariable(localVariable)
                    if (localVariable.name?.contains(changedPart, ignoreCase = true) == true) {
                        results.add(localVariable)
                    }
                }
                
                override fun visitReferenceExpression(referenceExpression: PsiReferenceExpression) {
                    super.visitReferenceExpression(referenceExpression)
                    if (referenceExpression.referenceName?.contains(changedPart, ignoreCase = true) == true) {
                        results.add(referenceExpression)
                    }
                }
                
                override fun visitMethodCallExpression(methodCall: PsiMethodCallExpression) {
                    super.visitMethodCallExpression(methodCall)
                    if (methodCall.methodExpression.referenceName?.contains(changedPart, ignoreCase = true) == true) {
                        results.add(methodCall)
                    }
                }
            })
        } catch (e: Exception) {
            // Ignore errors for individual method processing
            println("Debug: Error searching in method ${method.name}: ${e.message}")
        }
    }
    
    private fun searchInDirectoryContains(
        directory: VirtualFile,
        changedPart: String,
        results: MutableList<PsiElement>
    ) {
        try {
            directory.children.forEach { child ->
                if (child.isDirectory) {
                    searchInDirectoryContains(child, changedPart, results)
                } else if (child.extension == "java") {
                    val psiFile = PsiManager.getInstance(project).findFile(child)
                    if (psiFile is PsiJavaFile) {
                        psiFile.classes.forEach { psiClass ->
                            searchInClassContains(psiClass, changedPart, results)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore errors for individual files/directories
            println("Debug: Error searching directory ${directory.path}: ${e.message}")
        }
    }
    
    private fun searchInClassContainsLimited(
        psiClass: PsiClass,
        changedPart: String,
        results: MutableList<PsiElement>,
        maxResults: Int,
        resultCountSetter: (Int) -> Unit
    ) {
        val project = psiClass.project
        val scope = GlobalSearchScope.allScope(project)
        var currentCount = results.size

        // Class declaration + usages (contains matching)
        if (currentCount < maxResults && psiClass.name?.contains(changedPart, ignoreCase = true) == true) {
            results.add(psiClass)
            currentCount++
            resultCountSetter(currentCount)
            
            if (currentCount < maxResults) {
                ReferencesSearch.search(psiClass, scope).allowParallelProcessing()
                    .forEach(Processor<PsiReference> { ref ->
                        if (currentCount < maxResults) {
                            results.add(ref.element)
                            currentCount++
                            resultCountSetter(currentCount)
                        }
                        currentCount < maxResults
                    })
            }
        }

        // Method declaration + usages (contains matching)
        psiClass.methods.forEach { method ->
            if (currentCount >= maxResults) return@forEach
            
            if (method.name.contains(changedPart, ignoreCase = true)) {
                results.add(method)
                currentCount++
                resultCountSetter(currentCount)
                
                if (currentCount < maxResults) {
                    ReferencesSearch.search(method, scope).allowParallelProcessing()
                        .forEach(Processor<PsiReference> { ref ->
                            if (currentCount < maxResults) {
                                results.add(ref.element)
                                currentCount++
                                resultCountSetter(currentCount)
                            }
                            currentCount < maxResults
                        })
                }
            }
            
            // Search for local variables and method calls within this method (contains matching)
            if (currentCount < maxResults) {
                searchInMethodContainsLimited(method, changedPart, results, maxResults, resultCountSetter)
                currentCount = results.size
            }
        }

        // Field declaration + usages (contains matching)
        psiClass.fields.forEach { field ->
            if (currentCount >= maxResults) return@forEach
            
            if (field.name.contains(changedPart, ignoreCase = true)) {
                results.add(field)
                currentCount++
                resultCountSetter(currentCount)
                
                if (currentCount < maxResults) {
                    ReferencesSearch.search(field, scope).allowParallelProcessing()
                        .forEach(Processor<PsiReference> { ref ->
                            if (currentCount < maxResults) {
                                results.add(ref.element)
                                currentCount++
                                resultCountSetter(currentCount)
                            }
                            currentCount < maxResults
                        })
                }
            }
        }
    }
    
    private fun searchInMethodContainsLimited(
        method: PsiMethod,
        changedPart: String,
        results: MutableList<PsiElement>,
        maxResults: Int,
        resultCountSetter: (Int) -> Unit
    ) {
        try {
            var currentCount = results.size
            // Use JavaRecursiveElementVisitor to traverse all elements in the method
            method.accept(object : JavaRecursiveElementVisitor() {
                override fun visitLocalVariable(localVariable: PsiLocalVariable) {
                    super.visitLocalVariable(localVariable)
                    if (currentCount < maxResults && localVariable.name?.contains(changedPart, ignoreCase = true) == true) {
                        results.add(localVariable)
                        currentCount++
                        resultCountSetter(currentCount)
                    }
                }
                
                override fun visitReferenceExpression(referenceExpression: PsiReferenceExpression) {
                    super.visitReferenceExpression(referenceExpression)
                    if (currentCount < maxResults && referenceExpression.referenceName?.contains(changedPart, ignoreCase = true) == true) {
                        results.add(referenceExpression)
                        currentCount++
                        resultCountSetter(currentCount)
                    }
                }
                
                override fun visitMethodCallExpression(methodCall: PsiMethodCallExpression) {
                    super.visitMethodCallExpression(methodCall)
                    if (currentCount < maxResults && methodCall.methodExpression.referenceName?.contains(changedPart, ignoreCase = true) == true) {
                        results.add(methodCall)
                        currentCount++
                        resultCountSetter(currentCount)
                    }
                }
            })
        } catch (e: Exception) {
            // Ignore errors for individual method processing
            println("Debug: Error searching in method ${method.name}: ${e.message}")
        }
    }
    
    private fun searchInDirectoryContainsLimited(
        directory: VirtualFile,
        changedPart: String,
        results: MutableList<PsiElement>,
        maxResults: Int,
        resultCountSetter: (Int) -> Unit
    ) {
        try {
            directory.children.forEach { child ->
                if (results.size >= maxResults) return@forEach
                
                if (child.isDirectory) {
                    searchInDirectoryContainsLimited(child, changedPart, results, maxResults, resultCountSetter)
                } else if (child.extension == "java") {
                    val psiFile = PsiManager.getInstance(project).findFile(child)
                    if (psiFile is PsiJavaFile) {
                        psiFile.classes.forEach { psiClass ->
                            if (results.size < maxResults) {
                                searchInClassContainsLimited(psiClass, changedPart, results, maxResults, resultCountSetter)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore errors for individual files/directories
            println("Debug: Error searching directory ${directory.path}: ${e.message}")
        }
    }
    
    private fun searchInMethod(
        method: PsiMethod,
        symbolName: String,
        results: MutableList<PsiElement>
    ) {
        try {
            // Use JavaRecursiveElementVisitor to traverse all elements in the method
            method.accept(object : JavaRecursiveElementVisitor() {
                override fun visitLocalVariable(localVariable: PsiLocalVariable) {
                    super.visitLocalVariable(localVariable)
                    if (localVariable.name?.equals(symbolName, ignoreCase = true) == true) {
                        results.add(localVariable)
                    }
                }
                
                override fun visitReferenceExpression(referenceExpression: PsiReferenceExpression) {
                    super.visitReferenceExpression(referenceExpression)
                    if (referenceExpression.referenceName?.equals(symbolName, ignoreCase = true) == true) {
                        results.add(referenceExpression)
                    }
                }
                
                override fun visitMethodCallExpression(methodCall: PsiMethodCallExpression) {
                    super.visitMethodCallExpression(methodCall)
                    if (methodCall.methodExpression.referenceName?.equals(symbolName, ignoreCase = true) == true) {
                        results.add(methodCall)
                    }
                }
            })
        } catch (e: Exception) {
            // Ignore errors for individual method processing
            println("Debug: Error searching in method ${method.name}: ${e.message}")
        }
    }
    
    
    private fun searchInDirectory(
        directory: VirtualFile,
        symbolName: String,
        results: MutableList<PsiElement>
    ) {
        try {
            directory.children.forEach { child ->
                if (child.isDirectory) {
                    searchInDirectory(child, symbolName, results)
                } else if (child.extension == "java") {
                    val psiFile = PsiManager.getInstance(project).findFile(child)
                    if (psiFile is PsiJavaFile) {
                        psiFile.classes.forEach { psiClass ->
                            searchInClass(psiClass, symbolName, results)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore errors for individual files/directories
            println("Debug: Error searching directory ${directory.path}: ${e.message}")
        }
    }
     

}