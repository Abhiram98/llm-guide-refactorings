package org.boulderse.ijserver.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.io.IOException
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.UUID

class DockerManager {
    var dockerCommand: List<String>? = null
    private val objectMapper: ObjectMapper by lazy { ObjectMapper().registerKotlinModule() }

    fun testDockerPath(): Boolean {
        if (dockerCommand == null) findDockerPath()

        val dockerCmd =
            dockerCommand ?: return false.also {
                println("Docker path not set; run findDockerPath() first.")
            }

        return try {
            val processBuilder = ProcessBuilder(dockerCmd + listOf("--help"))
            val process = processBuilder.start()
            val finished = process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)
            val exitCode = if (finished) process.exitValue() else -1
            if (exitCode == 0) {
                println("Docker is accessible and responding to --help.")
                true
            } else {
//                val err = process.errorStream.bufferedReader().readText()
                println("Docker --help failed with exit $exitCode")
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
                dockerCommand = if (isWindows) listOf("cmd", "/c", output) else listOf(output)
                println("Found docker via system command: $dockerCommand")
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
                dockerCommand = if (isWindows) listOf("cmd", "/c", path) else listOf(path)
                println("Found docker at known location: $dockerCommand")
                return
            }
        }

        println("Docker executable not found in PATH or known locations.")
    }

    fun checkDockerDaemon(): Boolean {
        val dockerCmd =
            dockerCommand ?: return false.also {
                println("Docker path not set; run findDockerPath() first.")
            }

        return try {
            val processBuilder = ProcessBuilder(dockerCmd + listOf("info"))

            val process = processBuilder.start()
            val finished = process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)
            val exitCode = if (finished) process.exitValue() else -1

//            val stdout = process.inputStream.bufferedReader().readText()
//            val stderr = process.errorStream.bufferedReader().readText()

            if (exitCode == 0) {
                println("Docker daemon is running and reachable.")
                true
            } else {
                println("Docker daemon check failed (exit=$exitCode).")
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
            val home = resolveHomeDirectory() ?: return emptyMap()
            val dockerDir = home.resolve(".docker")
            if (!Files.exists(dockerDir)) {
                println("Docker config directory not found; skipping credential helper workaround.")
                return emptyMap()
            }

            val originalConfig = dockerDir.resolve("config.json")
            if (!Files.exists(originalConfig)) {
                println("Docker config.json not found; skipping credential helper workaround.")
                return emptyMap()
            }

            // Create temp directory and copy entire .docker directory structure
            val tempDir = Files.createTempDirectory("rename-agent-docker-${UUID.randomUUID()}")

            // Copy the entire .docker directory to preserve contexts and other metadata
            copyDockerDirectory(dockerDir, tempDir)

            // Now sanitize just the config.json in the temp location
            val tempConfigPath = tempDir.resolve("config.json")
            val sanitizedConfig = sanitizeDockerConfig(originalConfig)
            Files.writeString(tempConfigPath, sanitizedConfig)

            val configPath = tempDir.toAbsolutePath().toString()
            println("Applying docker credential helper workaround using config at $configPath")
            mapOf("DOCKER_CONFIG" to configPath)
        } catch (e: Exception) {
            println("Failed to create docker config override: ${e.message}")
            e.printStackTrace()
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
        val home = resolveHomeDirectory() ?: return false
        val configPath = home.resolve(".docker").resolve("config.json")
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

    private fun sanitizeDockerConfig(configPath: Path): String {
        return try {
            val content = Files.readString(configPath)
            val rootNode = objectMapper.readTree(content)
            if (rootNode is ObjectNode) {
                // Only remove credential helpers - preserve everything else including context info
                rootNode.remove(listOf("credsStore", "credHelpers"))
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode)
        } catch (e: Exception) {
            println("Failed to sanitize docker config: ${e.message}")
            e.printStackTrace()
            "{}"
        }
    }

    private fun copyDockerDirectory(sourceDir: Path, targetDir: Path) {
        try {
            Files.walk(sourceDir).use { stream ->
                stream.forEach { source ->
                    try {
                        val destination = targetDir.resolve(sourceDir.relativize(source))

                        if (Files.isDirectory(source)) {
                            if (!Files.exists(destination)) {
                                Files.createDirectories(destination)
                            }
                        } else {
                            // Skip the bin directory files - they're executables we don't need
                            val relativePath = sourceDir.relativize(source).toString()
                            if (relativePath.startsWith("bin${File.separator}") || relativePath == "bin") {
                                return@forEach
                            }

                            // Handle symlinks by copying the target
                            if (Files.isSymbolicLink(source)) {
                                try {
                                    val target = Files.readSymbolicLink(source)
                                    // Only copy if target exists and is readable
                                    if (Files.exists(target) && Files.isReadable(target) && Files.isRegularFile(target)) {
                                        Files.copy(target, destination, StandardCopyOption.REPLACE_EXISTING)
                                    }
                                } catch (e: Exception) {
                                    println("Skipping symlink $source: ${e.message}")
                                }
                            } else if (Files.isRegularFile(source) && Files.isReadable(source)) {
                                Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING)
                            }
                        }
                    } catch (e: Exception) {
                        println("Skipping ${source.fileName}: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            println("Warning during docker directory copy: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun resolveHomeDirectory(): Path? {
        val candidates =
            listOfNotNull(
                System.getProperty("user.home"),
                System.getenv("HOME"),
                System.getenv("USERPROFILE"),
                run {
                    val drive = System.getenv("HOMEDRIVE")
                    val path = System.getenv("HOMEPATH")
                    if (!drive.isNullOrBlank() && !path.isNullOrBlank()) drive + path else null
                },
            )
                .map { it.trim() }
                .filter { it.isNotEmpty() }

        val home = candidates.firstOrNull() ?: return null
        return try {
            Paths.get(home)
        } catch (_: Exception) {
            null
        }
    }
}