package com.intellij.ml.llm.template.refactoringobjects.inline

import com.intellij.ml.llm.template.refactoringobjects.introduce.MyIntroduceFieldHandler
import com.intellij.ml.llm.template.utils.PsiUtils
import com.intellij.psi.PsiDeclarationStatement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiLocalVariable
import com.intellij.psi.PsiMethod
import com.intellij.refactoring.inline.InlineConstantFieldProcessor
import com.intellij.refactoring.inline.InlineLocalHandler
import com.intellij.refactoring.inline.InlineObjectProcessor
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import com.siyeh.ig.fixes.InlineVariableFix


class InlineVariableTest: LightPlatformCodeInsightTestCase() {
    private var projectPath = "src/test"
    override fun getTestDataPath(): String {
        return projectPath
    }


    fun `test inline variable`(){
        configureByFile("/testdata/HelloWorld.java")
        val lineNumber = 62
        val psiMethods: List<PsiMethod> = PsiUtils.getElementsOfTypeOnLine(
            file, editor, lineNumber, PsiMethod::class.java
        )
        assert(psiMethods.isNotEmpty())
        val hostMethod = psiMethods[0]
        val psiDecl: List<PsiDeclarationStatement> = PsiUtils.getElementsOfTypeOnLine(
            file, editor, 63, PsiDeclarationStatement::class.java
        )
        assert(psiDecl.isNotEmpty())
        val eleToInline = psiDecl[0].declaredElements[0]

        val inlineHandler = InlineLocalHandler()
        val canInline = inlineHandler.canInlineElement(eleToInline)
        assert(canInline)

        inlineHandler.inlineElement(project, editor, eleToInline)

        println(file.text)

        assert(file.text.contains("""    public static void constructString(Integer a, Boolean b){
        System.out.println("String: " + "s" + a + b);
    }"""))



    }

    fun `test inline variable 2`(){
        configureByFile("/testdata/HelloWorld.java")
        val lineNumber = 51
        val psiMethods: List<PsiMethod> = PsiUtils.getElementsOfTypeOnLine(
            file, editor, lineNumber, PsiMethod::class.java
        )
        assert(psiMethods.isNotEmpty())
        val hostMethod = psiMethods[0]
        val psiDecl: List<PsiDeclarationStatement> = PsiUtils.getElementsOfTypeOnLine(
            file, editor, 52, PsiDeclarationStatement::class.java
        )
        assert(psiDecl.isNotEmpty())
        val eleToInline = psiDecl[0].declaredElements[0]

        val inlineHandler = InlineLocalHandler()
        val canInline = inlineHandler.canInlineElement(eleToInline)
        assert(canInline) // this is surprisingly true

        try{ inlineHandler.inlineElement(project, editor, eleToInline) }
        catch (e: Exception){
            print("expected failure")
            print(e.message)
            return
        }
        throw Exception("There should have been an error. Inline not possible here.")



    }

}