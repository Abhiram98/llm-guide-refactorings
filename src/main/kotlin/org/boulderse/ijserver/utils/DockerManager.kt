package org.boulderse.ijserver.utils

import kotlinx.io.IOException
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID

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

    fun prepareCredentialHelperWorkaround(): Map<String, String> {
        if (!isCredentialHelperMissing()) return emptyMap()

        return try {
            val tempDir = Files.createTempDirectory("rename-agent-docker-${UUID.randomUUID()}")
            val configFile = tempDir.resolve("config.json")
            Files.writeString(configFile, "{}")
            val configPath = tempDir.toAbsolutePath().toString()
            println("Applying docker credential helper workaround using config at $configPath")
            mapOf("DOCKER_CONFIG" to configPath)
        } catch (e: Exception) {
            println("Failed to create docker config override: ${e.message}")
            emptyMap()
        }
    }

    fun applyEnvironmentOverrides(
        builder: ProcessBuilder,
        overrides: Map<String, String>,
    ) {
        overrides.forEach { (key, value) ->
            builder.environment()[key] = value
        }
    }

    private fun isCredentialHelperMissing(): Boolean {
        val home = System.getenv("HOME") ?: return false
        val configPath = Paths.get(home, ".docker", "config.json")
        if (!Files.exists(configPath)) return false

        return try {
            val config = Files.readString(configPath)
            val helperNames =
                mutableSetOf<String>().apply {
                    Regex("\"credsStore\"\\s*:\\s*\"([^\"]+)\"")
                        .find(config)
                        ?.let { add(it.groupValues[1]) }
                    Regex("\"credHelpers\"\\s*:\\s*\\{([^}]*)}")
                        .find(config)
                        ?.groupValues
                        ?.getOrNull(1)
                        ?.let { helpersBody ->
                            Regex("\"[^\"]+\"\\s*:\\s*\"([^\"]+)\"")
                                .findAll(helpersBody)
                                .forEach { add(it.groupValues[1]) }
                        }
                }

            helperNames.any { helper ->
                val executable = "docker-credential-$helper"
                val helperMissing = !isExecutableOnPath(executable)
                if (helperMissing) {
                    println("Detected docker credential helper '$executable' configured but not present on PATH.")
                }
                helperMissing
            }
        } catch (e: Exception) {
            println("Failed to inspect docker config for credential helpers: ${e.message}")
            false
        }
    }

    private fun isExecutableOnPath(executable: String): Boolean {
        val path = System.getenv("PATH") ?: return false
        val isWindows = System.getProperty("os.name").lowercase().contains("win")

        return path
            .split(File.pathSeparatorChar)
            .filter { it.isNotBlank() }
            .any { dir ->
                val candidate = File(dir, executable)
                val candidateWithExe = if (isWindows) File(dir, "$executable.exe") else null
                (candidate.exists() && candidate.canExecute()) ||
                    (candidateWithExe?.let { it.exists() && it.canExecute() } ?: false)
            }
    }
}
