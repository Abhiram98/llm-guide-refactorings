package com.intellij.ml.llm.template.utils

import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import kotlin.io.path.Path

class JavaParsingUtilsTest{

    @Test
    fun testIsStatic(){
        val filePath = "/Users/abhiram/Documents/TBE/RefactoringMiner/src/main/java/org/refactoringminer/util/AstUtils.java"
        val outPath = "/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/refminer_data/isStaticOut.txt"
        val signature = "public getKeyFromMethodBinding(binding IMethodBinding) : String"
        assertTrue(
            JavaParsingUtils.isMethodStatic(Path(filePath), signature)
        )
    }

    @Test
    fun testIsStatic2(){
        val filePath = "/Users/abhiram/Documents/TBE/evaluation_projects/flink/flink-runtime/src/main/java/org/apache/flink/runtime/clusterframework/BootstrapTools.java"
        val signature = "public getTaskManagerShellCommand(flinkConfig Configuration, tmParams ContaineredTaskManagerParameters, configDirectory String, logDirectory String, hasLogback boolean, hasLog4j boolean, hasKrb5 boolean, mainClass Class<?>, mainArgs String) : String"
        assertTrue(
            JavaParsingUtils.isMethodStatic(Path(filePath), signature)
        )

    }

    @Test
    fun testIsStatic3(){
        val filePath = "/Users/abhiram/Documents/TBE/evaluation_projects/flink/flink-runtime/src/main/java/org/apache/flink/runtime/clusterframework/BootstrapTools.java"
        val signature = "public getStartCommand(template String, startCommandValues Map<String,String>) : String"
        assertTrue(
            JavaParsingUtils.isMethodStatic(Path(filePath), signature)
        )
    }

    @Test
    fun testIsStatic4(){
        val filePath = "/Users/abhiram/Documents/TBE/evaluation_projects/elasticsearch/server/src/main/java/org/elasticsearch/search/aggregations/bucket/BestBucketsDeferringCollector.java"
        val signature = "package Entry(aggCtx AggregationExecutionContext, docDeltas PackedLongValues, buckets PackedLongValues) : record"
        assertFalse(
            JavaParsingUtils.isMethodStatic(Path(filePath), signature)
        )
    }

    @Test
    fun testIsStatic5(){
        val filePath = "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/streams/src/main/java/org/apache/kafka/streams/state/internals/RocksDBStore.java"
        val signature = "public flush(accessor DBAccessor) : void"
        assertFalse(
            JavaParsingUtils.isMethodStatic(Path(filePath), signature)
        )
    }

    @Test
    fun testIsStaticClass(){
        val filePath = "/Users/abhiram/Documents/TBE/evaluation_projects/flink/flink-runtime/src/main/java/org/apache/flink/runtime/clusterframework/BootstrapTools.java"
        val className = "org.apache.flink.runtime.clusterframework.BootstrapTools"
        assertFalse(
            JavaParsingUtils.isClassStatic(Path(filePath), className)
        )

    }


    @Test
    fun testIsStaticClass2(){
        val filePath = "/Users/abhiram/Documents/TBE/evaluation_projects/flink/flink-runtime/src/main/java/org/apache/flink/runtime/io/network/partition/hybrid/HsSubpartitionFileReaderImpl.java"
        val className = "org.apache.flink.runtime.io.network.partition.hybrid.HsSubpartitionFileReaderImpl.BufferIndexOrError"
        assertTrue(
            JavaParsingUtils.isClassStatic(Path(filePath), className)
        )

    }

    @Test
    fun testIsStaticClass3() {
        val filePath =
            "/Users/abhiram/Documents/TBE/evaluation_projects/ruoyi-vue-pro/yudao-module-crm/yudao-module-crm-biz/src/main/java/cn/iocoder/yudao/module/crm/util/CrmQueryWrapperUtils.java"
        val className = "cn.iocoder.yudao.module.crm.util.CrmQueryWrapperUtils"
        assertFalse(
            JavaParsingUtils.isClassStatic(Path(filePath), className)
        )
    }




        @Test
    fun testFindFields(){
        val filePath = "/Users/abhiram/Documents/TBE/RefactoringMiner/src/main/java/org/refactoringminer/util/AstUtils.java"
        assertTrue(
            JavaParsingUtils.findFieldTypes(Path(filePath), "org.refactoringminer.util.AstUtils").isEmpty()
        )
    }

    @Test
    fun testFindFields2(){
        val filePath = "/Users/abhiram/Documents/TBE/jmove/src/src/br/ufmg/dcc/labsoft/java/jmove/approach/CalculateMediaApproach.java"

        val fields = JavaParsingUtils.findFieldTypes(Path(filePath), "br.ufmg.dcc.labsoft.java.jmove.approach.CalculateMediaApproach")
        print(fields)
        assertTrue(
            fields.isNotEmpty()
        )
    }

    @Test
    fun testFindFields3(){
        val filePath = "/Users/abhiram/Documents/TBE/evaluation_projects/flink/flink-java/src/main/java/org/apache/flink/api/java/operators/PartitionOperator.java"
        val fields = JavaParsingUtils.findFieldTypes(Path(filePath), "org.apache.flink.api.java.operators.PartitionOperator")
        print(fields)
        assertTrue(
            fields.isNotEmpty()
        )
    }

    @Test
    fun testFindFields4(){
        val filePath = "/Users/abhiram/Documents/TBE/evaluation_projects/elasticsearch/server/src/main/java/org/elasticsearch/cluster/SnapshotsInProgress.java"
        val fields = JavaParsingUtils.findFieldTypes(Path(filePath), "org.elasticsearch.cluster.SnapshotsInProgress.ShardSnapshotStatus")
        print(fields)
        assertTrue(
            fields.isNotEmpty()
        )
    }

    @Test
    fun `test find fields with qualified names`(){
        val filePath = "/Users/abhiram/Documents/TBE/jmove/src/src/br/ufmg/dcc/labsoft/java/jmove/approach/CalculateMediaApproach.java"

        val fields = JavaParsingUtils.findQualifiedTypesInClass(
            Path(filePath),
            "br.ufmg.dcc.labsoft.java.jmove.approach.CalculateMediaApproach",
            "/Users/abhiram/Documents/TBE/jmove/src/src/"
        )
        print(fields)
        assertTrue(
            fields.isNotEmpty()
        )
    }

    @Test
    fun `test find fields with qualified names kafka`(){
        val filePath = "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/raft/src/test/java/org/apache/kafka/raft/RaftClientTestContext.java"

        val fields = JavaParsingUtils.findQualifiedTypesInClass(
            Path(filePath),
            "org.apache.kafka.raft.RaftClientTestContext",
            "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/clients/src/main/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/clients/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/tools/src/main/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/tools/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/tools/tools-api/src/main/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/tools/tools-api/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/core/src/main/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/core/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/group-coordinator/src/main/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/group-coordinator/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/group-coordinator/group-coordinator-api/src/main/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/server-common/src/main/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/server-common/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/trogdor/src/main/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/trogdor/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/shell/src/main/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/shell/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/streams/src/main/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/streams/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/streams/upgrade-system-tests-31/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/streams/upgrade-system-tests-36/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/streams/upgrade-system-tests-37/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/streams/test-utils/src/main/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/streams/test-utils/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/streams/upgrade-system-tests-30/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/streams/upgrade-system-tests-0110/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/streams/upgrade-system-tests-0102/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/streams/upgrade-system-tests-23/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/streams/upgrade-system-tests-24/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/streams/upgrade-system-tests-25/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/streams/upgrade-system-tests-22/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/streams/quickstart/java/src", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/streams/quickstart/java/target/classes/archetype-resources/src/main/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/streams/quickstart/java/src/main/resources/archetype-resources/src/main/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/streams/upgrade-system-tests-35/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/streams/upgrade-system-tests-32/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/streams/upgrade-system-tests-33/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/streams/upgrade-system-tests-34/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/streams/examples/src/main/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/streams/examples/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/streams/upgrade-system-tests-0100/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/streams/upgrade-system-tests-0101/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/streams/upgrade-system-tests-27/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/streams/upgrade-system-tests-20/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/streams/upgrade-system-tests-11/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/streams/upgrade-system-tests-10/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/streams/upgrade-system-tests-28/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/streams/upgrade-system-tests-21/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/streams/upgrade-system-tests-26/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/server/src/main/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/server/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/storage/src/main/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/storage/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/storage/api/src/main/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/storage/api/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/generator/src/main/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/generator/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/jmh-benchmarks/src/main/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/examples/src/main/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/log4j-appender/src/main/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/log4j-appender/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/raft/src/main/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/raft/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/transaction-coordinator/src/main/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/connect/test-plugins/src/main/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/connect/file/src/main/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/connect/file/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/connect/runtime/src/main/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/connect/runtime/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/connect/basic-auth-extension/src/main/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/connect/basic-auth-extension/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/connect/json/src/main/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/connect/json/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/connect/transforms/src/main/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/connect/transforms/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/connect/api/src/main/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/connect/api/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/connect/mirror-client/src/main/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/connect/mirror-client/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/connect/mirror/src/main/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/connect/mirror/src/test/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/metadata/src/main/java", "/Users/abhiram/Documents/TBE/evaluation_projects/kafka/metadata/src/test/java"
        )
        print(fields)
        assertTrue(
            fields.isNotEmpty()
        )
    }

    @Test
    fun testClassExists(){
        val path = "/Users/abhiram/Documents/TBE/evaluation_projects/elasticsearch/test/framework/src/main/java/org/elasticsearch/common/logging/ChunkedLoggingStreamTestUtils.java"
        val qualName= "org.elasticsearch.common.logging.ChunkedLoggingStreamTestUtils"
        assertTrue(
            JavaParsingUtils.doesClassExist(Path(path), qualName)
        )
    }

    @Test
    fun testClassExists2(){
        val path = "/Users/abhiram/Documents/TBE/evaluation_projects/elasticsearch/server/src/main/java/org/elasticsearch/cluster/SnapshotsInProgress.java"
        val qualName= "org.elasticsearch.cluster.SnapshotsInProgress.ShardSnapshotStatus"
        assertTrue(
            JavaParsingUtils.doesClassExist(Path(path), qualName)
        )
    }

    @Test
    fun testIsExtractable(){
        val path = "/Users/abhiram/Documents/TBE/evaluation_projects/elasticsearch/server/src/main/java/org/elasticsearch/cluster/SnapshotsInProgress.java"
        val variableTypes = JavaParsingUtils.findTypesInRange(Path(path), 133, 139)
        assertTrue(variableTypes.isNotEmpty())
    }

    @Test
    fun testIsExtractable2(){
        val path = "/Users/abhiram/Documents/TBE/evaluation_projects/elasticsearch/server/src/main/java/org/elasticsearch/cluster/SnapshotsInProgress.java"
        val variableTypes = JavaParsingUtils.findTypesInRange(Path(path), 215, 248)
        print(variableTypes)
        assertTrue(variableTypes.isNotEmpty())
    }

    @Test
    fun `test find all methods in class`(){
        val filePath = "/Users/abhiram/Documents/TBE/jmove/src/src/br/ufmg/dcc/labsoft/java/jmove/approach/CalculateMediaApproach.java"

        val fields = JavaParsingUtils.getMethodInformation(
            Path(filePath),
            "br.ufmg.dcc.labsoft.java.jmove.approach.CalculateMediaApproach",
            "/Users/abhiram/Documents/TBE/jmove/src/src/"
        )
        print(fields)
        assertTrue(
            fields.isNotEmpty()
        )
    }
}