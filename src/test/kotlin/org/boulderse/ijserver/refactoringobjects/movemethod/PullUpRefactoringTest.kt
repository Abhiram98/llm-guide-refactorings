package org.boulderse.ijserver.refactoringobjects.movemethod

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiComment
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.refactoring.memberPullUp.PullUpProcessor
import com.intellij.refactoring.util.DocCommentPolicy
import com.intellij.refactoring.util.classMembers.MemberInfo
import com.intellij.testFramework.LightPlatformCodeInsightTestCase

class PullUpRefactoringTest : LightPlatformCodeInsightTestCase() {
    private var projectPath = "src/test"

    private val packageName = "org.boulderse.ijserver.testdata"
    private val packageStatement = "package $packageName;\n"
    private val classASource =
        "public class A {\n" +
            "    int counter = 0;\n" +
            "    public void m3(){\n" +
            "        System.out.println(\"Hello world\");\n" +
            "    }\n" +
            "}"
    private val classBSource =
        "public class B extends A {\n" +
            "    public void foo() {\n" +
            "    counter+=1;\n" +
            "    }\n" +
            "\n" +
            "    public void bar() {\n" +
            "    }\n" +
            "}\n"
    private val clientSource =
        "public class Client extends A {\n" +
            "\n" +
            "    public void foo(){\n" +
            "    counter += 1;\n" +
            "    }\n" +
            "\n" +
            "}\n"

    private fun createClassesABC() {
        createAndSaveFile(
            projectPath + "/B.java",
            packageStatement +
                "\n" +
                classBSource,
        )
        createAndSaveFile(
            projectPath + "/Client.java",
            packageStatement +
                "\n" +
                clientSource,
        )
        configureFromFileText(
            projectPath + "/A.java",
            packageStatement +
                "\n" +
                classASource,
        )
//        println(file.text)
    }

    override fun getTestDataPath(): String = projectPath

    fun testPushDown() {
        createClassesABC()
        val psiClassB =
            JavaPsiFacade.getInstance(project).findClass(
                "$packageName.B",
                GlobalSearchScope.projectScope(project),
            )!!

        val psiClassA =
            JavaPsiFacade.getInstance(project).findClass(
                "$packageName.A",
                GlobalSearchScope.projectScope(project),
            )!!

        val methodFoo = psiClassB.methods[0]

        PullUpProcessor(
            psiClassB,
            psiClassA,
            listOf(methodFoo).map { MemberInfo(it) }.toTypedArray(),
            DocCommentPolicy(1),
        ).run()

        val psiClassAcopy =
            JavaPsiFacade.getInstance(project).findClass(
                "$packageName.A",
                GlobalSearchScope.projectScope(project),
            )!!
        println("Final result")
        println(psiClassAcopy.text)

//        assert(!psiClassAcopy.text.contains("public void m1(){"))

        val psiClassBCopy =
            JavaPsiFacade.getInstance(project).findClass(
                "$packageName.B",
                GlobalSearchScope.projectScope(project),
            )!!
        println(psiClassBCopy.text)

//        assert(psiClassB.text.contains("public void m1(){"))

        val psiClassClient =
            JavaPsiFacade.getInstance(project).findClass(
                "$packageName.Client",
                GlobalSearchScope.projectScope(project),
            )!!
        println(psiClassClient.text)
//        assert(psiClassClient.text.contains("public void m1(){"))
    }
}
