package org.boulderse.ijserver.refactoringobjects.conditionals

import com.intellij.testFramework.LightPlatformCodeInsightTestCase

class If2SwitchFactoryTest : LightPlatformCodeInsightTestCase() {
    private var projectPath = "src/test"

    override fun getTestDataPath(): String = projectPath

    fun testSwitch2If() {
        configureByFile("/testdata/HelloWorld.java")

        val refObjs =
            Switch2IfFactory.createObjectsFromFuncCall(
                "convert_switch2if(21)",
                project,
                editor,
                file,
            )
        assert(refObjs.isNotEmpty())
        refObjs[0].performRefactoring(project, editor, file)

        println(file.text)
        assert(
            file.text.contains(
                "    public void prettyPrintInteger(Integer num){\n" +
                    "        if (num == 1) {\n" +
                    "            System.out.println(\"ONE!!\");\n" +
                    "        } else if (num == 2) {\n" +
                    "            System.out.println(\"TWO!!\");\n" +
                    "        } else if (num == 5) {\n" +
                    "            System.out.println(\"FIVE!!\");\n" +
                    "        } else {\n" +
                    "            System.out.println(num);\n" +
                    "        }\n" +
                    "    }",
            ),
        )
    }

    fun testIf2Switch() {
        configureByFile("/testdata/HelloWorld.java")

        val refObjs =
            If2Switch.factory.createObjectsFromFuncCall("convert_if2switch(29)", project, editor, file)
        assert(refObjs.isNotEmpty())
        refObjs[0].performRefactoring(project, editor, file)
    }

    fun testIf2Ternary() {
        configureByFile("/testdata/HelloWorld.java")

        val refObjs =
            If2Ternary.factory.createObjectsFromFuncCall("convert_if2ternary(41)", project, editor, file)
        assert(refObjs.isNotEmpty())
        refObjs[0].performRefactoring(project, editor, file)
        println(file.text)
        assert(
            file.text.contains(
                "    public Integer numMinus10(Integer num){\n" +
                    "        return num >= 0 ? num - 10 : num + 10;\n" +
                    "    }",
            ),
        )
    }

    fun testTernary2If() {
        configureByFile("/testdata/HelloWorld.java")

        val refObjs =
            Ternary2If.factory.createObjectsFromFuncCall(
                "convert_ternary2if(48)",
                project,
                editor,
                file,
            )
        assert(refObjs.isNotEmpty())
        refObjs[0].performRefactoring(project, editor, file)
        println(file.text)
        assert(
            file.text.contains(
                "    public Integer numMinus10Ternary(Integer num){\n" +
                    "        if (num >= 0) return num - 10;\n" +
                    "        return num + 10;\n" +
                    "    }",
            ),
        )
    }
}
