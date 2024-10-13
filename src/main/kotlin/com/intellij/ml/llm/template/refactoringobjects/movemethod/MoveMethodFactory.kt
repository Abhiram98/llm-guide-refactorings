package com.intellij.ml.llm.template.refactoringobjects.movemethod

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import com.intellij.ml.llm.template.intentions.ApplySuggestRefactoringIntention
import com.intellij.ml.llm.template.models.LLMBaseResponse
import com.intellij.ml.llm.template.models.sendChatRequest
import com.intellij.ml.llm.template.prompts.MoveMethodRefactoringPrompt
import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.refactoringobjects.MyRefactoringFactory
import com.intellij.ml.llm.template.telemetry.EFTelemetryDataManager
import com.intellij.ml.llm.template.utils.JsonUtils
import com.intellij.ml.llm.template.utils.PsiUtils
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.refactoring.move.MoveCallback
import com.intellij.refactoring.move.MoveInstanceMembersUtil
import com.intellij.refactoring.move.moveInstanceMethod.MoveInstanceMethodDialog
import com.intellij.refactoring.move.moveInstanceMethod.MoveInstanceMethodProcessor
import com.intellij.refactoring.move.moveMembers.MoveMembersDialog
import com.intellij.refactoring.openapi.impl.JavaRefactoringFactoryImpl
import com.intellij.refactoring.suggested.startOffset
import com.intellij.usageView.UsageInfo
import dev.langchain4j.model.chat.ChatLanguageModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import kotlin.math.min
import kotlin.system.measureTimeMillis
import org.jetbrains.kotlin.idea.base.codeInsight.handlers.fixers.endLine
import org.jetbrains.kotlin.idea.base.codeInsight.handlers.fixers.startLine
import org.jetbrains.kotlin.idea.refactoring.showWithTransaction


class MoveMethodFactory {


    data class MovePivot(val psiClass: PsiClass, val psiElement: PsiElement?, var rationale: String? = null)
    data class MoveSuggestion(
        @SerializedName("target_class")
        val targetClassName: String,
        @SerializedName("rationale")
        val rationale: String
    )
    companion object: MyRefactoringFactory{

        const val TOPN_SUGGESTIONS4USER = 1
        const val TOPN_SUGGESTIONS4LLM = 5
        const val llmContextLimit = 128000
        val llmResponseCache = mutableMapOf<String, LLMBaseResponse>()
        var myInvokeFinished: Boolean? = null
        const val MAX_TARGET_CLASS4LLM = 3

        fun test(){

        }

        override fun createObjectsFromFuncCall(
            funcCall: String,
            project: Project,
            editor: Editor,
            file: PsiFile
        ): List<AbstractRefactoring> {

            val params = getParamsFromFuncCall(funcCall)
            val methodName = getStringFromParam(params[0])
            val targetVariable = getStringFromParam(params[1])

            return createMoveMethodFromName(editor, file, methodName, project)
        }

        fun createMoveMethodFromName(
            editor: Editor,
            file: PsiFile,
            methodName: String,
            project: Project,
            llmChatModel: ChatLanguageModel? = null,
            telemetyDataManager: EFTelemetryDataManager? =null
        ): List<AbstractRefactoring> {
            val outerClass: PsiElement? =
                runReadAction {
                    PsiUtils.getParentClassOrNull(editor, language = file.language) ?: file.getChildOfType<PsiClass>()
                }
            val methodToMove =
                runReadAction { PsiUtils.getMethodNameFromClass(outerClass, methodName) } ?: return listOf()

            return createMoveMethodRefactorings(project, methodToMove, editor, file, llmChatModel, telemetyDataManager)
        }

        fun createMoveMethodFromPsiMethod(
            editor: Editor,
            file: PsiFile,
            methodPsi: PsiMethod,
            project: Project,
            llmChatModel: ChatLanguageModel? = null,
            telemetyDataManager: EFTelemetryDataManager? =null
        ): List<AbstractRefactoring> {
            return createMoveMethodRefactorings(project, methodPsi, editor, file, llmChatModel, telemetyDataManager)
        }

        private fun createMoveMethodRefactorings(
            project: Project,
            methodToMove: PsiMethod,
            editor: Editor,
            file: PsiFile,
            llmChatModel: ChatLanguageModel?,
            telemetryDataManager: EFTelemetryDataManager?
        ): List<AbstractRefactoring> {

            val validMovePivots = getPotentialMovePivots(project, editor, file, methodToMove) // these pivots are also valid.
            val targetPivotsWithSimilarity: List<Pair<MovePivot, Double>>
            val similarityComputationTime = measureTimeMillis {
                targetPivotsWithSimilarity =
                    validMovePivots
                        .filter { runReadAction{ methodToMove.containingClass?.qualifiedName != it.psiClass.qualifiedName } }
                        .map { pivot ->
                            val similarity = runReadAction{ PsiUtils.computeCosineSimilarity(methodToMove, pivot.psiClass) }
//                            val cbSimilarity = runReadAction{ CodeBertScore.computeCodeBertScore(methodToMove, pivot.psiClass) }
                            pivot to similarity
                        }

            }
            val targetPivotsSorted = runReadAction{
                targetPivotsWithSimilarity
                    .sortedByDescending { it.second }
                    .distinctBy { it.first.psiClass.name + it.first.psiElement?.text }
                    .map { it.first }
                    .subList(0, min(targetPivotsWithSimilarity.size, MAX_TARGET_CLASS4LLM))
            }
            telemetryDataManager
                ?.addPotentialTargetClassesOrdered(
                    methodToMove.name,
                    runReadAction{ targetPivotsWithSimilarity.map { it.first.psiClass.name to it.second } },
                    "cosine",
                    similarityComputationTime)

            val pivotsSortedByLLM =
                if (llmChatModel!=null)
                    rerankByLLM(targetPivotsSorted,
                        methodToMove,
                        runReadAction{ methodToMove.containingClass },
                        project,
                        llmChatModel,
                        telemetryDataManager)?.filter { it.rationale!=null } ?: targetPivotsSorted
                else
                    targetPivotsSorted
            if (pivotsSortedByLLM.isEmpty())
                return emptyList()
            logPotentialPivots(pivotsSortedByLLM.subList(0, min(3, pivotsSortedByLLM.size)), methodToMove)

            if (PsiUtils.isMethodStatic(methodToMove)){
                return runReadAction {
                    val moveMethods = pivotsSortedByLLM.map {
                        it.psiClass.qualifiedName?.let { it1 ->
                            val classToMoveTo = JavaPsiFacade.getInstance(project).findClass(it1, GlobalSearchScope.projectScope(project))
                            if (classToMoveTo != null) {
                                MyMoveStaticMethodRefactoring(
                                    methodToMove.startLine(editor.document),
                                    methodToMove.endLine(editor.document),
                                    methodToMove, classToMoveTo,
                                    rationale = it.rationale
                                )
                            }
                            else{
                                null
                            }
                        }
                    }.filterNotNull()

                    if (moveMethods.isEmpty())
                        return@runReadAction emptyList()
                    return@runReadAction moveMethods.subList(0, min(TOPN_SUGGESTIONS4USER, moveMethods.size))
                }
            }

            val moveMethods = pivotsSortedByLLM
                .map {
                    if (it.psiElement!=null) {
//                        val processor = runReadAction {
//                            MoveInstanceMethodProcessor(
//                                project, methodToMove, it.psiElement as PsiVariable, "public",
//                                runReadAction {
//                                    getParamNamesIfNeeded(
//                                        MoveInstanceMembersUtil.getThisClassesToMembers(methodToMove),
//                                        it.psiElement as? PsiField
//                                    )
//                                }
//                            )
//                        }
                        MyMoveInstanceMethodRefactoring(
                            methodToMove.startLine(editor.document),
                            methodToMove.endLine(editor.document),
                            methodToMove,
                            it.psiElement as PsiVariable,
                            classToMoveTo = it.psiClass,
                            rationale = it.rationale
                        )
                    }else {
                        null
                    }
                }
                .filterNotNull()
            if (moveMethods.isEmpty())
                return emptyList()
            return moveMethods.subList(0, min(TOPN_SUGGESTIONS4USER, moveMethods.size)) // choose top-3 moves
        }

        private fun getValidPivots(
            project: Project,
            editor: Editor,
            file: PsiFile,
            methodToMove: PsiMethod,
            targetPivots: List<MovePivot>
        ): List<MovePivot> {
            return targetPivots.filter {
                if (PsiUtils.isMethodStatic(methodToMove))
                    checkStaticMoveValidity(project, methodToMove, it)
                else
                    checkInstanceMoveValidity(project, methodToMove, it)
            }
        }

        private fun checkInstanceMoveValidity(
            project: Project,
            methodToMove: PsiMethod,
            it: MovePivot
        ): Boolean {
            return runReadAction {
                val processor = MoveInstanceMethodProcessorAutoValidator(
                    project, methodToMove, it.psiElement as PsiVariable, "public",
                    false,
                    runReadAction {
                        getParamNamesIfNeeded(
                            MoveInstanceMembersUtil.getThisClassesToMembers(methodToMove),
                            it.psiElement as? PsiField
                        )
                    }
                )
                val refUsages = Ref<Array<UsageInfo>>(processor.delegateFindUsages())
                return@runReadAction processor.delegatePreprocessUsages(refUsages)
            }
        }

        private fun logPotentialPivots(
            targetPivotsSorted: List<MovePivot>,
            methodToMove: PsiMethod
        ) {
            ApplySuggestRefactoringIntention.log2fileAndViewer(
                "Found potential target class(s) for ${methodToMove.name}", Logger.getInstance(this::class.java))
            ApplySuggestRefactoringIntention.log2fileAndViewer(
                "Potential Target Classes -> ${
                    targetPivotsSorted.distinctBy { it.psiClass.name }.map{it.psiClass.name}.joinToString(", ")
                }",
                Logger.getInstance(this::class.java)
            )
        }

        private fun rerankByLLM(
            targetPivotsSorted: List<MovePivot>,
            methodToMove: PsiMethod,
            containingClass: PsiClass?,
            project: Project,
            llmChatModel: ChatLanguageModel,
            telemetryDataManager: EFTelemetryDataManager?
        ) : List<MovePivot>?{

            val response: LLMBaseResponse?
            val methodText = runReadAction{ methodToMove.text }
            val potentialClassBodies = runReadAction {
                targetPivotsSorted.mapNotNull { pivot ->
                    pivot.psiClass.text
                }
            }

            val llmResponseTime = measureTimeMillis {
                response = llmResponseCache[methodText] ?: sendChatRequest(
                    project,
                    MoveMethodRefactoringPrompt().askForTargetClassPriorityPrompt(
                        methodText,
                        potentialClassBodies.joinToString("\n"),
                        targetPivotsSorted.distinctBy { it.psiClass.name }
                    ),
                    llmChatModel)
            }
            if (response!=null){
                llmResponseCache[methodText]?: llmResponseCache.put(runReadAction{ methodText }, response)
                try {
                    val priorityOrder =
                        (JsonParser.parseString(JsonUtils.sanitizeJson(response.getSuggestions()[0].text)) as JsonArray)
                        .map { try{ Gson().fromJson(it, MoveSuggestion::class.java) } catch (e: Exception){null} }
                        .filterNotNull()
                    val targetClassesRanked = priorityOrder.map { it.targetClassName }
                    return runReadAction {
                        priorityOrder.forEach { moveSuggestion ->
                            targetPivotsSorted
                                .filter { it.psiClass.name == moveSuggestion.targetClassName }
                                .forEach { it.rationale = moveSuggestion.rationale }
                        }

                        val llmSortedPivots = targetPivotsSorted.sortedBy {
                            val index = targetClassesRanked.indexOf(it.psiClass.name)
                            if (index == -1) {
                                targetPivotsSorted.size + 1
                            } else {
                                index
                            }
                        }
                        telemetryDataManager?.addLlmTargetClassPriorityResponse(
                            methodToMove.name,
                            llmSortedPivots.map { it.psiClass.name }.filterNotNull(),
                            llmResponseTime
                        )

                        return@runReadAction llmSortedPivots
                    }
                } catch (e: Exception) {
                    runReadAction {
                        telemetryDataManager?.addLlmTargetClassPriorityResponse(
                            methodToMove.name,
                            response.getSuggestions()[0].text,
                            llmResponseTime
                        )
                        e.printStackTrace()
                    }
                }
            }
            return null
        }

        private fun getPotentialMovePivots(project: Project, editor: Editor, file: PsiFile, methodToMove: PsiMethod): List<MovePivot> {
            if (runReadAction{ methodToMove.containingClass } ==null) return emptyList()
            if (PsiUtils.isMethodStatic(methodToMove)
                && runReadAction {
                    MoveMembersPreConditions.checkPreconditions(project, arrayOf(methodToMove), null, null)
                }){
//                return runReadAction {
//                    (PsiUtils.fetchClassesInProject(
//                        methodToMove.containingClass!!,
//                        project
//                    ))
//                        .map { MovePivot(it, null) }
//                }

                val potentialTargets = runReadAction {
                    (PsiUtils.fetchImportsInFile(file, project) +
                            PsiUtils.fetchPrioritizedClasses(methodToMove.containingClass!!, project))}
                val dedupPotentialTargets = potentialTargets.distinctBy { it.name }
                val potentialMovePivots = dedupPotentialTargets.map { MovePivot(it, null) }
                val validMovePivots = getValidPivots(project, editor, file, methodToMove, potentialMovePivots)
                return validMovePivots.subList(0, min(validMovePivots.size, 100)).map { MovePivot(it.psiClass, null) }
            }else{
                val handler = MoveInstanceMethodHandlerForPlugin()
                val potentialPivots = runReadAction {
                    handler.invoke(project, arrayOf(methodToMove), null)
                    handler.suitableVariablesToMove.map {
                        val clazz = PsiUtils.findClassFromQualifier(it.type.canonicalText, project)
                        if (clazz != null)
                            MovePivot(clazz, it)
                        else
                            null
                    }.filterNotNull()
                }
                return getValidPivots(project, editor, file, methodToMove, potentialPivots)
            }
        }

        fun tryMoveToClass(
            methodToMove: PsiMethod,
            targetClassName: String,
            project: Project,
            editor: Editor,
            file: PsiFile
        ): List<AbstractRefactoring>{

            if (PsiUtils.isMethodStatic(methodToMove)) {
                val qualifiedClassName = PsiUtils.getQualifiedTypeInFile(
                    methodToMove.containingFile, targetClassName
                )
                if (qualifiedClassName!=null){
                    return createStaticMove(methodToMove, editor, qualifiedClassName)
                }
            }else{
                val variableOfType = PsiUtils.getVariableOfType(methodToMove, targetClassName)
                if (variableOfType!=null){
//                    return createMoveMethodRefactorings(variableOfType, project, methodToMove, editor)
                }
            }

            return listOf()
        }

        fun createStaticMove(
            methodToMove: PsiMethod,
            editor: Editor,
            qualifiedClassName: String
        ): List<MyMoveStaticMethodRefactoring> {

            val classToMoveTo = JavaPsiFacade.getInstance(methodToMove.project)
                .findClass(qualifiedClassName, GlobalSearchScope.projectScope(methodToMove.project))
            if (classToMoveTo==null) return emptyList()

            return listOf(
                MyMoveStaticMethodRefactoring(
                    methodToMove.startLine(editor.document),
                    methodToMove.endLine(editor.document),
                    methodToMove, classToMoveTo
                )
            )
        }

        override val logicalName: String
            get() = "Move Method"
        override val apiFunctionName: String
            get() = "move_method"
        override val APIDocumentation: String
            get() = "def move_method(method_name, target_variable_name):\n" +
                    "    \"\"\"\n" +
                    "    Moves a method from its current class or context to a target class or object.\n" +
                    "\n" +
                    "    This function refactors code by moving a method identified by `method_name` from its original class or context\n" +
                    "    to a target object identified by `target_variable_name`. It assumes that the necessary updates to the\n" +
                    "    source code are handled externally.\n" +
                    "\n" +
                    "    Parameters:\n" +
                    "    - method_name (str): The name of the method to be moved.\n" +
                    "    - target_variable_name (str): The name of the target object to which the method should be moved.\n" +
                    "    \"\"\""



        class MyMoveInstanceMethodRefactoring(
            override val startLoc: Int,
            override val endLoc: Int,
            val methodToMove: PsiMethod,
            val psiVariable: PsiVariable,
            val classToMoveTo: PsiClass,
            val rationale: String? = null
        ) : AbstractRefactoring(){

            init {
                description = runReadAction{
                    "Move method ${methodToMove.name} to ${classToMoveTo.qualifiedName}\n" +
                            "Rationale: $rationale"
                }
            }
            override fun performRefactoring(project: Project, editor: Editor, file: PsiFile) {
                super.performRefactoring(project, editor, file)
                applied = false
                val moveDialog = MyMoveInstanceMethodDialog(methodToMove, arrayOf(psiVariable), MyMoveCallBack(this))
                moveDialog.show()
//                applied = moveDialog.triggeredRefactoring
                reverseRefactoring = getReverseRefactoringObject(project, editor, file)
            }

            override fun isValid(project: Project, editor: Editor, file: PsiFile): Boolean {
                isValid = methodToMove.isPhysical
                return isValid!!
            }

            override fun getRefactoringPreview(): String {
                return "Move method ${methodToMove.name} to class ${classToMoveTo.name}"
            }

            override fun getStartOffset(): Int {
                return methodToMove.startOffset
            }

            override fun getEndOffset(): Int {
                return methodToMove.endOffset
            }

            override fun getReverseRefactoringObject(
                project: Project,
                editor: Editor,
                file: PsiFile
            ): AbstractRefactoring? {
                TODO("Not yet implemented")
            }

            override fun recalibrateRefactoring(project: Project, editor: Editor, file: PsiFile): AbstractRefactoring? {
                if (isValid==true)
                    return this
                return null // TODO: implement search
            }

        }

        private fun checkStaticMoveValidity(project: Project,
                                            methodToMove: PsiMethod,
                                            movePivot: MovePivot) : Boolean {
            return runReadAction {
                if (methodToMove.containingClass!!.name==movePivot.psiClass.name)
                    return@runReadAction false
//                return@runReadAction true
                if (movePivot.psiClass.qualifiedName==null) return@runReadAction false
                val processor = MoveStaticMethodValidator(
                    project,
                    methodToMove.containingClass!!,
                    movePivot.psiClass,
                    methodToMove
                )
                val refUsages = Ref<Array<UsageInfo>>(processor.delegateFindUsages())
                return@runReadAction processor.delegatePreprocessUsages(refUsages)
            }
        }
        tailrec suspend fun waitForBackgroundFinish(maxDelay: Long, checkPeriod: Long) : Boolean{
            if(maxDelay < 0) return false
            if(myInvokeFinished==true) return true
            delay(checkPeriod)
            return waitForBackgroundFinish(maxDelay - checkPeriod, checkPeriod)
        }

        private fun getParamNamesIfNeeded(
            myThisClassesMap: Map<PsiClass, Set<PsiMember>>,
            targetVariable: PsiField?
        ): MutableMap<PsiClass, String> {
            var parameterNames: MutableMap<PsiClass, String> = mutableMapOf()
            for (aClass in myThisClassesMap.keys) {

                val members = myThisClassesMap[aClass]
                if (targetVariable==null || members == null || members.size != 1 || !members.contains(targetVariable)) {
                    parameterNames.put(aClass, aClass.name!!.lowercase())
                }
                //Just the field is referenced
                // Skip adding parameter
            }
            return parameterNames
        }

    }

    class MyMoveCallBack(val ref: AbstractRefactoring): MoveCallback {
        override fun refactoringCompleted() {
            ref.applied = true
        }
    }

    class MyMoveStaticMethodRefactoring(
        override val startLoc: Int,
        override val endLoc: Int,
        val methodToMove: PsiMethod,
        val classToMoveTo: PsiClass,
        val rationale: String?=null
    ) : AbstractRefactoring(){
        val sourceClass: PsiClass = methodToMove.containingClass!!
        val methodName = methodToMove.name
        init {
            description = "move method ${methodToMove.name} to $classToMoveTo\n" +
                    "Rationale: $rationale"
        }



        override fun performRefactoring(project: Project, editor: Editor, file: PsiFile) {
            super.performRefactoring(project, editor, file)
            applied = false
            val dialog = MoveMembersDialog(
                file.project, methodToMove.containingClass!!, classToMoveTo, setOf(methodToMove), MyMoveCallBack(this))
            dialog.show()
            reverseRefactoring = getReverseRefactoringObject(project, editor, file)
        }

        override fun isValid(project: Project, editor: Editor, file: PsiFile): Boolean {
            isValid = methodToMove.isPhysical
            return methodToMove.isPhysical
        }

        override fun getRefactoringPreview(): String {
            return "Move Static method ${methodToMove.name} to class ${classToMoveTo.name?.split(".")?.last()}"
        }

        override fun getStartOffset(): Int {
            return methodToMove.startOffset
        }

        override fun getEndOffset(): Int {
            return methodToMove.endOffset
        }

        override fun getReverseRefactoringObject(
            project: Project,
            editor: Editor,
            file: PsiFile
        ): AbstractRefactoring? {

            val foundMethod = PsiUtils.getMethodNameFromClass(classToMoveTo, methodName)
            if (foundMethod!=null)
                return MyMoveStaticMethodRefactoring(
                    foundMethod.startLine(editor.document),
                    foundMethod.endLine(editor.document),
                    foundMethod, sourceClass)
            return null
        }

        override fun recalibrateRefactoring(project: Project, editor: Editor, file: PsiFile): AbstractRefactoring? {
            if (isValid==true)
                return this
            return null // TODO: Impl search
        }

    }

}