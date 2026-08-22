package com.example.pace_ometer.ui.activerun

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pace_ometer.PaceometerApp
import com.example.pace_ometer.data.repository.PersonalRecordRepository
import com.example.pace_ometer.data.repository.RunRepository
import com.example.pace_ometer.service.RunServiceConnection
import com.example.pace_ometer.service.RunState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ActiveRunViewModel(application: Application) : AndroidViewModel(application) {

    private val connection = RunServiceConnection(application)
    private val app: PaceometerApp = application as PaceometerApp
    private val runRepository: RunRepository = app.runRepository
    private val personalRecordRepository: PersonalRecordRepository = app.personalRecordRepository

    val runState: StateFlow<RunState> = connection.runState

    init {
        connection.bind()
    }

    fun start() = connection.start()
    fun pause() = connection.pause()
    fun resume() = connection.resume()
    fun stop() = connection.stop()

    fun saveRun(runId: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            runRepository.getRun(runId)?.let { run ->
                val savedRun = run.copy(isSaved = true)
                runRepository.updateRun(savedRun)
                val samples = runRepository.getSamples(runId)
                personalRecordRepository.evaluateAndUpsert(savedRun, samples)
            }
            connection.resetToIdle()
            onDone()
        }
    }

    fun discardRun(runId: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            runRepository.discardRun(runId)
            connection.resetToIdle()
            onDone()
        }
    }

    override fun onCleared() {
        super.onCleared()
        connection.unbind()
    }
}
