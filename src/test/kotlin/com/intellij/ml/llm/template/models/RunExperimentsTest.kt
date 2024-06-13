package com.intellij.ml.llm.template.models

import org.junit.Test

class RunExperimentsTest{

    @Test
    fun testRun(){
        RunExperiments(temperature = 0.7, iterations = 5, moveMethodsFile = "/data/move_methods_selected.csv").runAllAndSave()
    }

    @Test
    fun testRunAll(){
        RunExperiments(temperature = 0.7, iterations = 5, moveMethodsFile = "/data/move_methods_all.csv").runAllAndSave()
    }

    @Test
    fun testGetApiSelected(){
        GetRefactoringObjects(
            "data/moveMethodResponses(2).json",
            "data/classSourcesSelected.json").run()
    }

    @Test
    fun testGetApiAll(){
        GetRefactoringObjects(
            "data/moveMethodResponses(5).json",
            "data/classSourcesAll.json",
            "data/apiResponses(2).json"
        ).run()
    }
}