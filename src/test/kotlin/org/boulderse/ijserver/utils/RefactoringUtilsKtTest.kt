package org.boulderse.ijserver.utils

import com.intellij.psi.PsiIfStatement
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import org.boulderse.ijserver.refactoringobjects.AbstractRefactoring
import org.boulderse.ijserver.refactoringobjects.conditionals.If2Switch
import org.boulderse.ijserver.refactoringobjects.extractfunction.ExtractMethod
import org.boulderse.ijserver.refactoringobjects.extractfunction.ExtractMethodFactory
import org.boulderse.ijserver.refactoringobjects.looping.For2While

class RefactoringUtilsKtTest : LightPlatformCodeInsightTestCase() {
    private var projectPath = "src/test"
    private val packageName = "org.boulderse.ijserver.testdata"
    private val packageStatement = "package $packageName;\n"

    override fun getTestDataPath(): String = projectPath

    fun testOrdering() {
        configureByFile("/testdata/TransactionCalculator.java")

        val refactoringList = mutableListOf<AbstractRefactoring>()
        val for2while = For2While.factory.fromStartLoc(15, project, editor, file)
        if (for2while != null) {
            refactoringList.add(for2while)
        }

        val em =
            ExtractMethodFactory.createObjectsFromFuncCall(
                "extract_method(13,30, \'performComputation\')",
                project,
                editor,
                file,
            )
        refactoringList.addAll(em)

        val if2switch = If2Switch.factory.fromStartLoc(19, project, editor, file)
        if2switch?.let { refactoringList.add(it) }

        assert(refactoringList.size == 3)
        val orderedRefactorings = getExecutionOrder(refactoringList)
        print(orderedRefactorings)

        assert(orderedRefactorings[0].getRefactoringPreview() == "Convert If to Switch")
        assert(orderedRefactorings[1].getRefactoringPreview() == "Convert For loop to While loop")
        assert(orderedRefactorings[2] is ExtractMethod)
    }

    fun testPsiLocating() {
        val classASource =
            "class A{" +
                "    public void linearSearch(List<Integer> array, int value){\n" +
                "        for (int i=0;i<array.size();i++){\n" +
                "            if (array.get(i) ==value)\n" +
                "                System.out.println(\"Found value.\");\n" +
                "        }\n" +
                "    }" +
                "    public void linearSearchDuplicate(List<Integer> array, int value){\n" +
                "        for (int i=0;i<array.size();i++){\n" +
                "            if (array.get(i) ==value)\n" +
                "                System.out.println(\"Found value.\");\n" +
                "        }\n" +
                "    }" +
                "}"
        configureFromFileText(
            projectPath + "/A.java",
            packageStatement +
                "\n" +
                classASource,
        )

        print(file.text)
    }

    fun testPsiLocating2() {
        configureByFile("/testdata/TransactionCalculator.java")
        print(file.text)
        val ifStatement =
            file.children[3]
                .children[11]
                .children[9]
                .children[2]
                .children[2]
                .children[18]
                .children[11]
                .children[0]
                .children[8] as PsiIfStatement
        print(ifStatement.isValid)
        print(ifStatement.isPhysical)

        val if2switch = If2Switch.factory.fromStartLoc(19, project, editor, file)!!

        val ref = For2While.factory.fromStartLoc(15, project, editor, file)
        ref?.performRefactoring(project, editor, file)
        print("done")
        print(ifStatement.isValid)
        print(ifStatement.isPhysical)

        if2switch.performRefactoring(project, editor, file) // this doesn't refactor the code.

        val foundIfStatement = PsiUtils.searchForPsiElement(file, ifStatement)
        assert(foundIfStatement != null)
        val newRefactoring = if2switch.recalibrateRefactoring(project, editor, file)!!
        newRefactoring.performRefactoring(project, editor, file)

        print(file.text)
        assert(
            file.text.contains(
                "while (i < prices.length) {\n" +
                    "                    double price = prices[i];\n" +
                    "                    int quantity = quantities[i];\n" +
                    "                    double itemTotal = price * quantity;\n" +
                    "                    switch (customerType) {\n" +
                    "                        case 1 -> itemTotal *= (1 - discountRate);\n" +
                    "                        case 2 -> itemTotal *= (1 - (discountRate / 2));\n" +
                    "                        case 3 -> itemTotal *= (1 - (discountRate * 2));\n" +
                    "                    }\n" +
                    "                    sum += itemTotal;\n" +
                    "                    i++;\n" +
                    "                }",
            ),
        )
    }
}
