package org.boulderse.ijserver.refactoringobjects.introduce

import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiLocalVariable
import com.intellij.psi.PsiMethod
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import org.boulderse.ijserver.utils.PsiUtils

class IntroduceParamTest : LightPlatformCodeInsightTestCase() {
    private var projectPath = "src/test"

    override fun getTestDataPath(): String = projectPath

    fun testIntroduceParam() {
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
        val methodToReplace = psiMethods[0]
        val psiLiterals: List<PsiLiteralExpression> =
            PsiUtils.getElementsOfTypeOnLine(
                file,
                editor,
                16,
                PsiLiteralExpression::class.java,
            )
        assert(psiLiterals.isNotEmpty())

        val refObj = IntroduceParameter(13, 13, "printString", methodToReplace, psiLiterals[0], null)
        refObj.performRefactoring(project, editor, file)

        println(file.text)

        assert(
            file.text.contains(
                """    public void linearSearch(List<Integer> array, int value, java.lang.String printString){
        for (int i=0;i<array.size();i++){
            if (array.get(i) ==value)
                System.out.println(printString);
        }
    }""",
            ),
        )
    }

    fun `test introduce param 2`() {
        configureByFile("/testdata/HelloWorld.java")
        val lineNumber = 51
        val psiMethods: List<PsiMethod> =
            PsiUtils.getElementsOfTypeOnLine(
                file,
                editor,
                lineNumber,
                PsiMethod::class.java,
            )
        assert(psiMethods.isNotEmpty())
        val methodToReplace = psiMethods[0]
        val psiLiterals: List<PsiLiteralExpression> =
            PsiUtils.getElementsOfTypeOnLine(
                file,
                editor,
                52,
                PsiLiteralExpression::class.java,
            )
        val psiLocalVariables: List<PsiLocalVariable> =
            PsiUtils.getElementsOfTypeOnLine(
                file,
                editor,
                52,
                PsiLocalVariable::class.java,
            )
        assert(psiLiterals.isNotEmpty())

        val refObj = IntroduceParameter(13, 13, "printString", methodToReplace, psiLiterals[0], psiLocalVariables[0])
        refObj.performRefactoring(project, editor, file)

        println(file.text)

        assert(
            file.text.contains(
                """    public static void prettyPrintArray(List<Integer> array, java.lang.String printString){

        for(int i = 0; i < array.size(); i++) {
            printString += "Element: "+(i+1);
            printString += array.get(i).toString();
            printString += "\n";
        }
        System.out.println(printString);
    }""",
            ),
        )
    }
}
