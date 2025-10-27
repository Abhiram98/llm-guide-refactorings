package org.boulderse.ijserver.refactoringobjects.movemethod

import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.makeStatic.MakeMethodStaticProcessor
import com.intellij.refactoring.makeStatic.Settings
import com.intellij.refactoring.openapi.impl.JavaRefactoringFactoryImpl
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import org.boulderse.ijserver.utils.PsiUtils
import org.jetbrains.kotlin.idea.core.moveCaret

class MoveMethodTest : LightPlatformCodeInsightTestCase() {
    private var projectPath = "src/test"

    private val packageName = "org.boulderse.ijserver.testdata"
    private val packageStatement = "package $packageName;\n"
    private val classASource =
        "public class A {\n" +
            "    $packageName.B objB;\n" +
            "//    int counter = 0;\n" +
            "    public void m1(){\n" +
            "        objB.foo();\n" +
            "        objB.bar();\n" +
            "//        counter+=1;\n" +
            "    }\n" +
            "\n" +
            "    public void m2($packageName.B paramObjB){\n" +
            "        paramObjB.foo();\n" +
            "        paramObjB.bar();\n" +
            "    }\n" +
            "\n" +
            "    public void m3(){\n" +
            "        System.out.println(\"Hello world\");\n" +
            "    }\n" +
            "    public void m4(){\n" +
            "        objB.foo();\n" +
            "        objB.bar();\n" +
            "        counter+=1;\n" +
            "    }" +
            "}"
    private val classBSource =
        "public class B {\n" +
            "    public void foo() {\n" +
            "    }\n" +
            "\n" +
            "    public void bar() {\n" +
            "    }\n" +
            "}\n"
    private val clientSource =
        "public class Client {\n" +
            "\n" +
            "    public void compute(){\n" +
            "        $packageName.A objA = new $packageName.A();\n" +
            "        $packageName.B objB = new $packageName.B();\n" +
            "//        $packageName.C objC = new $packageName.C();\n" +
            "        objA.m1();\n" +
            "        objA.m2(objB);//objB.m2()\n" +
            "        objA.m3();\n" +
            "        objA.m4();\n" +
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
        println(file.text)
    }

    override fun getTestDataPath(): String = projectPath

    fun testMoveMethodM1() {
        createClassesABC()

        editor.moveCaret(editor.document.getLineStartOffset(6))
        val refObjs =
            MoveMethodFactory.createObjectsFromFuncCall(
                "move_method('m1', 'objB')",
                project,
                editor,
                file,
            )
        assert(refObjs.isNotEmpty())
        refObjs[0].performRefactoring(project, editor, file)
        val PsiClassB =
            JavaPsiFacade.getInstance(project).findClass(
                "$packageName.B",
                GlobalSearchScope.projectScope(project),
            )!!
        val clientClass =
            JavaPsiFacade.getInstance(project).findClass(
                "$packageName.Client",
                GlobalSearchScope.projectScope(project),
            )!!
        assert(
            PsiClassB.text.contains(
                "        public void m1(){\n" +
                    "            foo();\n" +
                    "            bar();\n" +
                    "    //        counter+=1;\n" +
                    "        }",
            ),
        )
        assert(
            clientClass.text.contains(
                "    public void compute(){\n" +
                    "        org.boulderse.ijserver.testdata.A objA = new org.boulderse.ijserver.testdata.A();\n" +
                    "        org.boulderse.ijserver.testdata.B objB = new org.boulderse.ijserver.testdata.B();\n" +
                    "//        org.boulderse.ijserver.testdata.C objC = new org.boulderse.ijserver.testdata.C();\n" +
                    "        objA.objB.m1();\n" +
                    "        objA.m2(objB);//objB.m2()\n" +
                    "        objA.m3();\n" +
                    "        objA.m4();\n" +
                    "    }",
            ),
        )
    }

    fun testMoveMethodM1ByClassName() {
        createClassesABC()

        editor.moveCaret(editor.document.getLineStartOffset(6))
        val refObjs =
            MoveMethodFactory.createObjectsFromFuncCall(
                "move_method('m1', 'B')",
                project,
                editor,
                file,
            )
        assert(refObjs.isNotEmpty())
        refObjs[0].performRefactoring(project, editor, file)
        val PsiClassB =
            JavaPsiFacade.getInstance(project).findClass(
                "$packageName.B",
                GlobalSearchScope.projectScope(project),
            )!!
        val clientClass =
            JavaPsiFacade.getInstance(project).findClass(
                "$packageName.Client",
                GlobalSearchScope.projectScope(project),
            )!!
        assert(
            PsiClassB.text.contains(
                "        public void m1(){\n" +
                    "            foo();\n" +
                    "            bar();\n" +
                    "    //        counter+=1;\n" +
                    "        }",
            ),
        )
        assert(
            clientClass.text.contains(
                "    public void compute(){\n" +
                    "        org.boulderse.ijserver.testdata.A objA = new org.boulderse.ijserver.testdata.A();\n" +
                    "        org.boulderse.ijserver.testdata.B objB = new org.boulderse.ijserver.testdata.B();\n" +
                    "//        org.boulderse.ijserver.testdata.C objC = new org.boulderse.ijserver.testdata.C();\n" +
                    "        objA.objB.m1();\n" +
                    "        objA.m2(objB);//objB.m2()\n" +
                    "        objA.m3();\n" +
                    "        objA.m4();\n" +
                    "    }",
            ),
        )
    }

    fun testMoveMethodM2() {
        createClassesABC()

        val m1LineNum = 12
        editor.moveCaret(editor.document.getLineStartOffset(m1LineNum))
        val refObjs =
            MoveMethodFactory.createObjectsFromFuncCall(
                "move_method('m2', 'paramObjB')",
                project,
                editor,
                file,
            )
        assert(refObjs.isNotEmpty())
        refObjs[0].performRefactoring(project, editor, file)

        val PsiClassB =
            JavaPsiFacade.getInstance(project).findClass(
                "$packageName.B",
                GlobalSearchScope.projectScope(project),
            )!!
        val clientClass =
            JavaPsiFacade.getInstance(project).findClass(
                "$packageName.Client",
                GlobalSearchScope.projectScope(project),
            )!!
        assert(
            PsiClassB.text.contains(
                "    public void m2(){\n" +
                    "        foo();\n" +
                    "        bar();\n" +
                    "    }",
            ),
        )
        assert(
            clientClass.text.contains(
                "    public void compute(){\n" +
                    "        org.boulderse.ijserver.testdata.A objA = new org.boulderse.ijserver.testdata.A();\n" +
                    "        org.boulderse.ijserver.testdata.B objB = new org.boulderse.ijserver.testdata.B();\n" +
                    "//        org.boulderse.ijserver.testdata.C objC = new org.boulderse.ijserver.testdata.C();\n" +
                    "        objA.m1();\n" +
                    "        objB.m2();//objB.m2()\n" +
                    "        objA.m3();\n" +
                    "        objA.m4();\n" +
                    "    }",
            ),
        )
    }

    fun testMoveMethodM2ByClassName() {
        createClassesABC()

        val m1LineNum = 12
        editor.moveCaret(editor.document.getLineStartOffset(m1LineNum))
        val refObjs =
            MoveMethodFactory.createObjectsFromFuncCall(
                "move_method('m2', 'paramObjB')",
                project,
                editor,
                file,
            )
        assert(refObjs.isNotEmpty())
        refObjs[0].performRefactoring(project, editor, file)

        val PsiClassB =
            JavaPsiFacade.getInstance(project).findClass(
                "$packageName.B",
                GlobalSearchScope.projectScope(project),
            )!!
        val clientClass =
            JavaPsiFacade.getInstance(project).findClass(
                "$packageName.Client",
                GlobalSearchScope.projectScope(project),
            )!!
        assert(
            PsiClassB.text.contains(
                "    public void m2(){\n" +
                    "        foo();\n" +
                    "        bar();\n" +
                    "    }",
            ),
        )
        assert(
            clientClass.text.contains(
                "    public void compute(){\n" +
                    "        org.boulderse.ijserver.testdata.A objA = new org.boulderse.ijserver.testdata.A();\n" +
                    "        org.boulderse.ijserver.testdata.B objB = new org.boulderse.ijserver.testdata.B();\n" +
                    "//        org.boulderse.ijserver.testdata.C objC = new org.boulderse.ijserver.testdata.C();\n" +
                    "        objA.m1();\n" +
                    "        objB.m2();//objB.m2()\n" +
                    "        objA.m3();\n" +
                    "        objA.m4();\n" +
                    "    }",
            ),
        )
    }

    fun testMoveMethodM4() {
        // TEST failing because unable to resolve types within test.
        createClassesABC()

        val m1LineNum = 20
        editor.moveCaret(editor.document.getLineStartOffset(m1LineNum))
        val refObjs =
            MoveMethodFactory.createObjectsFromFuncCall(
                "move_method('m4', 'objB')",
                project,
                editor,
                file,
            )
        assert(refObjs.isNotEmpty())
        refObjs[0].performRefactoring(project, editor, file)

        val PsiClassB =
            JavaPsiFacade.getInstance(project).findClass(
                "$packageName.B",
                GlobalSearchScope.projectScope(project),
            )!!
        val clientClass =
            JavaPsiFacade.getInstance(project).findClass(
                "$packageName.Client",
                GlobalSearchScope.projectScope(project),
            )!!

        assert(
            PsiClassB.text.contains(
                "    public void m4($packageName.A a){\n" +
                    "        foo();\n" +
                    "        bar();\n" +
                    "        a.counter+=1;\n" +
                    "    }",
            ),
        )
        assert(
            clientClass.text.contains(
                "    public void compute(){\n" +
                    "        org.boulderse.ijserver.testdata.A objA = new org.boulderse.ijserver.testdata.A();\n" +
                    "        org.boulderse.ijserver.testdata.B objB = new org.boulderse.ijserver.testdata.B();\n" +
                    "//        org.boulderse.ijserver.testdata.C objC = new org.boulderse.ijserver.testdata.C();\n" +
                    "        objA.m1();\n" +
                    "        objB.m2();//objB.m2()\n" +
                    "        objA.m3();\n" +
                    "        objA.objB.m4(objA);\n" +
                    "    }",
            ),
        )
    }

    fun testMakeMethodStatic() {
        configureByFile("/testdata/HelloWorld.java")
        val lineNumber = 13
        val psiMethods: List<PsiMethod> =
            PsiUtils.getElementsOfTypeOnLine(
                file,
                editor,
                lineNumber,
                PsiMethod::class.java,
            )
        assert(psiMethods.isNotEmpty())
        MakeMethodStaticProcessor(project, psiMethods[0], Settings(true, null, null)).run()
        println(file.text)
        assert(file.text.contains("public static void linearSearch(List<Integer> array, int value){"))
    }

    fun testMoveStaticMethod() {
        createAndSaveFile(
            projectPath + "/MyWorld.java",
            packageStatement +
                "\n" +
                "public class MyWorld {}",
        )
        configureByFile("/testdata/HelloWorld.java")
        val lineNumber = 62
        val psiMethods: List<PsiMethod> =
            PsiUtils.getElementsOfTypeOnLine(
                file,
                editor,
                lineNumber,
                PsiMethod::class.java,
            )
        assert(psiMethods.isNotEmpty())

        val refFactory = JavaRefactoringFactoryImpl(project)
        val moveRefactoring =
            refFactory.createMoveMembers(
                psiMethods.toTypedArray(),
                "$packageName.MyWorld",
                "public",
            )
        moveRefactoring.run()

        val changedClass =
            JavaPsiFacade.getInstance(project).findClass(
                "$packageName.MyWorld",
                GlobalSearchScope.projectScope(project),
            )
        assert(changedClass != null)
        println(changedClass!!.text)
        assert(
            changedClass.text.contains(
                "public class MyWorld {\n" +
                    "    public static void constructString(Integer a, Boolean b){\n" +
                    "        String s = \"String: \" + \"s\" + a + b;\n" +
                    "        System.out.println(s);\n" +
                    "    }\n" +
                    "}",
            ),
        )
    }

    fun testMoveStaticMethodFromClassName() {
        createAndSaveFile(
            projectPath + "/MyWorld.java",
            packageStatement +
                "\n" +
                "public class MyWorld {}",
        )
        configureByFile("/testdata/HelloWorld.java")
        val lineNumber = 67
        val psiMethods: List<PsiMethod> =
            PsiUtils.getElementsOfTypeOnLine(
                file,
                editor,
                lineNumber,
                PsiMethod::class.java,
            )
        assert(PsiUtils.isMethodStatic(psiMethods[0]))
        assert(psiMethods.isNotEmpty())
        val fullyQualiedName =
            PsiUtils.getQualifiedTypeInFile(file, "MyWorld")
        val t2 =
            PsiUtils.getQualifiedTypeInFile(file, "List")
        assert(fullyQualiedName != null)
        val refFactory = JavaRefactoringFactoryImpl(project)
        val moveRefactoring =
            refFactory.createMoveMembers(
                psiMethods.toTypedArray(),
                "$packageName.MyWorld",
                "public",
            )
        moveRefactoring.run()

        print(psiMethods[0].containingClass?.qualifiedName)
        val changedClass =
            JavaPsiFacade.getInstance(project).findClass(
                "$packageName.MyWorld",
                GlobalSearchScope.projectScope(project),
            )
        assert(changedClass != null)
        println(changedClass!!.text)
        assert(
            changedClass.text.contains(
                "public class MyWorld {\n" +
                    "    public static void constructString(Integer a, Boolean b){\n" +
                    "        String s = \"String: \" + \"s\" + a + b;\n" +
                    "        System.out.println(s);\n" +
                    "    }\n" +
                    "}",
            ),
        )
    }

    fun testMoveInstanceMethod() {
        createAndSaveFile(
            projectPath + "/MyWorld.java",
            packageStatement +
                "\n" +
                "public class MyWorld {}",
        )
        configureByFile("/testdata/HelloWorld.java")
        val lineNumber = 13
        val psiMethods: List<PsiMethod> =
            PsiUtils.getElementsOfTypeOnLine(
                file,
                editor,
                lineNumber,
                PsiMethod::class.java,
            )
        assert(psiMethods.isNotEmpty())

        val refFactory = JavaRefactoringFactoryImpl(project)
        val moveRefactoring =
            refFactory.createMoveMembers(
                psiMethods.toTypedArray(),
                "$packageName.MyWorld",
                "public",
            )
        moveRefactoring.run()

        val changedClass =
            JavaPsiFacade.getInstance(project).findClass(
                "$packageName.MyWorld",
                GlobalSearchScope.projectScope(project),
            )
        assert(changedClass != null)
        println(changedClass!!.text)
        assert(
            changedClass.text.contains(
                "public class MyWorld {\n" +
                    "    public void linearSearch(List<Integer> array, int value){\n" +
                    "        for (int i=0;i<array.size();i++){\n" +
                    "            if (array.get(i) ==value)\n" +
                    "                System.out.println(\"Found value.\");\n" +
                    "        }\n" +
                    "    }\n" +
                    "}",
            ),
        )
    }

    fun testMoveInstanceMethodUsingFieldsFailing() {
        createAndSaveFile(
            projectPath + "/Reader.java",
            packageStatement +
                "\n" +
                "public class Reader {}",
        )
        configureByFile("/testdata/A1_CSC540.java")
        val lineNumber = 86
        val psiMethods: List<PsiMethod> = listOf(PsiUtils.getLeftmostPsiElement(lineNumber - 1, editor, file)?.parent as PsiMethod)
        assert(psiMethods.isNotEmpty())

        val refFactory = JavaRefactoringFactoryImpl(project)
        val moveRefactoring =
            refFactory.createMoveMembers(
                psiMethods.toTypedArray(),
                "$packageName.Reader",
                "public",
                true,
            )
        try {
            moveRefactoring.run()
            throw Exception("Should have failed.")
        } catch (e: BaseRefactoringProcessor.ConflictsInTestsException) {
            println(e.messages)
            throw e
        }
    }

    fun testMoveInstanceMethodUsingFieldsFailing2() {
        createAndSaveFile(
            projectPath + "/Reader.java",
            packageStatement +
                "\n" +
                "public class Reader {}",
        )
        configureByFile("/testdata/A1_CSC540.java")
        val lineNumber = 146
        val psiMethods: List<PsiMethod> = listOf(PsiUtils.getLeftmostPsiElement(lineNumber - 1, editor, file)?.parent as PsiMethod)
        assert(psiMethods.isNotEmpty())

        val refFactory = JavaRefactoringFactoryImpl(project)
        val moveRefactoring =
            refFactory.createMoveMembers(
                psiMethods.toTypedArray(),
                "$packageName.Reader",
                "public",
                true,
            )
//        MoveInstanceMethodProcessor()
//        MoveMethod
        MakeMethodStaticProcessor(
            project,
            psiMethods[0],
            Settings(true, "classParam", null),
        ).run()
//        refFactory.createMakeMethodStatic(
//            psiMethods[0],
//            null,
//            null,
//            null,
//            null
//        )
//        ConvertToInstanceMethodProcessor(project, psiMethods[0], null, null, null)
        try {
            moveRefactoring.run()
            val changedClass =
                JavaPsiFacade.getInstance(project).findClass(
                    "$packageName.Reader",
                    GlobalSearchScope.projectScope(project),
                )
            assert(changedClass != null)
            println(file.text)
            print("------New file------")
            println(changedClass!!.containingFile.text)
            assert(
                changedClass.text.contains(
                    "public class Reader {\n" +
                        "    public void linearSearch(List<Integer> array, int value){\n" +
                        "        for (int i=0;i<array.size();i++){\n" +
                        "            if (array.get(i) ==value)\n" +
                        "                System.out.println(\"Found value.\");\n" +
                        "        }\n" +
                        "    }\n" +
                        "}",
                ),
            )
        } catch (e: BaseRefactoringProcessor.ConflictsInTestsException) {
            println(e.messages)
        }
    }
}
