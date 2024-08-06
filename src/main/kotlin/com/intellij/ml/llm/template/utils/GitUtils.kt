package com.intellij.ml.llm.template.utils

import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.MethodDeclaration
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.diff.Edit
import org.eclipse.jgit.lib.ObjectChecker.tree
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ObjectLoader
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.patch.FileHeader
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.treewalk.filter.PathFilter
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter
import org.eclipse.jgit.util.io.DisabledOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException


class GitUtils {
    companion object{
        fun getLatestCommit(projectPath: String): String{
            val f = FileRepositoryBuilder()
                .setGitDir(File("$projectPath/.git"))
                .build()
            val commit = Git(f).log().setMaxCount(1).call().iterator().next()

            return ""
        }
        fun t(){

        }

        fun getDiffsInLatestCommit(projectPath: String): List<DiffEntry>{
            val repo = FileRepositoryBuilder()
                .setGitDir(File("$projectPath/.git"))
                .build()


            val oldHead: ObjectId = repo.resolve("HEAD^^{tree}")
            val head: ObjectId = repo.resolve("HEAD^{tree}")

            val diffStrings = mutableListOf<String>()

            val diffs = getDiffEntries(repo, oldHead, head, diffStrings)
            val diffFormatter = DiffFormatter(DisabledOutputStream.INSTANCE)
            diffFormatter.setRepository(repo)
            diffFormatter.setContext(0)
//            val entries = diffFormatter.scan(newTreeIter, oldTreeIter)
            val hunks = diffs
                .map{ diffFormatter.toFileHeader(it) }
                .sortedBy { it.toEditList().map { x -> x.endB-x.beginB }.sum() }
            return hunks
        }

        private fun getDiffEntries(
            repo: Repository,
            oldHead: ObjectId,
            head: ObjectId,
            diffStrings: MutableList<String>
        ): MutableList<DiffEntry> {
            val diffs = repo.newObjectReader().use { reader ->
                val oldTreeIter =
                    CanonicalTreeParser()
                oldTreeIter.reset(reader, oldHead)
                val newTreeIter =
                    CanonicalTreeParser()
                newTreeIter.reset(reader, head)

                Git(repo).use { git ->
                    val diffs = git.diff()
                        .setNewTree(newTreeIter)
                        .setOldTree(oldTreeIter)
                        .call()

                    for (entry in diffs) {
                        val out: ByteArrayOutputStream = ByteArrayOutputStream()
                        DiffFormatter(out).use { formatter ->
                            formatter.setRepository(repo)
                            formatter.format(entry)
                            System.out.println(out.toString())
                            diffStrings.add(out.toString())
                        }
                        println("Entry: $entry")
                    }
                    diffs
                }
            }
            return diffs
        }

        fun sortDiffsBySize(diffs: List<DiffEntry>){

        }

        fun getLast2Commits(projectPath: String) {
            val repo = FileRepositoryBuilder()
                .setGitDir(File("$projectPath/.git"))
                .build()
            val iterator = Git(repo).log().setMaxCount(2).call().iterator()
        }

        fun getFileContentsFromHunk(
            repository: Repository,
            tree: RevTree,
            hunk: FileHeader
        ): String {
            return TreeWalk(repository).use { treeWalk ->
                treeWalk.addTree(tree)
                treeWalk.setRecursive(true)
                treeWalk.setFilter(PathFilter.create(hunk.newPath))
                check(treeWalk.next()) { "Did not find expected file ${hunk.newPath}" }

                val objectId: ObjectId = treeWalk.getObjectId(0)
                val loader: ObjectLoader = repository.open(objectId)
                val outStream = ByteArrayOutputStream()
                // and then one can the loader to read the file
                loader.copyTo(outStream)
                outStream.toString()
            }
        }

        @Throws(IOException::class)
        fun prepareTreeParser(repository: Repository, objectId: String): AbstractTreeIterator {
            // from the commit we can build the tree which allows us to construct the TreeParser
            RevWalk(repository).use { walk ->
                val commit =
                    walk.parseCommit(ObjectId.fromString(objectId))
                val tree = walk.parseTree(commit.tree.id)

                val treeParser = CanonicalTreeParser()
                repository.newObjectReader().use { reader ->
                    treeParser.reset(reader, tree.id)
                }
                walk.dispose()
                return treeParser
            }
        }

        private fun checkForLongMethod(d: DiffEntry): Boolean {
            TODO("Not yet implemented")
        }

    }

    fun listChangesInCommits(repository: Repository) {
        try {
            Git(repository).use { git ->
                val commits = git.log().call()
                val revWalk = RevWalk(repository)

                for (commit in commits) {
                    println("Commit: " + commit.name)
                    println("Author: " + commit.authorIdent.name)
                    println("Date: " + commit.authorIdent.getWhen())
                    println("Message: " + commit.fullMessage)

                    if (commit.parentCount > 0) {
                        val parent = revWalk.parseCommit(commit.getParent(0).id)
                        val oldTreeIter: AbstractTreeIterator =
                            prepareTreeParser(repository, parent.tree.id.name)
                        val newTreeIter: AbstractTreeIterator =
                            prepareTreeParser(repository, commit.tree.id.name)

                        DiffFormatter(ByteArrayOutputStream()).use { formatter ->
                            formatter.setRepository(repository)
                            val entries =
                                formatter.scan(oldTreeIter, newTreeIter)
                            for (entry in entries) {
                                println("Change: " + entry.changeType + " " + entry.newPath)
                            }
                        }
                    }

                    println()
                }
                revWalk.dispose()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


}