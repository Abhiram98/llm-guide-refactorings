package org.boulderse.ijserver.utils

import org.junit.Test

class GitUtilsTest{
    @Test
    fun testGitUtils(){
        val diffs = GitUtils.getDiffsInLatestCommit("/Users/abhiram/Documents/TBE/evaluation_projects/elasticsearch")
        GitUtils.sortDiffsBySize(diffs)
    }
}