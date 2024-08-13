import com.intellij.ml.llm.template.GitCommitAnalyser;
import com.intellij.ml.llm.template.utils.GitUtils;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import java.io.File;
import java.io.IOException;

public class AnalyseGitProject {
    public static void main(String[] args) throws Exception {
        String projectPath = args[0];
        try {
            findInterestingCommits(projectPath);
        } catch (Exception e){
            e.printStackTrace();
            throw e;
        }
    }

    private static void findInterestingCommits(String projectPath) throws IOException {
//        Repository repo = new FileRepositoryBuilder()
//                .setGitDir(new File(projectPath+"/.git"))
//                .build();
        new GitCommitAnalyser(projectPath).findInterestingCommits();
    }

}
