package com.intellij.ml.llm.template

import com.intellij.ml.llm.template.refactoringobjects.extractfunction.EFCandidate
import com.intellij.ml.llm.template.telemetry.*
import com.intellij.ml.llm.template.utils.EFCandidatesApplicationTelemetryObserver
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import junit.framework.TestCase

class RefTelemetryDataTest : LightPlatformCodeInsightTestCase() {
    private var projectPath = "src/test"
    override fun getTestDataPath(): String {
        return projectPath
    }

    fun `test telemetry data collection sessions`() {
        val manager = EFTelemetryDataManager()
        val firstSessionId = manager.newSession()
        val secondSessionId = manager.newSession()

        TestCase.assertFalse(secondSessionId == firstSessionId)
        TestCase.assertEquals(secondSessionId, manager.currentSession())
    }

    fun `test host function telemetry data added successfully`() {
        val manager = EFTelemetryDataManager()
        val sessionId = manager.newSession()
        val hostFunctionTelemetryData = HostFunctionTelemetryData(
            hostFunctionSize = 10,
            lineStart = 5,
            lineEnd = 14,
            bodyLineStart = 6,
            language = "java",
            sourceCode = "", // TODO: Add in the source code.
            filePath = "",
            methodCount = 0
        )
        val efAppTelemetryObserver = EFCandidatesApplicationTelemetryObserver()
        TODO("Update line below")
//        efAppTelemetryObserver.update(
//            EFNotification(
//                EFCandidateApplicationPayload(
//                    result = EFApplicationResult.FAIL,
//                    reason = LLMBundle.message("extract.function.entire.function.selection.message"),
//                    candidate = EFCandidate(
//                        functionName = "foo",
//                        offsetStart = 10,
//                        offsetEnd = 20,
//                        lineStart = 100,
//                        lineEnd = 200
//                    ).also {
//                        it.type = EfCandidateType.AS_IS
//                    },
//                )
//            )
//        )
//        efAppTelemetryObserver.update(
//            EFNotification(
//                EFCandidateApplicationPayload(
//                    result = EFApplicationResult.OK,
//                    reason = "",
//                    candidate = EFCandidate(
//                        functionName = "bar",
//                        offsetStart = 15,
//                        offsetEnd = 25,
//                        lineStart = 105,
//                        lineEnd = 205
//                    ).also {
//                        it.type = EfCandidateType.ADJUSTED
//                    }
//                )
//            )
//        )
//
//        val candidateTelemetryDataList =
//            EFTelemetryDataUtils.buildCandidateTelemetryData(efAppTelemetryObserver.getData())
//        val candidatesTelemetryData = EFCandidatesTelemetryData(
//            numberOfSuggestions = 1,
//            candidates = candidateTelemetryDataList
//        )
//
//        val userSelectionTelemetryData = EFUserSelectionTelemetryData(
//            lineStart = 105,
//            lineEnd = 205,
//            functionSize = 100,
//            positionInHostFunction = 10,
//            selectedCandidateIndex = 0,
//            candidateType = EfCandidateType.ADJUSTED,
//            elementsType = emptyList(),
//        )
//
//        manager
//            .addHostFunctionTelemetryData(hostFunctionTelemetryData)
//            .addCandidatesTelemetryData(candidatesTelemetryData)
//            .addUserSelectionTelemetryData(userSelectionTelemetryData)
//
//        val telemetryData = manager.getData(sessionId)
//
//        TestCase.assertEquals(hostFunctionTelemetryData, telemetryData!!.hostFunctionTelemetryData)
//        TestCase.assertEquals(candidatesTelemetryData, telemetryData.candidatesTelemetryData)
//        TestCase.assertEquals(userSelectionTelemetryData, telemetryData.userSelectionTelemetryData)
    }

    fun `test build host function telemetry data from code snippet`() {
        val codeSnippet =
            """
            fun `test telemetry data collection sessions`() {
                val manager = EFTelemetryDataManager()
                val firstSessionId = manager.newSession()
                val secondSessionId = manager.newSession()

                TestCase.assertFalse(secondSessionId == firstSessionId)
                TestCase.assertEquals(secondSessionId, manager.currentSession())
            } 
            """.trimIndent()
        val expectedHostFunctionTelemetryData = HostFunctionTelemetryData(
            hostFunctionSize = 8,
            lineStart = 5,
            lineEnd = 12,
            bodyLineStart = 6,
            language = "java",
            sourceCode = "",
            filePath = "",
            methodCount = 0
        )
        TestCase.assertEquals(
            expectedHostFunctionTelemetryData,
            EFTelemetryDataUtils.buildHostFunctionTelemetryData(
                codeSnippet = codeSnippet,
                lineStart = 5,
                bodyLineStart = 6,
                language = "java",
                filePath = "",
                hostClassPsi = null
            )
        )
    }

    fun `test get psi elements names Kotlin`() {
        configureByFile("/testdata/RodCuttingProblem.kt")
        val efCandidate = EFCandidate(
            functionName = "foo",
            offsetStart = 321,
            offsetEnd = 523,
            lineStart = 10,
            lineEnd = 17
        )



//        val psiElementsTelemetryData = EFTelemetryDataUtils.buildElementsTypeTelemetryData(
//            ExtractMethodFactory.fromEFCandidate(efCandidate), file)

//        TestCase.assertEquals(2, psiElementsTelemetryData.size)
//        TestCase.assertTrue(psiElementsTelemetryData.contains(EFPsiElementsTypesTelemetryData("BINARY_EXPRESSION", 1)))
//        TestCase.assertTrue(psiElementsTelemetryData.contains(EFPsiElementsTypesTelemetryData("FOR", 1)))
    }
}