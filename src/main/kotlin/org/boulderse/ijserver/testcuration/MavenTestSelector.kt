package org.boulderse.ijserver.testcuration

import java.io.File
import java.util.concurrent.TimeUnit

class MavenTestSelector(
    limitTestCount: Int,
) : TestSelector(limitTestCount) {
    override fun runTest(testMethod: TestMethod): ProcessStatus {
        val projectBasePath = testMethod.testClass.project.basePath!!
        return executeProcess(
            listOf(
                "mvn",
                "test",
                "-Dtest=${testMethod.testClass.qualifiedName}#${testMethod.testMethod.name}",
            ),
            File(projectBasePath),
        )
    }
}
