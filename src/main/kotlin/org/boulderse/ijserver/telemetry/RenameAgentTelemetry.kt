package org.boulderse.ijserver.telemetry

import com.google.gson.Gson
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.util.io.createDirectories
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration

class RenameAgentTelemetryManager {

    @Serializable
    data class TelemetryData(
        @SerialName("accepted_count")
        var acceptedCount: Int = 0,
        @SerialName("rejected_count")
        var rejectedCount: Int = 0,
        @SerialName("scope_change_count")
        var patternChangedCount: Int = 0,

        @SerialName("guard_change_count")
        var guardChangedCount: Int = 0,

        @SerialName("accepted_map")
        val acceptedMap: MutableMap<String, Int> = mutableMapOf(),
        @SerialName("rejected_map")
        val rejectedMap: MutableMap<String, Int> = mutableMapOf(),

        @SerialName("total_files")
        var totalFiles: Int = 0,

        @SerialName("inspected_files")
        var inspectedFiles: Int = 0,

        @SerialName("start_time")
        val startTime: String = java.time.Instant.now().toString(),

        @SerialName("end_time")
        var endTime: String? = null,

        @SerialName("stopped_early")
        var stoppedEarly: Boolean = false,

        @SerialName("review_time")
        var reviewTime: Long = 0

        )

    var currentTelemetryData: TelemetryData? = null

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

    fun startNewSession(){
        currentTelemetryData = TelemetryData()
    }

    fun addAcceptRating(elementType: String? = null, ){
        currentTelemetryData?.acceptedCount += 1
        if (elementType!=null){
            val count = currentTelemetryData?.acceptedMap?.getOrPut(elementType, { 0 })
            currentTelemetryData?.acceptedMap[elementType] = count?.plus(1) ?: 0
        }
    }

    fun addRejectRating(elementType: String? = null){
        currentTelemetryData?.rejectedCount += 1

        if (elementType!=null){
            val count = currentTelemetryData?.rejectedMap?.getOrPut(elementType, { 0 })
            currentTelemetryData?.rejectedMap[elementType] = count?.plus(1) ?: 0
        }
    }

    fun patternChanged() {
        currentTelemetryData?.patternChangedCount += 1
    }

    fun guardChanged(){
        currentTelemetryData?.guardChangedCount += 1
    }

    fun endSession(){
        currentTelemetryData?.endTime = java.time.Instant.now().toString()
        runBlocking {
            withContext(Dispatchers.IO) {
                logFile.appendText("${Gson().toJson(currentTelemetryData)}\n")
            }
        }
    }

    fun addTotalFiles(){
        currentTelemetryData?.totalFiles += 1
    }

    fun addInspectedFile(){
        currentTelemetryData?.inspectedFiles += 1
    }

    fun stoppedEarly(){
        currentTelemetryData?.stoppedEarly = true
    }

    fun addHumanTime(duration: Duration) {
        currentTelemetryData?.reviewTime += duration.inWholeMilliseconds
    }

    companion object{
        var telemetryManager: RenameAgentTelemetryManager? = null
        fun getInstance(): RenameAgentTelemetryManager {
            if (telemetryManager == null){
                telemetryManager = RenameAgentTelemetryManager()
            }
            return telemetryManager!!
        }

        const val LOG_DIR_NAME = "ref_plugin_logs"
        const val LOG_FILE_NAME = "ref_telemetry_data.jsonl"
    }
}