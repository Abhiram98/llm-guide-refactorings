package org.boulderse.ijserver.utils

import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import kotlin.io.path.Path

class JavaParsingUtilsTest {
    @Test
    fun testIsStatic() {
        val filePath = "/Users/abhiram/Documents/TBE/RefactoringMiner/src/main/java/org/refactoringminer/util/AstUtils.java"
        val outPath = "/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/refminer_data/isStaticOut.txt"
        val signature = "public getKeyFromMethodBinding(binding IMethodBinding) : String"
        assertTrue(
            JavaParsingUtils.isMethodStatic(Path(filePath), signature),
        )
    }

    @Test
    fun testIsStatic2() {
        val filePath = "/Users/abhiram/Documents/TBE/evaluation_projects/flink/flink-runtime/src/main/java/org/apache/flink/runtime/clusterframework/BootstrapTools.java"
        val signature = "public getTaskManagerShellCommand(flinkConfig Configuration, tmParams ContaineredTaskManagerParameters, configDirectory String, logDirectory String, hasLogback boolean, hasLog4j boolean, hasKrb5 boolean, mainClass Class<?>, mainArgs String) : String"
        assertTrue(
            JavaParsingUtils.isMethodStatic(Path(filePath), signature),
        )
    }

    @Test
    fun testIsStatic3() {
        val filePath = "/Users/abhiram/Documents/TBE/evaluation_projects/flink/flink-runtime/src/main/java/org/apache/flink/runtime/clusterframework/BootstrapTools.java"
        val signature = "public getStartCommand(template String, startCommandValues Map<String,String>) : String"
        assertTrue(
            JavaParsingUtils.isMethodStatic(Path(filePath), signature),
        )
    }

    @Test
    fun testIsStatic4() {
        val filePath = "/Users/abhiram/Documents/TBE/evaluation_projects/elasticsearch/server/src/main/java/org/elasticsearch/search/aggregations/bucket/BestBucketsDeferringCollector.java"
        val signature = "package Entry(aggCtx AggregationExecutionContext, docDeltas PackedLongValues, buckets PackedLongValues) : record"
        assertFalse(
            JavaParsingUtils.isMethodStatic(Path(filePath), signature),
        )
    }

    @Test
    fun testIsStatic5() {
        val filePath = "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/streams/src/main/java/org/apache/kafka/streams/state/internals/RocksDBStore.java"
        val signature = "public flush(accessor DBAccessor) : void"
        assertFalse(
            JavaParsingUtils.isMethodStatic(Path(filePath), signature),
        )
    }

    @Test
    fun testIsStaticClass() {
        val filePath = "/Users/abhiram/Documents/TBE/evaluation_projects/flink/flink-runtime/src/main/java/org/apache/flink/runtime/clusterframework/BootstrapTools.java"
        val className = "org.apache.flink.runtime.clusterframework.BootstrapTools"
        assertFalse(
            JavaParsingUtils.isClassStatic(Path(filePath), className),
        )
    }

    @Test
    fun testIsStaticClass2() {
        val filePath = "/Users/abhiram/Documents/TBE/evaluation_projects/flink/flink-runtime/src/main/java/org/apache/flink/runtime/io/network/partition/hybrid/HsSubpartitionFileReaderImpl.java"
        val className = "org.apache.flink.runtime.io.network.partition.hybrid.HsSubpartitionFileReaderImpl.BufferIndexOrError"
        assertTrue(
            JavaParsingUtils.isClassStatic(Path(filePath), className),
        )
    }

    @Test
    fun testIsStaticClass3() {
        val filePath =
            "/Users/abhiram/Documents/TBE/evaluation_projects/ruoyi-vue-pro/yudao-module-crm/yudao-module-crm-biz/src/main/java/cn/iocoder/yudao/module/crm/util/CrmQueryWrapperUtils.java"
        val className = "cn.iocoder.yudao.module.crm.util.CrmQueryWrapperUtils"
        assertFalse(
            JavaParsingUtils.isClassStatic(Path(filePath), className),
        )
    }

    @Test
    fun testFindFields() {
        val filePath = "/Users/abhiram/Documents/TBE/RefactoringMiner/src/main/java/org/refactoringminer/util/AstUtils.java"
        assertTrue(
            JavaParsingUtils.findFieldTypes(Path(filePath), "org.refactoringminer.util.AstUtils").isEmpty(),
        )
    }

    @Test
    fun testFindFields2() {
        val filePath = "/Users/abhiram/Documents/TBE/jmove/src/src/br/ufmg/dcc/labsoft/java/jmove/approach/CalculateMediaApproach.java"

        val fields = JavaParsingUtils.findFieldTypes(Path(filePath), "br.ufmg.dcc.labsoft.java.jmove.approach.CalculateMediaApproach")
        print(fields)
        assertTrue(
            fields.isNotEmpty(),
        )
    }

    @Test
    fun testFindFields3() {
        val filePath = "/Users/abhiram/Documents/TBE/evaluation_projects/flink/flink-java/src/main/java/org/apache/flink/api/java/operators/PartitionOperator.java"
        val fields = JavaParsingUtils.findFieldTypes(Path(filePath), "org.apache.flink.api.java.operators.PartitionOperator")
        print(fields)
        assertTrue(
            fields.isNotEmpty(),
        )
    }

    @Test
    fun testFindFields4() {
        val filePath = "/Users/abhiram/Documents/TBE/evaluation_projects/elasticsearch/server/src/main/java/org/elasticsearch/cluster/SnapshotsInProgress.java"
        val fields = JavaParsingUtils.findFieldTypes(Path(filePath), "org.elasticsearch.cluster.SnapshotsInProgress.ShardSnapshotStatus")
        print(fields)
        assertTrue(
            fields.isNotEmpty(),
        )
    }

    @Test
    fun testClassExists() {
        val path = "/Users/abhiram/Documents/TBE/evaluation_projects/elasticsearch/test/framework/src/main/java/org/elasticsearch/common/logging/ChunkedLoggingStreamTestUtils.java"
        val qualName = "org.elasticsearch.common.logging.ChunkedLoggingStreamTestUtils"
        assertTrue(
            JavaParsingUtils.doesClassExist(Path(path), qualName),
        )
    }

    @Test
    fun testClassExists2() {
        val path = "/Users/abhiram/Documents/TBE/evaluation_projects/elasticsearch/server/src/main/java/org/elasticsearch/cluster/SnapshotsInProgress.java"
        val qualName = "org.elasticsearch.cluster.SnapshotsInProgress.ShardSnapshotStatus"
        assertTrue(
            JavaParsingUtils.doesClassExist(Path(path), qualName),
        )
    }

    @Test
    fun testIsExtractable() {
        val path = "/Users/abhiram/Documents/TBE/evaluation_projects/elasticsearch/server/src/main/java/org/elasticsearch/cluster/SnapshotsInProgress.java"
        val variableTypes = JavaParsingUtils.findTypesInRange(Path(path), 133, 139)
        assertTrue(variableTypes.isNotEmpty())
    }

    @Test
    fun testIsExtractable2() {
        val path = "/Users/abhiram/Documents/TBE/evaluation_projects/elasticsearch/server/src/main/java/org/elasticsearch/cluster/SnapshotsInProgress.java"
        val variableTypes = JavaParsingUtils.findTypesInRange(Path(path), 215, 248)
        print(variableTypes)
        assertTrue(variableTypes.isNotEmpty())
    }
}
