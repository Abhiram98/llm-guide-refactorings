package org.boulderse.ijserver.refactoringobjects.extractclass

import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiJavaFileImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import org.boulderse.ijserver.refactoringobjects.introduce.MyIntroduceFieldHandler
import org.boulderse.ijserver.utils.PsiUtils

class ExtractClassTest : LightPlatformCodeInsightTestCase() {
    private var projectPath = "src/test"

    override fun getTestDataPath(): String = projectPath

    fun `test extract class`() {
        configureByFile("/testdata/HelloWorld.java")
        val lineNumber = 40
        val psiMethods: List<PsiMethod> =
            PsiUtils.getElementsOfTypeOnLine(
                file,
                editor,
                lineNumber,
                PsiMethod::class.java,
            )
        assert(psiMethods.isNotEmpty())

//        val processor = ExtractInterfaceProcessor(
//            project,
//            false,
//            file.containingDirectory,
//            "HelloWorldInterface",
//            (file as PsiJavaFileImpl).classes[0],
//            psiMethods.map { MemberInfo(it) }.toTypedArray(),
//            DocCommentPolicy<PsiComment>(DocCommentPolicy.ASIS)
//        )
//        processor.run()

        val ref =
            ExtractInterfaceRefactoring.createFromMembers(
                (file as PsiJavaFileImpl).classes[0],
                listOf("numMinus10"),
                "HelloWorldInterface",
                "HelloWorld",
            )
        ref.performRefactoring(project, editor, file)

        println(file.text)

        val clazz =
            JavaPsiFacade.getInstance(project).findClass(
                "org.boulderse.ijserver.testdata.HelloWorldInterface",
                GlobalSearchScope.projectScope(project),
            )!!
        println(clazz.interfaces[0].text)
//        println(interfacePsi.text)

//        assert(file.text.contains("""    public static void prettyPrintArray(List<Integer> array){
//        result = "Array: ";
//
//        for(int i = 0; i < array.size(); i++) {
//            result += "Element: "+(i+1);
//            result += array.get(i).toString();
//            result += "\n";
//        }
//        System.out.println(result);
//    }"""))
//        assert(file.text.contains("    private static String result;"))
    }

    fun `test introduce field 2`() {
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
        throw NotImplementedError("call introduce interface handler")
//        MyIntroduceFieldHandler(project, editor).expressionToField(psiLiterals[0])

        println(file.text)

        assert(
            file.text.contains(
                """    public static void prettyPrintArray(List<Integer> array){
        HelloWorld.result = "Array: ";
        String result = HelloWorld.result;

        for(int i = 0; i < array.size(); i++) {
            result += "Element: "+(i+1);
            result += array.get(i).toString();
            result += "\n";
        }
        System.out.println(result);
    }""",
            ),
        )
    }

    fun `test introduce field instance`() {
        configureByFile("/testdata/HelloWorld.java")
        val lineNumber = 28
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
                30,
                PsiLiteralExpression::class.java,
            )
        val psiLiterals2: List<PsiLiteralExpression> =
            PsiUtils.getElementsOfTypeOnLine(
                file,
                editor,
                32,
                PsiLiteralExpression::class.java,
            )
        assert(psiLiterals.isNotEmpty())
        throw NotImplementedError("call introduce interface handler")
//        MyIntroduceFieldHandler(project, editor).expressionToField(psiLiterals[0])
//        MyIntroduceFieldHandler(project, editor).expressionToField(psiLiterals2[0])

        println(file.text)

        assert(
            file.text.contains(
                """    private java.lang.String string;
    private java.lang.String string1;""",
            ),
        )
        assert(
            file.text.contains(
                """public void prettyPrintIntegerIfImpl(Integer num){
        if (num==1){
            string = "ONE!!";
            System.out.println(string);
        } else if (num==2) {
            string1 = "TWO!!";
            System.out.println(string1);
        }""",
            ),
        )
    }
}
