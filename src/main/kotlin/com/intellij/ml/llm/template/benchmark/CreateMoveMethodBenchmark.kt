package com.intellij.ml.llm.template.benchmark

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.refactoringobjects.movemethod.MoveMethodFactory
import com.intellij.ml.llm.template.utils.MethodSignature
import com.intellij.ml.llm.template.utils.PsiUtils
import com.intellij.ml.llm.template.utils.PsiUtils.Companion.isMethodStatic
import com.intellij.ml.llm.template.utils.getExecutionOrder
import com.intellij.ml.llm.template.utils.openFile
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.PsiJavaFileImpl
import org.jetbrains.kotlin.j2k.getContainingClass

class CreateMoveMethodBenchmark(filename: String,
                                project: Project, editor: Editor, file: PsiFile,
                                refactorings: JsonArray
): CreateBenchmarkForFile(filename, project, editor, file, refactorings) {
    val currentQualifiedClass = (file as PsiJavaFileImpl).classes[0].qualifiedName!!
    val currentClassType = currentQualifiedClass.split(".").last()

    override fun create(){
        val allRefactoringObjects = mutableListOf<AbstractRefactoring>()
        for (refactoring in refactorings) {
            val refID = refactoring.asJsonObject.get("refID").asInt
            val refactoringsObjects = createMoveMethodInverse(refactoring)
            refactoringsObjects.map { refObjectToRefIDMap[it] = refID }
            allRefactoringObjects.addAll(refactoringsObjects)
        }
        // Execute all inverse refactorings
        executeReverse(allRefactoringObjects)
    }

    private fun createMoveMethodInverse(refactoring: JsonElement): List<AbstractRefactoring> {
        if (refactoring.asJsonObject.get("type").asString!="Move Method") return emptyList()

        val movedMethodDeclaration = refactoring.asJsonObject.get("rightSideLocations").asJsonArray
            .filter { it.asJsonObject.get("description").asString == "moved method declaration" }
        val newFile = movedMethodDeclaration
            .map {it.asJsonObject.get("filePath").asString}
        if (newFile.isEmpty()) return emptyList()

        val newMethodSignatureString = movedMethodDeclaration.map { it.asJsonObject.get("codeElement").asString }
        if (newMethodSignatureString.isEmpty()) return emptyList()

        val newMethodSignature = MethodSignature.getMethodSignatureParts(newMethodSignatureString[0]) ?: return emptyList()


        // TODO: Open right file, identify method.
        val movedEditorAndFile = openFile(newFile[0], project)
        val movedEditor = movedEditorAndFile.first
        val movedFile = movedEditorAndFile.second

        val movedMethod = PsiUtils.getMethodWithSignatureFromClass((movedFile as PsiJavaFileImpl).classes[0], newMethodSignature) ?: return emptyList()

        if (isMethodStatic(movedMethod)){
            // undo static move
            return MoveMethodFactory.createStaticMove(movedMethod, movedEditor, currentQualifiedClass)
        } else {
            // find pivot variable
            // undo
            print("unsupported")
            val undoMoves = mutableListOf<AbstractRefactoring>()

            for (param in  movedMethod.parameterList.parameters){

                if (param.type.canonicalText == currentQualifiedClass){
                    println("found move from parameter.")
                    undoMoves.addAll(
                        MoveMethodFactory.createInstanceMoveMethodRefactorings(param, project, movedMethod, movedEditor)
                    )
                }
            }

            for (field in (movedFile as PsiJavaFileImpl).classes[0].allFields){
                if (field.type.canonicalText == currentQualifiedClass){
                    println("found move from field")
                    undoMoves.addAll(
                        MoveMethodFactory.createInstanceMoveMethodRefactorings(field, project, movedMethod, movedEditor)
                    )
                }
            }
            return undoMoves
        }
    }
}