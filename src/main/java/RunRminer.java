import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.*;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class RunRminer {

    public static void main(String[] args) throws Exception {
        GitService gitService = new GitServiceImpl();
        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();

//        Repository repo = gitService.cloneIfNotExists(
//                "tmp/refactoring-toy-example",
//                "https://github.com/danilofes/refactoring-toy-example.git");
        Repository repo = gitService.openRepository("/Users/abhiram/Documents/TBE/evaluation_projects/elasticsearch");
        JsonArray allCommits = new JsonArray();

        miner.detectBetweenCommits(repo, "87e257e8739f64d61f13ed01f0c9e72dad65c5d9", "d59ca365cf27d8b26713eaa7269701360538c724",
//        miner.detectAtCommit(repo, "87e257e8739f64d61f13ed01f0c9e72dad65c5d9",
                new RefactoringHandler() {
            @Override
            public void handleModelDiff(String commitId, List<Refactoring> refactorings, UMLModelDiff modelDiff) {
                System.out.println("Refactorings at " + commitId);
                JsonObject commitObj = new JsonObject();

                commitObj.addProperty("sha1", commitId);
                JsonArray allRefactorings = new JsonArray();
                for (Refactoring ref : refactorings) {
                    System.out.println(ref.toString());
                    PurityCheckResult result = PurityChecker.check(ref, refactorings, modelDiff);
                    JsonObject obj = new JsonObject();
                    obj.add("ref", JsonParser.parseString(ref.toJSON()));

                    if (result != null) {
                        System.out.println(result);
                        obj.addProperty("purity", result.toString());
                    }else{
                        obj.addProperty("purity", "unknown");
                    }
                    allRefactorings.add(obj);
                }
                commitObj.add("refactorings", allRefactorings);
                allCommits.add(commitObj);
            }
        });
        System.out.println(allCommits.toString());
        Files.write(Path.of("/Users/abhiram/Documents/TBE/evaluation_projects/elastic-rminer_test.json"), allCommits.toString().getBytes(StandardCharsets.UTF_8));
    }
}
