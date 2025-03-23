package com.intellij.ml.llm.template.refactoringobjects.reformat

import com.intellij.ml.llm.template.cli.main
import com.intellij.ml.llm.template.utils.PsiUtils
import com.intellij.psi.PsiMethod
import com.intellij.refactoring.suggested.startOffset
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.junit.jupiter.api.Assertions.*

class ReformatFileTest: LightPlatformCodeInsightTestCase(){
    private var projectPath = "src/test"
    override fun getTestDataPath(): String {
        return projectPath
    }

    fun testReformat(){
        configureByFile("/testdata/ReformatExample.java")
        val psiMethods: List<PsiMethod> = PsiUtils.getElementsOfTypeOnLine(
            file, editor, 6, PsiMethod::class.java
        )
        assert(psiMethods.isNotEmpty())
        val mainMethod = psiMethods[0]
        ReformatFile.doReformat(file, mainMethod.startOffset, mainMethod.endOffset)
        print(file.text)

        assert(file.text == """package com.intellij.ml.llm.template.testdata;

import java.util.List;

class ReformatExample {
    public static void main(String[] args) {
        int x = 1;
        System.out.println(x);
        System.out.println("Hello world");
    }

}""")

    }

}