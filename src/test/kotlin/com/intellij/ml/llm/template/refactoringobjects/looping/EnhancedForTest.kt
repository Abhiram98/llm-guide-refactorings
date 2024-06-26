package com.intellij.ml.llm.template.refactoringobjects.looping

import com.intellij.codeInspection.*
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper
import com.intellij.codeInspection.streamMigration.StreamApiMigrationInspection
import com.intellij.ml.llm.template.utils.CodeTransformer
import com.intellij.ml.llm.template.utils.EFObserver
import com.intellij.ml.llm.template.utils.PsiUtils
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
        val codeTransformer = CodeTransformer()
        val efObserver = EFObserver()
        codeTransformer.addObserver(efObserver)

        configureByFile("/testdata/HelloWorld.java")
//        configureByFile("/testdata/HelloWorld.java")
        editor.moveCaret(305)
        val functionPsi: PsiElement? = PsiUtils.getParentFunctionOrNull(editor, language = file.language)
        val foreachInspection = ForCanBeForeachInspection()
//        val foreachInspection = (f.tool as ForCanBeForeachInspection)
        foreachInspection.shortName
        foreachInspection.displayName
        foreachInspection.groupDisplayName
        foreachInspection.descriptionFileName
//        val foreachInspection = ForLoopReplaceableByWhileInspection()
//        .buildVisitor()
        val problemsHolder = ProblemsHolder(
            InspectionManager.getInstance(project), file, true)
        val visitor = foreachInspection.buildVisitor(problemsHolder, true)



        functionPsi!!.accept(visitor)
        functionPsi!!.children[8].children[2].accept(visitor)
        assert(problemsHolder.hasResults())
        val p0 = problemsHolder.results[0]!!
        WriteCommandAction.runWriteCommandAction(project,
            Runnable {  p0.fixes?.get(0)?.applyFix(project, p0)})
        println(file.text)
        LocalInspectionToolWrapper(
            LocalInspectionEP()
        )

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

}