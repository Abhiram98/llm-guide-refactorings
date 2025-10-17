package org.boulderse.ijserver.refactoringobjects.looping

import com.intellij.codeInspection.*
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper
import com.intellij.codeInspection.streamMigration.StreamApiMigrationInspection
import org.boulderse.ijserver.utils.CodeTransformer
import org.boulderse.ijserver.utils.EFObserver
import org.boulderse.ijserver.utils.PsiUtils
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase
import com.intellij.refactoring.suggested.startOffset
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase.JAVA_20
import com.siyeh.ig.controlflow.ForLoopReplaceableByWhileInspection
import com.siyeh.ig.migration.ForCanBeForeachInspection
import org.jetbrains.kotlin.idea.core.moveCaret

class EnhancedForTest: LightPlatformCodeInsightTestCase() {
    private var projectPath = "src/test"
    override fun getTestDataPath(): String {
        return projectPath
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return JAVA_20
    }

    fun testEnhancedForRefactoring() {
        configureByFile("/testdata/HelloWorld.java")
        val refObjs = EnhancedForFactory.factory.createObjectsFromFuncCall(
            "use_enhanced_forloop(14)", project, editor, file
        )

        assert(refObjs.isNotEmpty()) // Test failing because unable to resolve
        refObjs[0].performRefactoring(project, editor, file)
        println(file.text)

    }

    fun testTraditionalForLoop(){
        val packageName = "org.boulderse.ijserver.testdata"
        val packageStatement = "package $packageName;\n"

        val classASource = "import java.util.List;\n" +
                "\n" +
                "class A {\n" +
                "    public static void main(String[] args) {\n" +
                "    }\n" +
                "\n" +
                "    public void linearSearch(List<Integer> array, int value){\n" +
                "        for (Integer integer : array) {\n" +
                "            if (integer == value)\n" +
                "                System.out.println(\"Found value.\");\n" +
                "        }\n" +
                "    }\n" +
                "}"
        configureFromFileText(
            projectPath + "/A.java",
            packageStatement +
                    "\n" +
                    classASource
        )

        val refObjs = TraditionalForFactory.createObjectsFromFuncCall(
            "use_traditional_forloop(10)", project, editor, file)
        assert(refObjs.isNotEmpty())
        refObjs[0].performRefactoring(project, editor, file)
        println(file.text)
        assert(file.text.contains("    public void linearSearch(List<Integer> array, int value){\n" +
                "        for (int i = 0; i < array.size(); i++) {\n" +
                "            Integer integer = array.get(i);\n" +
                "            if (integer == value)\n" +
                "                System.out.println(\"Found value.\");\n" +
                "        }\n" +
                "    }"))
    }

    fun testFor2While(){
        configureByFile("/testdata/HelloWorld.java")
        editor.moveCaret(305)
        val refObjs = For2While.factory.createObjectsFromFuncCall(
            "convert_for2while(14)", project, editor, file
        )
        assert(refObjs.isNotEmpty())
        refObjs[0].performRefactoring(project, editor, file)
        println(file.text)
        assert(file.text.contains(
            "    public void linearSearch(List<Integer> array, int value){\n" +
                    "        int i=0;\n" +
                    "        while (i<array.size()) {\n" +
                    "            if (array.get(i) ==value)\n" +
                    "                System.out.println(\"Found value.\");\n" +
                    "            i++;\n" +
                    "        }\n" +
                    "    }"))

    }


    fun testFor2Stream(){
        configureByFile("/testdata/HelloWorld.java")

        val refObjs = For2Stream.factory.createObjectsFromFuncCall(
            "convert_for2stream(14)", project, editor, file
        )
        assert(refObjs.isNotEmpty())
        refObjs[0].performRefactoring(project, editor, file)
        println(file.text)
        assert(file.text.contains(
            "    public void linearSearch(List<Integer> array, int value){\n" +
                    "        java.util.stream.IntStream.iterate(0, i -> i < array.size(), i -> i + 1).filter(i -> array.get(i) == value).mapToObj(i -> \"Found value.\").forEach(item -> System.out.println(item));\n" +
                    "    }"))
    }

    fun testFor2StreamReverse(){
        configureByFile("/testdata/HelloWorld.java")

        val refObjs = For2Stream.factory.createObjectsFromFuncCall(
            "convert_for2stream(14)", project, editor, file
        )
        assert(refObjs.isNotEmpty())
        refObjs[0].performRefactoring(project, editor, file)
        println(file.text)
        assert(file.text.contains(
            "    public void linearSearch(List<Integer> array, int value){\n" +
                    "        java.util.stream.IntStream.iterate(0, i -> i < array.size(), i -> i + 1).filter(i -> array.get(i) == value).mapToObj(i -> \"Found value.\").forEach(item -> System.out.println(item));\n" +
                    "    }"))

        refObjs[0]
            .getReverseRefactoringObject(project, editor, file)
            ?.performRefactoring(project, editor, file)
        println(file.text) // Test fails because failing to resolve
        assert(file.text.contains(
            "    public void linearSearch(List<Integer> array, int value){\n" +
                    "        for (int i=0;i<array.size();i++){\n" +
                    "            if (array.get(i) ==value)\n" +
                    "                System.out.println(\"Found value.\");\n" +
                    "        }\n" +
                    "    }"
        ))
    }

}