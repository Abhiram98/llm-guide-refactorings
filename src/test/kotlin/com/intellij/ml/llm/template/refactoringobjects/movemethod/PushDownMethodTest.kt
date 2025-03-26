package com.intellij.ml.llm.template.refactoringobjects.movemethod

import com.intellij.ml.llm.template.refactoringobjects.movemethod.pushdown.MyPushDownProcessor
import com.intellij.openapi.application.runWriteAction
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiComment
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.refactoring.memberPushDown.PushDownProcessor
import com.intellij.refactoring.util.DocCommentPolicy
import com.intellij.refactoring.util.classMembers.MemberInfo
import com.intellij.testFramework.LightPlatformCodeInsightTestCase

class PushDownMethodTest: LightPlatformCodeInsightTestCase() {
    private var projectPath = "src/test"

    private val packageName = "com.intellij.ml.llm.template.testdata"
    private val packageStatement = "package $packageName;\n"
    private val classASource = "public class A {\n" +
            "//    int counter = 0;\n" +
            "    public void m1(){\n" +
            "//        counter+=1;\n" +
            "    }\n" +
            "\n" +
            "    public void m3(){\n" +
            "        System.out.println(\"Hello world\");\n" +
            "    }\n" +
            "}"
    private val classBSource = "public class B extends A {\n" +
            "    public void foo() {\n" +
            "    }\n" +
            "\n" +
            "    public void bar() {\n" +
            "    }\n" +
            "}\n"
    private val clientSource = "public class Client extends A {\n" +
            "\n" +
            "    public void compute(){\n" +

            "    }\n" +
            "\n" +
            "}\n"

    private fun createClassesABC() {
        createAndSaveFile(
            projectPath + "/B.java",
            packageStatement +
                    "\n" +
                    classBSource
        )
        createAndSaveFile(
            projectPath + "/Client.java",
            packageStatement +
                    "\n" +
                    clientSource
        )
        configureFromFileText(
            projectPath + "/A.java",
            packageStatement +
                    "\n" +
                    classASource
        )
//        println(file.text)
    }

    override fun getTestDataPath(): String {
        return projectPath
    }

    fun testPushDown(){
        createClassesABC()
        val psiClassA = JavaPsiFacade.getInstance(project).findClass(
            "$packageName.A",
            GlobalSearchScope.projectScope(project))!!

//        val pushDownProcessor =
//            PushDownProcessor(
//                psiClassA,
//                listOf(psiClassA.methods[0]).map { MemberInfo(it) },
//                DocCommentPolicy<PsiComment>(1)
//            )
//        pushDownProcessor.run()

        val proc2 = MyPushDownProcessor(psiClassA, listOf(psiClassA.methods[0]).map { MemberInfo(it) }, DocCommentPolicy<PsiComment>(1))
       runWriteAction { proc2.delegatePerformRefactoring() }

        val psiClassAcopy = JavaPsiFacade.getInstance(project).findClass(
            "$packageName.A",
            GlobalSearchScope.projectScope(project))!!
        println("Final result")
        println(psiClassAcopy.text)

        assert(!psiClassAcopy.text.contains("public void m1(){"))


        val psiClassB = JavaPsiFacade.getInstance(project).findClass(
            "$packageName.B",
            GlobalSearchScope.projectScope(project))!!
        println(psiClassB.text)

        assert(psiClassB.text.contains("public void m1(){"))

        val psiClassClient = JavaPsiFacade.getInstance(project).findClass(
            "$packageName.Client",
            GlobalSearchScope.projectScope(project))!!
        println(psiClassClient.text)
        assert(psiClassClient.text.contains("public void m1(){"))

    }


}