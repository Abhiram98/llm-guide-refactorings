package com.intellij.ml.llm.template.telemetry

import com.google.gson.Gson
import com.intellij.ml.llm.template.utils.EFNotification
import com.intellij.ml.llm.template.utils.Observer
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.io.toNioPath
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.util.io.createDirectories
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class TelemetryDataObserver : Observer {
    companion object {
        const val LOG_DIR_NAME = "ref_plugin_logs"
        const val LOG_FILE_NAME = "ref_telemetry_data.jsonl"
    }

    private val logFile = PathManager.getLogPath().toNioPathOrNull()!!
        .resolve(LOG_DIR_NAME)
        .resolve(LOG_FILE_NAME).toFile()

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

    private fun logToPluginFile(telemetryData: RefTelemetryData) {
        runBlocking {
            withContext(Dispatchers.IO) {
                logFile.appendText("${Gson().toJson(telemetryData)}\n")
            }
        }
    }

    private fun logToPluginFile(telemetryData: AgenticTelemetry) {
        runBlocking {
            withContext(Dispatchers.IO) {
                logFile.appendText("${Gson().toJson(telemetryData)}\n")
            }
        }
    }

    override fun update(notification: EFNotification) {
        when (notification.payload) {
            is RefTelemetryData -> logToPluginFile(notification.payload)
            is AgenticTelemetry -> logToPluginFile(notification.payload)
        }
    }
}

class TelemetryElapsedTimeObserver: Observer {
    private val elapsedTime: HashMap<Int, Long> = HashMap()
    private val candidateSelectionElapsedTime: HashMap<Int, Long> = HashMap()
    private var currentSelectedIndex = 0
    private val logger = Logger.getInstance(javaClass)
    override fun update(notification: EFNotification) {
        when (notification.payload) {
            is EFTelemetryDataElapsedTimeNotificationPayload -> {
                when (notification.payload.action) {
                    TelemetryDataAction.START -> {
                        currentSelectedIndex = notification.payload.selectionIndex
                        candidateSelectionElapsedTime[currentSelectedIndex] = System.currentTimeMillis()
                        logger.debug("started index: $currentSelectedIndex at: ${candidateSelectionElapsedTime[currentSelectedIndex]}")
                    }
                    TelemetryDataAction.STOP -> {
                        val elapsed = System.currentTimeMillis() - candidateSelectionElapsedTime.getOrDefault(currentSelectedIndex, 0)
                        val cumulated = elapsedTime.getOrDefault(currentSelectedIndex, 0) + elapsed
                        elapsedTime[currentSelectedIndex] = cumulated
                        logger.debug("stopped $currentSelectedIndex, elapsed: $elapsed, cumulated: $cumulated")
                    }
                }
            }
        }
    }

    fun getTelemetryData(): List<CandidateElapsedTimeTelemetryData> {
        return elapsedTime.map {
            CandidateElapsedTimeTelemetryData(it.key, it.value)
        }
    }

    fun buildElapsedTimeTelemetryData(telemetryDataManager: EFTelemetryDataManager) {
        val elapsedTimeTelemetryData = this.getTelemetryData()
        val efTelemetryData = telemetryDataManager.getData()
        if (efTelemetryData != null) {
            efTelemetryData.elapsedTime = elapsedTimeTelemetryData
        }
    }
}