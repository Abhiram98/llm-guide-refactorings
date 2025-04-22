package com.intellij.ml.llm.template.refactoringobjects.introduce

import com.intellij.codeInsight.intention.impl.IntroduceVariableIntentionAction
import com.intellij.ml.llm.template.utils.PsiUtils
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiLocalVariable
import com.intellij.psi.PsiMethod
import com.intellij.refactoring.actions.IntroduceFieldAction
import com.intellij.refactoring.introduceField.IntroduceFieldHandler
import com.intellij.refactoring.move.moveClassesOrPackages.MoveClassesOrPackagesProcessor
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import com.siyeh.ig.fixes.IntroduceVariableFix
import org.jetbrains.kotlin.idea.intentions.IntroduceVariableIntention

class IntroduceFieldTest: LightPlatformCodeInsightTestCase() {
    private var projectPath = "src/test"
    override fun getTestDataPath(): String {
        return projectPath
    }


    fun `test introduce field`(){
        configureByFile("/testdata/HelloWorld.java")
        val lineNumber = 51
        val psiMethods: List<PsiMethod> = PsiUtils.getElementsOfTypeOnLine(
            file, editor, lineNumber, PsiMethod::class.java
        )
        assert(psiMethods.isNotEmpty())
        val methodToReplace = psiMethods[0]
        val psiLiterals: List<PsiLiteralExpression> = PsiUtils.getElementsOfTypeOnLine(
            file, editor, 52, PsiLiteralExpression::class.java
        )
        val psiLocalVariables: List<PsiLocalVariable> = PsiUtils.getElementsOfTypeOnLine(
            file, editor, 52, PsiLocalVariable::class.java
        )
        assert(psiLiterals.isNotEmpty())

        MyIntroduceFieldHandler(project, editor, psiLocalVariables[0],
            containingClass = (file as PsiJavaFile).classes[0]).variableToField()

        println(file.text)

        assert(file.text.contains("""    public static void prettyPrintArray(List<Integer> array){
        result = "Array: ";

        for(int i = 0; i < array.size(); i++) {
            result += "Element: "+(i+1);
            result += array.get(i).toString();
            result += "\n";
        }
        System.out.println(result);
    }"""))
        assert(file.text.contains("    private static String result;"))


    }

    fun `test introduce field 2`(){
        configureByFile("/testdata/HelloWorld.java")
        val lineNumber = 51
        val psiMethods: List<PsiMethod> = PsiUtils.getElementsOfTypeOnLine(
            file, editor, lineNumber, PsiMethod::class.java
        )
        assert(psiMethods.isNotEmpty())
        val methodToReplace = psiMethods[0]
        val psiLiterals: List<PsiLiteralExpression> = PsiUtils.getElementsOfTypeOnLine(
            file, editor, 52, PsiLiteralExpression::class.java
        )
        val psiLocalVariables: List<PsiLocalVariable> = PsiUtils.getElementsOfTypeOnLine(
            file, editor, 52, PsiLocalVariable::class.java
        )
        assert(psiLiterals.isNotEmpty())

        MyIntroduceFieldHandler(project, editor).expressionToField(psiLiterals[0])

        println(file.text)

        assert(file.text.contains("""    public static void prettyPrintArray(List<Integer> array){
        HelloWorld.result = "Array: ";
        String result = HelloWorld.result;

        for(int i = 0; i < array.size(); i++) {
            result += "Element: "+(i+1);
            result += array.get(i).toString();
            result += "\n";
        }
        System.out.println(result);
    }"""))


    }

    fun `test introduce field instance`(){
        configureByFile("/testdata/HelloWorld.java")
        val lineNumber = 28
        val psiMethods: List<PsiMethod> = PsiUtils.getElementsOfTypeOnLine(
            file, editor, lineNumber, PsiMethod::class.java
        )
        assert(psiMethods.isNotEmpty())
        val methodToReplace = psiMethods[0]
        val psiLiterals: List<PsiLiteralExpression> = PsiUtils.getElementsOfTypeOnLine(
            file, editor, 30, PsiLiteralExpression::class.java
        )
        val psiLiterals2: List<PsiLiteralExpression> = PsiUtils.getElementsOfTypeOnLine(
            file, editor, 32, PsiLiteralExpression::class.java
        )
        assert(psiLiterals.isNotEmpty())

        MyIntroduceFieldHandler(project, editor).expressionToField(psiLiterals[0])
        MyIntroduceFieldHandler(project, editor).expressionToField(psiLiterals2[0])

        println(file.text)

        assert(file.text.contains("""    private java.lang.String string;
    private java.lang.String string1;"""))
        assert(file.text.contains("""public void prettyPrintIntegerIfImpl(Integer num){
        if (num==1){
            string = "ONE!!";
            System.out.println(string);
        } else if (num==2) {
            string1 = "TWO!!";
            System.out.println(string1);
        }"""))


    }
}