package org.boulderse.ijserver

import org.boulderse.ijserver.refactoringobjects.extractfunction.EFSuggestion
import org.boulderse.ijserver.utils.CodeTransformer
import org.boulderse.ijserver.utils.EFApplicationResult
import org.boulderse.ijserver.refactoringobjects.extractfunction.EFCandidateFactory
import org.boulderse.ijserver.refactoringobjects.extractfunction.ExtractMethodFactory
import org.boulderse.ijserver.utils.EFObserver
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import junit.framework.TestCase

class CodeTransformerTest : LightPlatformCodeInsightTestCase() {
    private var projectPath = "src/test"
    override fun getTestDataPath(): String {
        return projectPath
    }

    fun `test failed extract function candidates are reported correctly`() {
        val codeTransformer = CodeTransformer()
        val efObserver = EFObserver()
        codeTransformer.addObserver(efObserver)

        configureByFile("/testdata/KafkaAdminClientTest.java")
        val efs = EFSuggestion(
            functionName = "createPartitionMetadata",
            lineStart = 113,
            lineEnd = 119
        )
        val efCandidates = EFCandidateFactory().buildCandidates(efs, editor, file)

        val funcCall = "extract_method(113, 119, \"createPartitionMetadata\")"
        val emObj = ExtractMethodFactory.createObjectsFromFuncCall(
            funcCall, project, editor, file
        )

        for (obj in emObj){
            if (obj.isValid(project, editor, file)) {
                obj.performRefactoring(project, editor, file)
            }
        }

//        efCandidates.forEach { candidate ->
//            configureByFile("/testdata/KafkaAdminClientTest.java")
//            TODO("apply candidate")
//            codeTransformer.applyCandidate(candidate, project, editor, file)
//        }

        val failedNotifications = efObserver.getNotifications(EFApplicationResult.FAIL)
        val successNotifications = efObserver.getNotifications(EFApplicationResult.OK)
        TestCase.assertEquals(1, failedNotifications.size)
        TestCase.assertEquals(1, successNotifications.size)
    }
}