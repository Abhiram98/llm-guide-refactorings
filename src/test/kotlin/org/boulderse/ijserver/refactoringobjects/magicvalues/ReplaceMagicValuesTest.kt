package org.boulderse.ijserver.refactoringobjects.magicvalues

import com.intellij.codeInspection.CommonProblemDescriptor
import com.intellij.codeInspection.GlobalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.magicConstant.MagicConstantInspection
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiAssignmentExpression
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiPolyadicExpression
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase
import com.intellij.refactoring.RefactoringFactory
import com.intellij.refactoring.actions.IntroduceConstantAction
import com.intellij.refactoring.inline.InlineConstantFieldProcessor
import com.intellij.refactoring.introduceField.IntroduceConstantHandler
import com.intellij.refactoring.openapi.impl.JavaRefactoringFactoryImpl
import com.intellij.refactoring.suggested.startOffset
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import com.siyeh.ig.fixes.IntroduceConstantFix
import com.siyeh.ig.internationalization.MagicCharacterInspection
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.evaluation.toConstant
import org.junit.jupiter.api.Assertions.*

class ReplaceMagicValuesTest: LightPlatformCodeInsightTestCase(){

    private var projectPath = "src/test"
    override fun getTestDataPath(): String {
        return projectPath
    }


    fun testReplaceMagicValue(){
        configureByFile("/testdata/HelloWorld.java")
        val startLoc = 55
        val element =
            PsiUtilBase.getElementAtOffset(
                file, editor.document.getLineStartOffset(startLoc-1))

        val startOffset = editor.document.getLineStartOffset(startLoc-1)
        val endOffset = editor.document.getLineStartOffset(startLoc)
        val stringLiteral = PsiTreeUtil
            .findChildrenOfType(element.parent, PsiAssignmentExpression::class.java)
            .filter { it.startOffset in startOffset..endOffset }[0]

//        val constantHandler = IntroduceConstantHandler()
//        constantHandler.invoke(project, arrayOf(stringLiteral))
//        val magicCharacterInspection = MagicCharacterInspection()
        val magicCharacterInspection = MagicConstantInspection()


//        val refactoringFactory = RefactoringFactory.getInstance(project)
//        refactoringFactory.toConstant()
//        IntroduceConstantAction()
//        IntroduceConstantFix()
//LocalInspectionToolSession(file, TextRange(startOffset, endOffset))
        val problemsHolder = ProblemsHolder(InspectionManager.getInstance(project), file, false)
        val visitor = magicCharacterInspection.buildVisitor(problemsHolder, false)
        stringLiteral?.accept(visitor)
        assert(problemsHolder.hasResults())
//        val constantFix = IntroduceConstantFix()
//        problemsHolder.registerProblem(stringLiteral, "Replace magic value with constant", constantFix)
        val problem = problemsHolder.results[0]!!
        val fix = problem.fixes!![0]

        WriteCommandAction.runWriteCommandAction(project,
            Runnable { fix.applyFix(project, problem) })
        println(file.text)


    }
    fun testIntroduceConstant() {
        configureByFile("/testdata/HelloWorld.java")
        val startLoc = 52
        val element =
            PsiUtilBase.getElementAtOffset(
                file, editor.document.getLineStartOffset(startLoc - 1)
            )

        val startOffset = editor.document.getLineStartOffset(startLoc - 1)
        val endOffset = editor.document.getLineStartOffset(startLoc)
        val stringLiteral = PsiTreeUtil
            .findChildrenOfType(element.parent, PsiLiteralExpression::class.java)
            .filter { it.startOffset in startOffset..endOffset }[0]

        val introConstant = IntroduceConstantHandler()
        IntroduceConstantAction()
        WriteCommandAction.runWriteCommandAction(project,
            Runnable { introConstant.invoke(project, arrayOf(stringLiteral))})

    }
}