package org.boulderse.ijserver.utils

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class RefMinerParser(
    val filePath: Path,
    val projectBasePath: Path,
) {
    val refData: JsonElement = JsonParser.parseString(Files.readString(filePath))
    val commitsData = refData.asJsonObject.getAsJsonArray("commits")
    val repo =
        FileRepositoryBuilder()
            .setGitDir(File("$projectBasePath/.git"))
            .build()
    val gitRepo = Git(repo)

    fun getStaticMoveMethods() {
//        val staticMethodCommits = listOf()
        commitsData.forEach {
            gitRepo
                .checkout()
                .setName(it.asJsonObject.get("sha1").asString)
                .setForced(true)
                .call()
            val refactorings = it.asJsonObject.getAsJsonArray("refactorings")
            val moveMethods =
                refactorings.filter { ref ->
                    ref.asJsonObject.get("type").asString == "Move Method"
                }
            val staticMoveMethods = moveMethods.filter { isStaticMove(it.asJsonObject) }
            staticMoveMethods.forEach { println(it.asJsonObject.get("refID")) }
        }
    }

    fun isStaticMove(refactoring: JsonObject): Boolean {
        val movedMethodDeclaration =
            refactoring.asJsonObject
                .get("rightSideLocations")
                .asJsonArray
                .filter { it.asJsonObject.get("description").asString == "moved method declaration" }
        val newFile =
            movedMethodDeclaration
                .map { it.asJsonObject.get("filePath").asString }
        if (newFile.isEmpty()) return false

        val newMethodSignatureString = movedMethodDeclaration.map { it.asJsonObject.get("codeElement").asString }
        if (newMethodSignatureString.isEmpty()) return false

        val newMethodSignature = MethodSignature.getMethodSignatureParts(newMethodSignatureString[0]) ?: return false

        return JavaParsingUtils.isMethodStatic(
            Path.of(projectBasePath.toString() + "/" + newFile[0]),
            newMethodSignature,
        )
    }
}
