package org.boulderse.ijserver.refactoringobjects.exceptionhandling

import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import com.siyeh.ipp.exceptions.DetailExceptionsIntention
import org.junit.jupiter.api.Assertions.*

class ExceptionHandalingRefactoringTest : LightPlatformCodeInsightTestCase() {
    private var projectPath = "src/test"

    override fun getTestDataPath(): String = projectPath

    fun addExceptionHandling() {
        val d = DetailExceptionsIntention()
//        d.processIntention(tryPsiElement)
    }
}
