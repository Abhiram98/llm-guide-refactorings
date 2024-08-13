package com.intellij.ml.llm.template

import ai.grazie.client.common.logging.qualifiedName
import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.body.MethodDeclaration
import com.intellij.ml.llm.template.utils.GitUtils
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.diff.Edit
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.patch.FileHeader
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter
import org.eclipse.jgit.util.io.DisabledOutputStream
import java.io.File


class GitCommitAnalyser(val repositoryPath:String) {

    data class InterestingCommmit(
        val methodName: String, val filePath: String,
        val editStart: Int, val editEnd: Int,
        val methodStart: Int, val methodEnd: Int)

    val repository: Repository = FileRepositoryBuilder()
        .setGitDir(File("$repositoryPath/.git"))
        .build()
    val COMMIT_LIMIT = 500
    val THRESHOLD = 20
    val METHOD_THRESHOLD_SIZE = 30
    val interestingCommits = mutableMapOf<RevCommit, MutableList<InterestingCommmit>>()


    fun findInterestingCommits(){

        val commits: Iterable<RevCommit> = Git(repository).log().all().call()
        var count = 0
        for (commit in commits) {
            if (count>COMMIT_LIMIT)
                break
            println("LogCommit: $commit")
            count++
            try {
                analyseTwoCommits(repository, commit.getParent(0), commit)
            } catch (e: ArrayIndexOutOfBoundsException){
                break
            }
        }
        println(interestingCommits)
        println(count)
    }

    private fun analyseTwoCommits(repository: Repository, oldCommit: RevCommit, newCommit: RevCommit ): Boolean{
        val oldTreeParser = GitUtils.prepareTreeParser(repository, oldCommit.name)
        val newTreeParser = GitUtils.prepareTreeParser(repository, newCommit.name)

        Git(repository).use { git ->
            val diff: List<DiffEntry> = git.diff()
                .setOldTree(oldTreeParser)
                .setNewTree(newTreeParser)
                .setPathFilter(PathSuffixFilter.create(".java")).call()

            for (entry in diff) {
                println("Entry: " + entry + ", from: " + entry.oldId + ", to: " + entry.newId)
                DiffFormatter(System.out).use { formatter ->
                    formatter.setRepository(repository)
                    formatter.format(entry)
                }
            }
            val diffFormatter = DiffFormatter(DisabledOutputStream.INSTANCE)
            diffFormatter.setRepository(repository)
            diffFormatter.setContext(0)
            val hunks = diff
                .filter { !it.newPath.lowercase().contains("test") }
                .map{ diffFormatter.toFileHeader(it) }
                .filter {
                    (it.toEditList()
                        .filter { x -> x.type == Edit.Type.INSERT || x.type == Edit.Type.REPLACE }
                        .map { x -> x.endB - x.beginB }.maxOrNull() ?: 0) > THRESHOLD
                }
            if (hunks.size>0 && hasInterestingHunk(hunks, repository, newCommit.tree, revCommit = newCommit)){
                return true
            }
        }
        return false
    }
    private fun hasInterestingHunk(hunks: List<FileHeader>, repository: Repository,
                                   tree: RevTree, revCommit: RevCommit
    ): Boolean {
        var interesting = false
        for (hunk in hunks){
            val edits = hunk.toEditList()
            val bigEdits = edits
                .filter { x -> x.type == Edit.Type.INSERT || x.type == Edit.Type.REPLACE }
                .filter { x -> (x.endB - x.beginB) > THRESHOLD }

            val fileContents = GitUtils.getFileContentsFromHunk(repository, tree, hunk)
            val parser = JavaParser(ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_19))
            val compilationUnit = parser.parse(fileContents).result.get()
            val largeMethods = compilationUnit.findAll(MethodDeclaration::class.java)
//                .map { it.range.get() }
                .filter { it.range.get().lineCount > METHOD_THRESHOLD_SIZE }

            for (method in largeMethods){
                val methodRange = method.range.get()
                for (edit in bigEdits){
                    if (edit.beginB >= methodRange.begin.line &&
                        edit.endB <= methodRange.end.line){ // edited a method.
                        if ((edit.endB - edit.beginB).toDouble()/methodRange.lineCount >= 0.3) {
                            // edited more than half of a method.
                            addInfo(revCommit,
                                InterestingCommmit(method.nameAsString, hunk.newPath,
                                    edit.beginB, edit.endB,
                                    methodRange.begin.line, methodRange.end.line))
                            interesting = true
                        }
                    }
                    if (methodRange.begin.line >= edit.beginB  &&
                        methodRange.end.line <= edit.endB){ // method inside a larger edit
                        addInfo(revCommit,
                            InterestingCommmit(method.nameAsString, hunk.newPath,
                                edit.beginB, edit.endB,
                                methodRange.begin.line, methodRange.end.line))
                        interesting = true
                    }
                }
            }

        }
        return interesting
    }

    fun addInfo(revCommit: RevCommit, interestingCommmit: InterestingCommmit){
        if(interestingCommits.get(revCommit)!=null){
            interestingCommits.get(revCommit)!!.add(interestingCommmit)
        }else{
            val infoList = mutableListOf(interestingCommmit)
            interestingCommits.put(revCommit, infoList)
        }
    }



}