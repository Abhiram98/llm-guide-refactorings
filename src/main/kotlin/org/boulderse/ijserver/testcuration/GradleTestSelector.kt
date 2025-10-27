package org.boulderse.ijserver.testcuration

import java.io.File

class GradleTestSelector(
    limitTestCount: Int,
) : TestSelector(limitTestCount) {
    override fun runTest(testMethod: TestMethod): ProcessStatus {
        val projectBasePath = testMethod.testClass.project.basePath!!
        val subProject = extractSubProject(testMethod, projectBasePath)
        return executeProcess(
            listOf("./gradlew", ":$subProject:test", "--tests", "${testMethod.testClass.qualifiedName}.${testMethod.testMethod.name}"),
            File(projectBasePath),
        )
    }

    private fun extractSubProject(
        testMethod: TestMethod,
        projectBasePath: String,
    ): String =
        testMethod.testClass.containingFile.virtualFile.path
            .removePrefix("$projectBasePath/")
            .split('/')[0]
}
