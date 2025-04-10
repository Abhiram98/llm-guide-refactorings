package com.intellij.ml.llm.template.server

import com.intellij.analysis.AnalysisScope
import com.intellij.codeInspection.ProblemDescriptorBase
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.actions.CodeInspectionAction
import com.intellij.codeInspection.ex.GlobalInspectionContextImpl
import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.ProjectUtil
import com.intellij.ml.llm.template.agents.RefactoringTools
import com.intellij.ml.llm.template.refactoringobjects.IdeInspection
import com.intellij.ml.llm.template.refactoringobjects.extractclass.ExtractClassRefactoring
import com.intellij.ml.llm.template.refactoringobjects.extractclass.ExtractInterfaceRefactoring
import com.intellij.ml.llm.template.refactoringobjects.extractfunction.ExtractMethodFactory
import com.intellij.ml.llm.template.refactoringobjects.movemethod.MoveMethodFactory
import com.intellij.ml.llm.template.refactoringobjects.pullup.PullUpRefactoring
import com.intellij.ml.llm.template.refactoringobjects.pullup.PushDownRefactoring
import com.intellij.ml.llm.template.refactoringobjects.reformat.ReformatFile
import com.intellij.ml.llm.template.refactoringobjects.renamevariable.RenameVariableFactory
import com.intellij.ml.llm.template.testcuration.TestSelector
import com.intellij.ml.llm.template.utils.FileUtils
import com.intellij.ml.llm.template.utils.PsiUtils
import com.intellij.ml.llm.template.utils.openFileFromQualifiedName
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.PsiJavaFileImpl
import com.intellij.refactoring.suggested.startOffset
import com.intellij.ui.tree.TreePathUtil
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
import org.jetbrains.kotlin.idea.base.codeInsight.handlers.fixers.startLine
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import java.nio.file.Path
import javax.swing.SwingUtilities
import javax.swing.SwingUtilities.invokeAndWait
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.math.abs


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

    fun Application.myApplicationModule() {
        install(ContentNegotiation) {
            json()
        }
        routing {
            get("/") {
                call.respondText("Hello, world!", ContentType.Text.Html)
            }

            get("/get_source_code"){
                call.respond(HttpStatusCode.OK, message = file!!.text)
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

                val response = runBlocking {  projectListener.waitForFinish() }
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
                    ProjectUtil.focusProjectWindow(project, true)
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

            post("/rename") {
                println("got a request")
                try {
                    val params = call.receive<RenameParams>()
                    println("renaming ${params.oldName}@${params.lineNum} -> ${params.newName}")

                    // Call IJ rename API here.
                    val renameObject = RenameVariableFactory.fromOldNewNameAll(
                        project, editor!!, file!!, params.oldName, params.newName)
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
                        invokeAndWait { refObj.performRefactoring(project, editor!!, file!!) }
                        call.respond(HttpStatusCode.OK, message = SUCCESS_MSG)
                        return@post
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
            post("/extract_method"){
                try {
                    val params = call.receive<ExtractMethodParams>()
                    println("extracting lines ${params.startLine} -> ${params.endLine}: ${params.newName}")

                    // Call IJ rename API here.
                    val refObjs = ExtractMethodFactory.fromStartEndLine(editor!!, file!!, params.startLine, params.endLine, params.newName)
                    if (refObjs.isNotEmpty()) {
                        var failedException: Exception? = null
                        invokeAndWait{
                            try{ refObjs[0].performRefactoring(project, editor!!, file!!) }
                            catch(ex: Exception){
                                failedException = ex
                            }
                        }
                        if (failedException==null)
                            call.respond(HttpStatusCode.OK, message = SUCCESS_MSG)
                        else
                            call.respond(message = failedException!!.message.toString(), status = HttpStatusCode.BadRequest)
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

                    val moveMethodObjects = MoveMethodFactory.createMoveMethodFromName(editor!!, file!!, project, params.methodName, params.targetClass)
                    if (moveMethodObjects.isNotEmpty()){
                        invokeAndWait{ moveMethodObjects[0].performRefactoring(project, editor!!, file!!) }
                        call.respond(HttpStatusCode.OK, message = SUCCESS_MSG)
                    }
                    else
                        call.respond(HttpStatusCode.NoContent, message = "could not create a refactoring object. " +
                                "Please check that you have not selected the entire body of a method")
                } catch (ex: IllegalStateException) {
                    call.respond(HttpStatusCode.BadRequest, message = "invalid parameters.")
                } catch (ex: JsonConvertException) {
                    call.respond(HttpStatusCode.BadRequest, message = "invalid parameters.")
                }
            }
            post("extract-class"){
                val params = call.receive<ExtractClassParams>()
                val psiClass = (file as PsiJavaFileImpl).classes[0]

                val refObj = if (params.extractInterface)
                    ExtractInterfaceRefactoring.createFromMembers(psiClass, params.members, params.newName, params.subClassName)
                else{
                    ExtractClassRefactoring.createFromMembers(psiClass, params.members, params.newName, params.subClassName)
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
                        call.respond(HttpStatusCode.NotFound, message = "no method with that name was found.")
                    else if (matches.size > 1 && params.lineNum==null)
                        call.respond(HttpStatusCode.BadRequest,
                            message = "Too many methods (${matches.size}) have that name. " +
                                "Please identify the method from it's line number.")

                    val methodPsi = matches.sortedBy { abs(it.startLine(editor!!.document) - params.lineNum!!) }.first()

                    val oldContents = runReadAction{ editor!!.document.text }
                    FileUtils.replaceFileContentsInRange(
                        Path(file!!.virtualFile.path),
                        methodPsi.startOffset, methodPsi.endOffset,
                        params.newContent
                    )
                    VfsUtil.markDirtyAndRefresh(false, true, true, project.baseDir)
                    SwingUtilities.invokeAndWait {
                        ReformatFile.doReformat(
                            file!!,
                            methodPsi.startOffset,
                            methodPsi.startOffset + params.newContent.length
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

            post("run_code_inspection"){
                val inspection = IdeInspection(project, AnalysisScope(file!!), file!!)
                invokeAndWait{ inspection.doInspect() }

//                        view.tree.root // traverse tree and find problems.
                inspection.waitForCompletion()
                call.respond(HttpStatusCode.OK, message = inspection.problems.toString())
                // Read and return results.
            }

        }
    }

    private fun reloadFileIfNeeded() {
        if (!file!!.isValid) // if the psi file got invalidated because of a rewrite, reload it's contents.
            file = PsiManager.getInstance(project).findFile(file!!.virtualFile)!!
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

}