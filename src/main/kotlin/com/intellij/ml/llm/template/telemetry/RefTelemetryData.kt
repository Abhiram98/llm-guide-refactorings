package com.intellij.ml.llm.template.telemetry

import com.google.gson.annotations.SerializedName
import com.intellij.ml.llm.template.intentions.ApplyMoveMethodInteractiveIntention
import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.refactoringobjects.UncreatableRefactoring
import com.intellij.ml.llm.template.refactoringobjects.extractfunction.EfCandidateType
import com.intellij.ml.llm.template.settings.RefAgentSettingsManager
import com.intellij.ml.llm.template.utils.EFCandidateApplicationPayload
import com.intellij.ml.llm.template.utils.PsiUtils
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.elementType
import org.jetbrains.kotlin.psi.psiUtil.elementsInRange
import java.util.*

data class RefTelemetryData(
    @SerializedName("id")
    var id: String,
) {
    @SerializedName("hostFunctionTelemetryData")
    lateinit var hostFunctionTelemetryData: HostFunctionTelemetryData

    @SerializedName("candidatesTelemetryData")
    lateinit var candidatesTelemetryData: RefCandidatesTelemetryData

    @SerializedName("userSelectionTelemetryData")
    lateinit var userSelectionTelemetryData: EFUserSelectionTelemetryData

    @SerializedName("elapsedTime")
    lateinit var elapsedTime: List<CandidateElapsedTimeTelemetryData>

    @SerializedName("processingTime")
    lateinit var processingTime: EFTelemetryDataProcessingTime

    @SerializedName("iterationData")
    var iterationData: MutableList<MoveMethodIterationData> = mutableListOf()

    @SerializedName("methodCompatibilityScores")
    var methodCompatibility: MutableMap<String, Pair<ApplyMoveMethodInteractiveIntention.MoveMethodSuggestion, Double>> = mutableMapOf()

    @SerializedName("llmMethodPriority")
    lateinit var llmPriority: LlmMovePriority

    @SerializedName("targetClassMap")
    var targetClassMap: MutableMap<String, TargetClass4Method> = mutableMapOf()

    @Transient
    lateinit var refactoringObjects: List<AbstractRefactoring>
}

data class HostFunctionTelemetryData(
    @SerializedName("hostFunctionSize")
    var hostFunctionSize: Int,

    @SerializedName("lineStart")
    var lineStart: Int,

    @SerializedName("lineEnd")
    var lineEnd: Int,

    @SerializedName("bodyLineStart")
    var bodyLineStart: Int,

    @SerializedName("language")
    var language: String,

    @SerializedName("filePath")
    var filePath: String,

    @SerializedName("sourceCode")
    var sourceCode: String,

    @SerializedName("methodCount")
    var methodCount: Int
)

data class RefCandidatesTelemetryData(
    @SerializedName("numberOfSuggestions")
    var numberOfSuggestions: Int,

    @SerializedName("candidates")
    var candidates: List<RefCandidateTelemetryData>,
)

data class RefCandidateTelemetryData(
    @SerializedName("lineStart")
    var lineStart: Int,

    @SerializedName("lineEnd")
    var lineEnd: Int,

    @SerializedName("refactoringType")
    var refactoringType: String,

    @SerializedName("refactoringInfo")
    var refactoringInformation: String,

    @SerializedName("description")
    var description: String,

    @SerializedName("couldCreateRefObject")
    var couldCreateRefObject: Boolean?=null,

    @SerializedName("valid")
    var valid: Boolean?=null,

    @SerializedName("applied")
    var applied: Boolean?=null,

    @SerializedName("startedRefactoringFlow")
    var startedRefactoringFlow: Boolean?=null,

    @SerializedName("undone")
    var undone: Boolean?=null,

    @SerializedName("userRating")
    var rating: String? = null

)

data class EFUserSelectionTelemetryData(
    @SerializedName("lineStart")
    var lineStart: Int,

    @SerializedName("lineEnd")
    var lineEnd: Int,

    @SerializedName("functionSize")
    var functionSize: Int,

    @SerializedName("positionInHostFunction")
    var positionInHostFunction: Int,

    @SerializedName("selectedCandidateIndex")
    var selectedCandidateIndex: Int,

    @SerializedName("candidateType")
    var candidateType: EfCandidateType,

    @SerializedName("elementsType")
    var elementsType: List<EFPsiElementsTypesTelemetryData>,
)

data class CandidateElapsedTimeTelemetryData(
    @SerializedName("candidateIndex")
    var candidateIndex: Int,

    @SerializedName("elapsedTime")
    var elapsedTime: Long,
)

data class EFPsiElementsTypesTelemetryData(
    @SerializedName("elementType")
    var type: String,

    @SerializedName("quantity")
    var quantity: Int,
)


enum class TelemetryDataAction() {
    START,
    STOP
}

data class EFTelemetryDataElapsedTimeNotificationPayload(
    @SerializedName("action")
    var action: TelemetryDataAction,

    @SerializedName("currentSelectionIndex")
    var selectionIndex: Int
)

data class EFTelemetryDataProcessingTime(
    @SerializedName("llmResponseTime")
    var llmResponseTime: Long,

    @SerializedName("pluginProcessingTime")
    var pluginProcessingTime: Long,

    @SerializedName("totalTime")
    var totalTime: Long
)

data class MoveMethodIterationData(
    @SerializedName("iteration_num")
    var iterationNum: Int,

    @SerializedName("suggested_move_methods")
    var suggestedMoveMethods: List<ApplyMoveMethodInteractiveIntention.MoveMethodSuggestion>,

    @SerializedName("llm_response_time")
    var llmResponseTime: Long
)

data class LlmMovePriority(
    @SerializedName("priority_method_names")
    var methodNames: List<String>?,

    @SerializedName("explanation")
    var explanation: String?,

    @SerializedName("llm_response_time")
    var llmResponseTime: Long
)
data class TargetClass4Method(
    @SerializedName("target_classes")
    var targetClasses: List<TargetClass>,

    @SerializedName("target_classes_sorted_by_llm")
    var targetClassesSorted: List<String>?,

    @SerializedName("llm_response_time")
    var llmResponseTime: Long?,

    @SerializedName("similarity_computation_time")
    var similarityComputationTime: Long,

    @SerializedName("similarity_metric")
    var similarityMetric: String,

    @SerializedName("target_class_priority_explanation")
    var explanation: String?

)

data class TargetClass(
    @SerializedName("class_name")
    val className: String,

    @SerializedName("similarity_score")
    val similarityScore: Double
)

data class AgenticTelemetry(
    @SerializedName("agentData")
    val agentIterationData: List<AgentIterationTelemetry>
){
    companion object{
        fun createFromSessionIds(sessionIds: List<String>,
                                 telemetryDataManager: EFTelemetryDataManager): AgenticTelemetry{
            return AgenticTelemetry(
                sessionIds
                    .map { telemetryDataManager.getData(it) }
                    .filterNotNull()
                    .mapIndexed { index: Int, refTelemetryData: RefTelemetryData
                        ->  AgentIterationTelemetry(iteration = index, refTelemetryData = refTelemetryData)}
            )
        }
    }
}

data class AgentIterationTelemetry(
    @SerializedName("iteration")
    val iteration: Int,

    @SerializedName("iterationData")
    val refTelemetryData: RefTelemetryData
)

class EFTelemetryDataManager {
    private var currentSessionId: String = ""
    private val data: MutableMap<String, RefTelemetryData> = mutableMapOf()
    private lateinit var currentTelemetryData: RefTelemetryData
    private var anonimizeTelemetry = RefAgentSettingsManager.getInstance().getAnonymizeTelemetry()
    private val anonMethodMap = mutableMapOf<String, String>()
    private val anonClassMap = mutableMapOf<String, String>()

    fun newSession(): String {
        anonimizeTelemetry = RefAgentSettingsManager.getInstance().getAnonymizeTelemetry()
        anonClassMap.clear()
        anonMethodMap.clear()
        currentSessionId = UUID.randomUUID().toString()
        currentTelemetryData = RefTelemetryData(currentSessionId)
        data[currentSessionId] = currentTelemetryData
        return currentSessionId
    }

    fun currentSession(): String {
        if (currentSessionId.isNotEmpty()) return currentSessionId
        return newSession()
    }

    fun addHostFunctionTelemetryData(hostFunctionTelemetryData: HostFunctionTelemetryData): EFTelemetryDataManager {
        currentTelemetryData.hostFunctionTelemetryData = hostFunctionTelemetryData
        return this
    }

    fun addCandidatesTelemetryData(candidatesTelemetryData: RefCandidatesTelemetryData): EFTelemetryDataManager {
        currentTelemetryData.candidatesTelemetryData = candidatesTelemetryData
        return this
    }

    fun addUserSelectionTelemetryData(userSelectionTelemetryData: EFUserSelectionTelemetryData): EFTelemetryDataManager {
        currentTelemetryData.userSelectionTelemetryData = userSelectionTelemetryData
        return this
    }

    fun getData(sessionId: String? = null): RefTelemetryData? {

        val sId = sessionId ?: currentSession()
        val refTelemetryData = data.getOrDefault(sId, null)
        if (refTelemetryData != null) {
            processAbstractRefactorings(refTelemetryData)
        }
        return refTelemetryData
    }

    private fun processAbstractRefactorings(refTelemetryData: RefTelemetryData) {
        val objs = refTelemetryData.refactoringObjects.map { it ->
            RefCandidateTelemetryData(
                it.startLoc, it.endLoc,
                it::class.simpleName.toString(),
                it.getRefactoringPreview(),
                if(anonimizeTelemetry) "Anonymous" else it.description,
                couldCreateRefObject = it !is UncreatableRefactoring,
                valid = it.isValid,
                applied = it.applied,
                undone = it.undone,
                rating = it.userRating,
                startedRefactoringFlow = it.startedRefactoringFlow
            )
        }

        refTelemetryData.candidatesTelemetryData = RefCandidatesTelemetryData(
            refTelemetryData.refactoringObjects?.size ?: 0, objs
        )

    }

    fun setRefactoringObjects(refactoringCandidates: List<AbstractRefactoring>) {
        currentTelemetryData.refactoringObjects = refactoringCandidates
    }
    private fun getAndSetAnonMethodName(methodName: String): String {
        val anonMethodName = anonMethodMap[methodName] ?: "method${anonMethodMap.size}"
        anonMethodMap.put(methodName, anonMethodName)
        return anonMethodName
    }
    private fun getAndSetAnonClassName(className: String): String {
        val anonClassName = anonClassMap[className] ?: "class${anonClassMap.size}"
        anonClassMap.put(className, anonClassName)
        return anonClassName
    }

    fun addMovesSuggestedInIteration(iter: Int, moveSuggestions: List<ApplyMoveMethodInteractiveIntention.MoveMethodSuggestion>, llmResponseTime: Long) {
        val transformedMethodNames = if (anonimizeTelemetry) {
            moveSuggestions.map {
                ApplyMoveMethodInteractiveIntention.MoveMethodSuggestion(
                    getAndSetAnonMethodName(it.methodName), "", getAndSetAnonClassName(it.targetClass), "", it.psiMethod)

            }
        }else{ moveSuggestions}
        currentTelemetryData.iterationData.add(MoveMethodIterationData(iter, transformedMethodNames, llmResponseTime))
    }

    fun addLLMPriorityResponse(moveMethodSuggestions: List<String>, llmResponseTime: Long) {
        val transformedMethodNames = if (anonimizeTelemetry){
            moveMethodSuggestions.map{getAndSetAnonMethodName(it)}
        }else{moveMethodSuggestions}
        currentTelemetryData.llmPriority = LlmMovePriority(transformedMethodNames, null, llmResponseTime)
    }
    fun addLLMPriorityResponse(responseText: String, llmResponseTime: Long) {
        currentTelemetryData.llmPriority = LlmMovePriority(
            null,
            if(anonimizeTelemetry) "LLM Gave some reasoning. Hiding for anonymity." else responseText,
            llmResponseTime
        )
    }

    fun addPotentialTargetClassesOrdered(
        methodToMove: String,
        targetClassesWithSimilarityMetric: List<Pair<String?, Double>>,
        similarityMetric: String,
        similarityComputationTime: Long
    ) {
        val anonIfTargetClassesWithSimilarity = if (anonimizeTelemetry){
            targetClassesWithSimilarityMetric.map{
                if (it.first==null)
                    null
                else{
                    Pair(getAndSetAnonClassName(it.first!!), it.second)
                }
            }.filterNotNull()
        }else{targetClassesWithSimilarityMetric}

        val anonIfMethodToMove = if(anonimizeTelemetry){
            getAndSetAnonMethodName(methodToMove)
        }else {methodToMove}

        val targetClassData = currentTelemetryData.targetClassMap.get(
            anonIfMethodToMove)
        val updatedInfo = TargetClass4Method(
            anonIfTargetClassesWithSimilarity.map { it.first.let { first ->
                    if (first != null) {
                        TargetClass(first, it.second)
                    }else {
                        null
                    }
                } }.filterNotNull(),
                similarityComputationTime = similarityComputationTime,
                similarityMetric = similarityMetric,
                targetClassesSorted = null,
                llmResponseTime = null,
                explanation = null
            )
        if (targetClassData!=null){
            targetClassData.targetClasses = updatedInfo.targetClasses
            targetClassData.similarityMetric = updatedInfo.similarityMetric
            targetClassData.similarityComputationTime = updatedInfo.similarityComputationTime
        }else{
            currentTelemetryData.targetClassMap.put(anonIfMethodToMove, updatedInfo)
        }
    }

    fun setTotalTime(totalPluginTime: Long) {
        currentTelemetryData.processingTime = EFTelemetryDataProcessingTime(llmResponseTime = -1, totalTime = totalPluginTime, pluginProcessingTime = -1)
    }

    fun addLlmTargetClassPriorityResponse(methodToMove: String, targetClassesSorted: List<String>, llmResponseTime: Long) {
        val anonIfTargetClasses = if (anonimizeTelemetry){
            targetClassesSorted.map{getAndSetAnonClassName(it)}
        }else{targetClassesSorted}

        val anonIfMethodToMove = if(anonimizeTelemetry){
            getAndSetAnonMethodName(methodToMove)
        }else {methodToMove}

        val targetClassData = currentTelemetryData.targetClassMap.get(
            anonIfMethodToMove)
        if (targetClassData!=null){
            targetClassData.targetClassesSorted = anonIfTargetClasses
            targetClassData.llmResponseTime = llmResponseTime
        }
    }
    fun addLlmTargetClassPriorityResponse(methodToMove: String, unparseableResponse: String, llmResponseTime: Long) {
        val targetClassData = currentTelemetryData.targetClassMap.get(
            methodToMove)
        if (targetClassData!=null){
            targetClassData.explanation =
                if(anonimizeTelemetry) "LLM gave some reasoning. Hiding for anonymity." else unparseableResponse
            targetClassData.llmResponseTime = llmResponseTime
        }
    }

    fun addMethodCompatibility(methodCompatibilitySuggestions: List<Pair<ApplyMoveMethodInteractiveIntention.MoveMethodSuggestion, Double>>) {
        val anonCompatibility = if(!anonimizeTelemetry){
            methodCompatibilitySuggestions.map { it.first.methodSignature to Pair(it.first, it.second) }
        }else {
            methodCompatibilitySuggestions.map{
                getAndSetAnonMethodName(it.first.methodSignature) to
                        Pair(
                            ApplyMoveMethodInteractiveIntention.MoveMethodSuggestion(
                                "", getAndSetAnonMethodName(it.first.methodSignature), "", "", it.first.psiMethod),
                            it.second) 
            }
        }
        currentTelemetryData.methodCompatibility.putAll(
            anonCompatibility
        )
    }
}

class EFTelemetryDataUtils {
    companion object {
        fun buildHostFunctionTelemetryData(
            codeSnippet: String,
            lineStart: Int,
            bodyLineStart: Int,
            language: String,
            filePath: String,
            hostClassPsi: PsiClass?
        ): HostFunctionTelemetryData {
            val functionSize = codeSnippet.lines().size
            return HostFunctionTelemetryData(
                lineStart = lineStart,
                lineEnd = lineStart + functionSize - 1,
                hostFunctionSize = functionSize,
                bodyLineStart = bodyLineStart,
                language = language,
                sourceCode = if (RefAgentSettingsManager.getInstance().getAnonymizeTelemetry()) "" else codeSnippet,
                filePath = if (RefAgentSettingsManager.getInstance().getAnonymizeTelemetry()) "" else filePath,
                methodCount = PsiUtils.getAllMethodsInClass(hostClassPsi).size
            )
        }

        private fun buildCandidateTelemetryData(
            candidateApplicationPayload: EFCandidateApplicationPayload,
            anonymizeDescription: Boolean = true): RefCandidateTelemetryData {
            val candidate = candidateApplicationPayload.candidate
            return RefCandidateTelemetryData(
                lineStart = candidate.startLoc,
                lineEnd = candidate.endLoc,
//                candidateType = candidate.getRefactoringName(),
//                candidateType = EfCandidateType.AS_IS,
//                applicationResult = candidateApplicationPayload.result,
//                reason = candidateApplicationPayload.reason,
                description = if(anonymizeDescription) "Anonymized." else candidate.description,
                refactoringType = candidate::class.java.toString(),
                refactoringInformation = if (anonymizeDescription) "Anonymized." else candidate.getRefactoringPreview()
//
            )
        }

        fun buildCandidateTelemetryData(candidateApplicationPayloadList: List<EFCandidateApplicationPayload>): List<RefCandidateTelemetryData> {
            val candidateTelemetryDataList: MutableList<RefCandidateTelemetryData> = mutableListOf()
            candidateApplicationPayloadList.forEach {
                candidateTelemetryDataList.add(buildCandidateTelemetryData(it))
            }
            return candidateTelemetryDataList.toList()
        }

        fun buildUserSelectionTelemetryData(
            efCandidate: AbstractRefactoring,
            candidateIndex: Int,
            hostFunctionTelemetryData: HostFunctionTelemetryData?,
            file: PsiFile
        ): EFUserSelectionTelemetryData {
            var positionInHostFunction = -1
            if (hostFunctionTelemetryData != null) {
                positionInHostFunction = efCandidate.startLoc - hostFunctionTelemetryData.bodyLineStart
            }
            return EFUserSelectionTelemetryData(
                lineStart = efCandidate.startLoc,
                lineEnd = efCandidate.endLoc,
                functionSize = efCandidate.endLoc - efCandidate.startLoc + 1,
                positionInHostFunction = positionInHostFunction,
                selectedCandidateIndex = candidateIndex,
                candidateType = EfCandidateType.AS_IS,
                elementsType = buildElementsTypeTelemetryData(efCandidate, file),
            )
        }

        fun buildElementsTypeTelemetryData(
            efCandidate: AbstractRefactoring,
            file: PsiFile
        ): List<EFPsiElementsTypesTelemetryData> {
            val psiElements = file.elementsInRange(TextRange(efCandidate.getStartOffset(), efCandidate.getEndOffset()))
            val namesList = psiElements.filter { it !is PsiWhiteSpace }.map { it.elementType.toString() }
            val namesQuantityMap = namesList.groupingBy { it }.eachCount()
            val result = namesQuantityMap.entries.map {
                EFPsiElementsTypesTelemetryData(
                    type = it.key,
                    quantity = it.value
                )
            }
            return result
        }
    }
}