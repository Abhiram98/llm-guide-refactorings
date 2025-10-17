package org.boulderse.ijserver.intentions

import org.boulderse.ijserver.benchmark.CreateBenchmarkForFile
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase


class CreateBenchmarkForFileIntentionTest: LightJavaCodeInsightFixtureTestCase() {

    override fun getTestDataPath(): String {
        return "/Users/abhiram/Documents/TBE/evaluation_projects/elasticsearch"
    }

    protected fun doTest(testName: String, hint: String?) {
        myFixture.configureByFile("$testName.java")
        val action = myFixture.findSingleIntention(hint!!)
        assert(action!=null)
        myFixture.launchAction(action)
        myFixture.checkResultByFile("$testName.after.java")
    }

    fun testIntention() {
        doTest("before.template", "SDK: Convert ternary operator to if statement")
    }

    fun testBenchmarkCreation(){

//        GitHistoryRefactoringMinerImpl x = new GitHistoryRefactoringMinerImpl();
//        x.detectBetweenCommits();

        // read RMiner output.
        // For each "interesting file", that is: >=5 refactorings, in a single commit
        // Go to commit
        // Create inverse refactoring objects
        // Execute them. Remember which ones were executed successfully.
        // create branch, create commit and save this information somewhere.
        val projectDir = "/Users/abhiram/Documents/TBE/evaluation_projects/elasticsearch"
        val refminerOut = "/Users/abhiram/Documents/TBE/evaluation_projects/elastic-interesting-files.json"
        myFixture.configureByFile("server/src/main/java/org/elasticsearch/index/translog/Translog.java")

//        CreateBenchmarkForFile(projectDir, project, editor, file).create()
    }
}