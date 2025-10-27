package org.boulderse.ijserver.utils

import org.junit.Test
import kotlin.io.path.Path

class FileUtilsTest {
    @Test
    fun `test overwrite file`() {
        val classLoader = this::class.java.classLoader
        val resource = classLoader.getResource("A1_CSC540.java")
        val fileContent = resource?.readText()

        assert(fileContent != null)

        FileUtils.replaceFileContents(
            Path(resource.path),
            fileContent + "another line",
        )

        val newContent = resource?.readText()!!
        assert(newContent.endsWith("another line"))

        FileUtils.replaceFileContents(
            Path(resource.path),
            fileContent!!,
        )
        val newContent2 = resource?.readText()!!
        assert(!newContent2.endsWith("another line"))
    }

    @Test
    fun `test overwrite in range`() {
        val classLoader = this::class.java.classLoader
        val resource = classLoader.getResource("A1_CSC540.java")
        val fileContent = resource?.readText()

        FileUtils.replaceFileContentsInRange(
            Path(resource.path),
            100,
            200,
            "something else",
        )
        val newContent = resource?.readText()!!
        println(newContent.substring(100, 200))
        assert(newContent.substring(100, 200).contains("something else"))
    }
}
