package org.boulderse.ijserver.refactoringobjects.renamevariable

import org.junit.Test
import org.junit.jupiter.api.Assertions.*

class RenameVariableFactoryTest{

    @Test
    fun testParamExtraction(){
        val params =
            RenameVariableFactory.getParamsFromFuncCall("rename_variable(\"olaName\", 'newName')")
        println(params)
        assert(params[0]=="\"olaName\"")
        assert(params[1]=="\'newName\'")
        assert(RenameVariableFactory.getStringFromParam(params[0])=="olaName")
        assert(RenameVariableFactory.getStringFromParam(params[1])=="newName")
    }

    @Test
    fun testKWParamExtraction(){
        val params =
            RenameVariableFactory.getParamsFromFuncCall("rename_variable(old_name=\"olaName\", new_name = 'newName')")
        println(params)
        assert(params[0]=="\"olaName\"")
        assert(params[1]=="\'newName\'")
        assert(RenameVariableFactory.getStringFromParam(params[0])=="olaName")
        assert(RenameVariableFactory.getStringFromParam(params[1])=="newName")
    }

    @Test
    fun testIntStringParamExtraction(){
        val params =
            RenameVariableFactory.getParamsFromFuncCall("rename_variable(old_name=\"olaName\", new_name = 'newName', 1, 2)")
        println(params)
        assert(params[0]=="\"olaName\"")
        assert(params[1]=="\'newName\'")
        assert(params[2]=="1")
        assert(params[3]=="2")
    }

}