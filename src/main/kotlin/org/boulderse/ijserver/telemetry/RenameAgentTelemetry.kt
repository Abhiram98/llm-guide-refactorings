package org.boulderse.ijserver.telemetry

import com.google.gson.Gson
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.psi.PsiFile
import com.intellij.util.io.createDirectories
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.boulderse.ijserver.refactoringobjects.renamevariable.RenameVariable
import org.boulderse.ijserver.utils.PsiUtils
import kotlin.time.Duration

class RenameAgentTelemetryManager {

    @Serializable
    data class EpochData(
        // One epoch is before scope refinement takes place. Here we're computing how the performance changes,
        // as scope refinement takes into account human feedback.
        @SerialName("accepted_count")
        var acceptedCount: Int = 0,
        @SerialName("rejected_count")
        var rejectedCount: Int = 0,
        @SerialName("identifiers_inspected_count") // total number of identifiers inspected by the tool, before presenting them to the developer.
        var identifiersInspected: Int = 0,
        @SerialName("interesting_identifiers_count") // total number of identifiers inspected by the tool, which match the pattern, before presenting them to the developer.
        var interestingIdentifiersCount: Int = 0,
        @SerialName("accepted_map")
        val acceptedMap: MutableMap<String, Int> = mutableMapOf(),
        @SerialName("rejected_map")
        val rejectedMap: MutableMap<String, Int> = mutableMapOf(),
        @SerialName("accepted_modifiers")
        val acceptedModifiers: MutableList<String> = mutableListOf(),
        @SerialName("rejected_modifiers")
        val rejectedModifiers: MutableList<String> = mutableListOf(),

    )

    @Serializable
    data class TelemetryData(
        @SerialName("accepted_count")
        var acceptedCount: Int = 0,
        @SerialName("rejected_count")
        var rejectedCount: Int = 0,
        @SerialName("identifiers_inspected_count") // total number of identifiers inspected by the tool, before presenting them to the developer.
        var identifiersInspected: Int = 0,
        @SerialName("interesting_identifiers_count") // total number of identifiers inspected by the tool, which match the pattern, before presenting them to the developer.
        var interestingIdentifiersCount: Int = 0,

        @SerialName("scope_change_count")
        var patternChangedCount: Int = 0,
        @SerialName("guard_change_count")
        var guardChangedCount: Int = 0,

        @SerialName("review_series")
        var reviewSeries: MutableList<String> = mutableListOf(),
        @SerialName("accepted_map")
        val acceptedMap: MutableMap<String, Int> = mutableMapOf(),
        @SerialName("rejected_map")
        val rejectedMap: MutableMap<String, Int> = mutableMapOf(),
        @SerialName("accepted_modifiers")
        val acceptedModifiers: MutableList<String> = mutableListOf(),
        @SerialName("rejected_modifiers")
        val rejectedModifiers: MutableList<String> = mutableListOf(),

        @SerialName("epoch_data")
        val epochData: MutableList<EpochData> = mutableListOf(EpochData()),

        @SerialName("total_files")
        var totalFiles: Int = 0,
        @SerialName("inspected_files")
        var inspectedFiles: Int = 0,
        @Transient
        val startTime: java.time.Instant =
            java.time.Instant
                .now(),
        @Transient
        var endTime: java.time.Instant? = null,
        @SerialName("elapsed_time")
        var elapsedTime: Long? = null,
        @SerialName("stopped_early")
        var stoppedEarly: Boolean = false,
        @SerialName("review_time")
        var reviewTime: Long = 0,
        @SerialName("seed_type")
        var seedType: String? = null,
        @SerialName("seed_modifiers")
        var seedModifiers: String? = null,
        @SerialName("plugin_version")
        val pluginVersion: String,
    )

    data class SensitiveData(
        val filesRefactored: MutableMap<PsiFile, MutableList<RenameVariable>> = mutableMapOf(),
    )

    var currentTelemetryData: TelemetryData? = null
    var currentSensitiveData: SensitiveData? = null

    private val logFile =
        PathManager
            .getLogPath()
            .toNioPathOrNull()!!
            .resolve(LOG_DIR_NAME)
            .resolve(LOG_FILE_NAME)
            .toFile()

    init {
        runBlocking {
            withContext(Dispatchers.IO) {
                if (!logFile.exists()) {
                    logFile.parent.toNioPathOrNull()?.createDirectories()
                    logFile.createNewFile()
                }
            }
        }
    }

    fun startNewSession() {
        currentTelemetryData = TelemetryData(pluginVersion = getPluginVersion())
        currentSensitiveData = SensitiveData()
    }

    fun startNewEpoch() {
        currentTelemetryData?.epochData?.add(
            EpochData()
        )
    }

    fun setSeedInfo(
        seedType: String,
        modifier: String? = null,
    ) {
        currentTelemetryData?.seedType = seedType
        if (modifier != null) {
            currentTelemetryData?.seedModifiers = modifier
        }
    }

    fun addAcceptRating(
        elementType: String? = null,
        modifier: String? = null,
    ) {
        currentTelemetryData?.acceptedCount += 1
        currentTelemetryData?.epochData?.last()?.acceptedCount += 1
        currentTelemetryData?.reviewSeries?.add("accepted")
        if (elementType != null) {
            val count = currentTelemetryData?.acceptedMap?.getOrPut(elementType, { 0 })
            currentTelemetryData?.acceptedMap[elementType] = count?.plus(1) ?: 0
            val countEpoch = currentTelemetryData?.epochData?.last()?.acceptedMap?.getOrPut(elementType, { 0 })
            currentTelemetryData?.epochData?.last()?.acceptedMap[elementType] = countEpoch?.plus(1) ?: 0
        }

        if (modifier != null) {
            currentTelemetryData?.acceptedModifiers?.add("$elementType: $modifier")
            currentTelemetryData?.epochData?.last()?.acceptedModifiers?.add("$elementType: $modifier")
        }
    }

    fun addIdentifierInspected(count: Int = 1) {
        currentTelemetryData?.identifiersInspected += count
        currentTelemetryData?.epochData?.last()?.identifiersInspected += count
    }

    fun addInterestingIdentifiers(count: Int = 1) {
        currentTelemetryData?.interestingIdentifiersCount += count
        currentTelemetryData?.epochData?.last()?.interestingIdentifiersCount += count
    }

    fun calculateIdentifierInspected() {
        currentTelemetryData?.identifiersInspected =
            currentSensitiveData
                ?.filesRefactored
                ?.keys
                ?.map { PsiUtils.countIdentifiers(it, null) }
                ?.sum()
                ?: currentTelemetryData?.identifiersInspected ?: 0
    }

    fun addRejectRating(
        elementType: String? = null,
        modifier: String? = null,
    ) {
        currentTelemetryData?.rejectedCount += 1
        currentTelemetryData?.epochData?.last()?.rejectedCount += 1
        currentTelemetryData?.reviewSeries?.add("rejected")

        if (elementType != null) {
            val count = currentTelemetryData?.rejectedMap?.getOrPut(elementType, { 0 })
            currentTelemetryData?.rejectedMap[elementType] = count?.plus(1) ?: 0
            val countEpoch = currentTelemetryData?.epochData?.last()?.rejectedMap?.getOrPut(elementType, { 0 })
            currentTelemetryData?.epochData?.last()?.rejectedMap[elementType] = countEpoch?.plus(1) ?: 0
        }

        if (modifier != null) {
            currentTelemetryData?.rejectedModifiers?.add(modifier)
            currentTelemetryData?.epochData?.last()?.rejectedModifiers?.add(modifier)
        }
    }

    fun patternChanged() {
        currentTelemetryData?.patternChangedCount += 1
    }

    fun guardChanged() {
        currentTelemetryData?.guardChangedCount += 1
    }

    fun endSession() {
        currentTelemetryData?.endTime =
            java.time.Instant
                .now()
        currentTelemetryData?.elapsedTime =
            currentTelemetryData?.endTime?.toEpochMilli()?.minus(currentTelemetryData?.startTime?.toEpochMilli() ?: 0)
        runBlocking {
            withContext(Dispatchers.IO) {
                logFile.appendText("${Gson().toJson(currentTelemetryData)}\n")
            }
        }
    }

    fun addTotalFiles() {
        currentTelemetryData?.totalFiles += 1
    }

    fun addInspectedFile() {
        currentTelemetryData?.inspectedFiles += 1
    }

    fun stoppedEarly() {
        currentTelemetryData?.stoppedEarly = true
    }

    fun addHumanTime(duration: Duration) {
        currentTelemetryData?.reviewTime += duration.inWholeMilliseconds
    }

    fun logRefactoring(
        file: PsiFile,
        pattern: RenameVariable,
    ) {
        val renames = currentSensitiveData?.filesRefactored?.getOrPut(file) { mutableListOf() }
        renames?.add(pattern)
    }

    fun logFileInspected(file: PsiFile) {
        currentSensitiveData?.filesRefactored?.getOrPut(file) { mutableListOf() }
    }

    companion object {
        var telemetryManager: RenameAgentTelemetryManager? = null

        fun getInstance(): RenameAgentTelemetryManager {
            if (telemetryManager == null) {
                telemetryManager = RenameAgentTelemetryManager()
            }
            return telemetryManager!!
        }

        const val LOG_DIR_NAME = "ref_plugin_logs"
        const val LOG_FILE_NAME = "rename_agent_telemetry.jsonl"

        private fun getPluginVersion(): String {
            // Try plugin descriptor first (preferred)
            return try {
                val pluginId =
                    com.intellij.openapi.extensions.PluginId
                        .getId("org.boulderse.ijserver")
                val descriptor =
                    com.intellij.ide.plugins.PluginManagerCore
                        .getPlugin(pluginId)
                descriptor?.version ?: getManifestVersion()
            } catch (_: Throwable) {
                getManifestVersion()
            }
        }

        private fun getManifestVersion(): String = object {}.javaClass.`package`.implementationVersion
    }
}
