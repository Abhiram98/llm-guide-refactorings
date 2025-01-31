package com.intellij.ml.llm.template.benchmark

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.refactoringobjects.UncreatableRefactoring
import com.intellij.ml.llm.template.refactoringobjects.extractfunction.InlineMethodFactory
import com.intellij.ml.llm.template.refactoringobjects.renamevariable.RenameVariableFactory
import com.intellij.ml.llm.template.utils.MethodSignature
import com.intellij.ml.llm.template.utils.MethodSignature.Companion.getMethodSignatureParts
import com.intellij.ml.llm.template.utils.Parameter
import com.intellij.ml.llm.template.utils.PsiUtils
import com.intellij.ml.llm.template.utils.getExecutionOrder
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.PsiJavaFileImpl
import io.ktor.util.reflect.*

open class CreateBenchmarkForFile(
//    val projectPath: String,
//    val refMinerOut: String,
    val filename: String,
    val project: Project, val editor: Editor, val file: PsiFile,
    val refactorings: JsonArray
) {
    val refObjectToRefIDMap = mutableMapOf<AbstractRefactoring, Int>()
    val statusMap = mutableMapOf<Int, Pair<Boolean, String>>()

    open fun create(){
        val allRefactoringObjects = mutableListOf<AbstractRefactoring>()
        for (refactoring in refactorings) {
            val refID = refactoring.asJsonObject.get("refID").asInt
            val refactoringsObjects = createReverseRefactoringObject(refactoring)
            refactoringsObjects.map { refObjectToRefIDMap[it] = refID }
            allRefactoringObjects.addAll(refactoringsObjects)
        }
        // Execute all inverse refactorings
        // TODO: deduplicate refactoring objects.
        //  Ex: Extract method can extract the same method from two separate places (refactoring miner reports it twice)
        executeReverse(getExecutionOrder(allRefactoringObjects))

        // Undo move methods.
        val moveMethodBenchmark = CreateMoveMethodBenchmark(filename, project, editor, file, refactorings)
        moveMethodBenchmark.create()
        statusMap.putAll(moveMethodBenchmark.statusMap)
    }



    private fun createReverseRefactoringObject(refactoring: JsonElement): List<AbstractRefactoring> {
        val inverseRefactorings = when(refactoring.asJsonObject.get("type").asString){
            "Extract Method" -> getExtractMethodInverse(refactoring)
            "Rename Variable" -> getRenameVariableInverse(refactoring)
            "Rename Method" -> getRenameMethodInverse(refactoring)
            "Rename Parameter" -> getRenameParameterInverse(refactoring)
            else -> {getUnsupportedRefactoring(refactoring)}
        }
        return inverseRefactorings

    }

    private fun getRenameParameterInverse(refactoring: JsonElement): List<AbstractRefactoring> {
        val refJsonObj = refactoring.asJsonObject
        
        val oldVariableDeclaration = refactoring.asJsonObject
            .get("leftSideLocations").asJsonArray
            .filter { it.asJsonObject.get("description").asString.equals("original variable declaration") }
            .map{ it.asJsonObject.get("codeElement").asString}
            .get(0)
        val splitColonOld = oldVariableDeclaration.split(":")
        val oldParam = Parameter(name= splitColonOld[0].trim(), type = splitColonOld[1].trim())

        val newVariableDeclaration = refactoring.asJsonObject
            .get("rightSideLocations").asJsonArray
            .filter { it.asJsonObject.get("description").asString.equals("renamed variable declaration") }
            .map{ it.asJsonObject.get("codeElement").asString}
            .get(0)
        val newSplitColon = newVariableDeclaration.split(":")
        val newParam = Parameter(name= newSplitColon[0].trim(), type = newSplitColon[1].trim())

        val methodSignatureString = refactoring.asJsonObject
            .get("rightSideLocations").asJsonArray
            .filter { it.asJsonObject.get("description").asString.equals("method declaration with renamed variable") }
            .map{ it.asJsonObject.get("codeElement").asString}
            .get(0)
        val methodSignature = MethodSignature.getMethodSignatureParts(methodSignatureString)



        val psiClass = (file as PsiJavaFileImpl).classes
        if (psiClass.size==0)
            return emptyList()

        if (methodSignature!=null) {
            val refObj = RenameVariableFactory.renameParameter(
                methodSignature, psiClass[0], newParam, oldParam
            )
            if (refObj!=null)
                return listOf(refObj)
        }

        return emptyList()
    }


    private fun getRenameMethodInverse(refactoring: JsonElement): List<AbstractRefactoring> {

        val refJsonObj = refactoring.asJsonObject
        val oldNewSplit = refJsonObj.get("description").asString
            .split("renamed to")
        val oldMethodSignature = getMethodSignatureParts(oldNewSplit[0])
        val newMethodSignature = getMethodSignatureParts(oldNewSplit[1])
        val psiClass = (file as PsiJavaFileImpl).classes
        if (psiClass.size==0)
            return emptyList()

        if (oldMethodSignature!=null && newMethodSignature!=null) {
            val refObj = RenameVariableFactory.renameMethod(
                newMethodSignature.methodName, psiClass[0], oldMethodSignature.methodName, newMethodSignature
            )
            if (refObj!=null)
                return listOf(refObj)
        }

        return emptyList()
    }

    private fun getRenameVariableInverse(refactoring: JsonElement): List<AbstractRefactoring> {
        val refJsonObj = refactoring.asJsonObject
        val methodSignature = refJsonObj.get("description").asString
            .split("in method")[1].split("from class")[0]
        val signature = getMethodSignatureParts(methodSignature)
        if (signature!=null){
            val oldName = refactoring.asJsonObject
                .get("leftSideLocations").asJsonArray
                .filter { it.asJsonObject.get("description").asString.equals("original variable declaration") }
                .map{ it.asJsonObject.get("codeElement").asString.split(":")[0].trim()}
                .get(0)

            val newName = refactoring.asJsonObject
                .get("rightSideLocations").asJsonArray
                .filter { it.asJsonObject.get("description").asString.equals("renamed variable declaration") }
                .map{ it.asJsonObject.get("codeElement").asString.split(":")[0].trim()}
                .get(0)

            val psiClass = (file as PsiJavaFileImpl).classes
            if (psiClass.size==0)
                return emptyList()
            val psiMethod = PsiUtils.getMethodWithSignatureFromClass(
                psiClass[0], signature
            )
            if (psiMethod!=null)
                return listOf(
                    RenameVariableFactory.fromOldNewName(
                        project, psiMethod, newName, oldName
                    )?:UncreatableRefactoring(1, 1, refJsonObj.get("description").asString)
                )
            return emptyList()
        }else{
            return emptyList()
        }
    }

    private fun getUnsupportedRefactoring(refactoring: JsonElement): List<AbstractRefactoring> {
        println("Unsupported refactoring: ${refactoring.asJsonObject.get("description").asString}")
        return listOf(
            UncreatableRefactoring(1, 1, refactoring.asJsonObject.get("description").asString)
        )
    }

    private fun getExtractMethodInverse(refactoring: JsonElement): List<AbstractRefactoring>{
        val extractedCodeElement = refactoring.asJsonObject
            .get("rightSideLocations").asJsonArray
            .filter { it.asJsonObject.get("description").asString.equals("extracted method declaration") }
            .map{ it.asJsonObject.get("codeElement").asString}
            .get(0)
        val signature = getMethodSignatureParts(extractedCodeElement)
        val psiClass = (file as PsiJavaFileImpl).classes
        if (psiClass.size==0 || signature==null) {
            println("Failed to create Inline: ${refactoring.asJsonObject.get("description").asString}")
            return emptyList()
        }
        return InlineMethodFactory.fromMethodSignature(
            file, editor, signature)

    }

    fun executeReverse(refObjects: List<AbstractRefactoring>){
        for (r in refObjects){
            try {
                if (r is UncreatableRefactoring){
                    statusMap[refObjectToRefIDMap[r]!!] = Pair(false, "Failed to create refactoring object")
                }
                else {
                    r.performRefactoring(project, editor, file)

                    statusMap[refObjectToRefIDMap[r]!!] = Pair(true, "success")
                }
            } catch (e: Exception) {
                println("Failed to reverse ${r.getRefactoringPreview()}")
                e.printStackTrace()
                statusMap[refObjectToRefIDMap[r]!!] = Pair(false, e.stackTraceToString())
            }
        }
    }
}