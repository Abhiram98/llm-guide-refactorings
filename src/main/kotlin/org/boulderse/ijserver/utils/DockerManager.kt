package org.boulderse.ijserver.utils

import kotlinx.io.IOException
import java.io.File

class DockerManager {
    var dockerPath: String? = null

    fun testDockerPath(): Boolean {
        if (dockerPath == null) findDockerPath()

        val dockerCmd =
            dockerPath ?: return false.also {
                println("Docker path not set; run findDockerPath() first.")
            }

        return try {
            val processBuilder = ProcessBuilder(dockerCmd, "--help")
            val process = processBuilder.start()
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                println("Docker is accessible and responding to --help.")
                true
            } else {
                val err = process.errorStream.bufferedReader().readText()
                println("Docker --help failed with exit $exitCode: $err")
                false
            }
        } catch (e: Exception) {
            println("Failed to execute docker: ${e.message}")
            false
        }
    }

    fun findDockerPath() {
        val os = System.getProperty("os.name").lowercase()
        val isWindows = os.contains("win")

        val searchCmd = if (isWindows) listOf("where", "docker") else listOf("which", "docker")
        val extraPaths = mutableListOf("/usr/local/bin", "/opt/homebrew/bin", "/usr/bin")

        // Build process with expanded PATH
        val processBuilder = ProcessBuilder(searchCmd)
        val env = processBuilder.environment()
        env["PATH"] = (env["PATH"] ?: "") + File.pathSeparator + extraPaths.joinToString(File.pathSeparator)

        try {
            val process = processBuilder.start()
            val exitCode = process.waitFor()
            val output =
                process.inputStream
                    .bufferedReader()
                    .readText()
                    .trim()

            if (exitCode == 0 && output.isNotBlank()) {
                dockerPath = output
                println("Found docker via system command: $dockerPath")
                return
            } else {
                println("'which/where docker' failed (exit=$exitCode): $output")
            }
        } catch (e: Exception) {
            println("Failed to run system command to locate docker: ${e.message}")
        }

        val possiblePaths =
            if (isWindows) {
                listOf(
                    "C:\\Program Files\\Docker\\Docker\\resources\\bin\\docker.exe",
                    "C:\\ProgramData\\DockerDesktop\\version-bin\\docker.exe",
                )
            } else {
                listOf("/usr/local/bin/docker", "/opt/homebrew/bin/docker", "/usr/bin/docker")
            }

        for (path in possiblePaths) {
            if (File(path).exists()) {
                dockerPath = path
                println("Found docker at known location: $dockerPath")
                return
            }
        }

        println("Docker executable not found in PATH or known locations.")
    }

    fun checkDockerDaemon(): Boolean {
        val dockerCmd =
            dockerPath ?: return false.also {
                println("Docker path not set; run findDockerPath() first.")
            }

        return try {
            val processBuilder = ProcessBuilder(dockerCmd, "info")

            val process = processBuilder.start()
            val exitCode = process.waitFor()

            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()

            if (exitCode == 0 && stdout.contains("Server Version")) {
                println("Docker daemon is running and reachable.")
                true
            } else {
                println("Docker daemon check failed (exit=$exitCode). Output:\n$stdout\nErrors:\n$stderr")
                false
            }
        } catch (e: IOException) {
            println("Failed to execute docker info: ${e.message}")
            false
        } catch (e: InterruptedException) {
            println("Interrupted while checking docker daemon: ${e.message}")
            false
        }
    }

    companion object {
        var singleton: DockerManager? = null

        fun getInstance(): DockerManager {
            if (singleton == null) singleton = DockerManager()
            return singleton!!
        }
    }
}
