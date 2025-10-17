import com.github.javaparser.StaticJavaParser;
import org.boulderse.ijserver.utils.JavaParsingUtils;

import java.io.File;
import java.io.FileNotFoundException;

public class GetStaticMethods {
    public static void main(String[] args) throws FileNotFoundException {
        String filePath = "";
        JavaParsingUtils.Companion.parseFile("/Users/abhiram/Documents/TBE/jmove/dataset-tse/lucene/large/big/lucene-4.2.0/core/src/java/org/apache/lucene/util/AttributeSource.java");
    }
}
