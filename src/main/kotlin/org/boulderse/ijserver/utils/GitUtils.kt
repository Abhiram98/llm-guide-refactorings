package org.boulderse.ijserver.utils

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.util.io.DisabledOutputStream
import java.io.File


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
            val iterator = Git(repo).log().setMaxCount(2).call().iterator()
            val latestCommit: RevCommit =
                iterator.next()
            val secondLastCommit: RevCommit =
                iterator.next()

//            val head = latestCommit.toObjectId()
//            val oldHead = secondLastCommit.toObjectId()

//            val oldHead: ObjectId = repo.resolve(secondLastCommit.name)
//            val head: ObjectId = repo.resolve(latestCommit.name)
            val oldHead: ObjectId = repo.resolve("HEAD^^{tree}")
            val head: ObjectId = repo.resolve("HEAD^{tree}")

            val revWalk: RevWalk = RevWalk(repo)
            val commit1: RevCommit = revWalk.parseCommit(latestCommit)
            val commit2: RevCommit = revWalk.parseCommit(secondLastCommit)

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
                        println("Entry: $entry")
                    }
                    diffs
                }
            }
            return diffs
        }

        fun sortDiffsBySize(diffs: List<DiffEntry>){
            diffs[0]
            val diffFormatter: DiffFormatter = DiffFormatter(DisabledOutputStream.INSTANCE)
//            diffFormatter.setRepository(git.getRepository())
            diffFormatter.toFileHeader(diffs.get(0));
        }
    }
}