package com.example.pace_ometer.ui.activerun

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pace_ometer.PaceometerApp
import com.example.pace_ometer.data.repository.RunRepository
import com.example.pace_ometer.service.RunServiceConnection
import com.example.pace_ometer.service.RunState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ActiveRunViewModel(application: Application) : AndroidViewModel(application) {

    private val connection = RunServiceConnection(application)
    private val runRepository: RunRepository = (application as PaceometerApp).runRepository

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
            runRepository.getRun(runId)?.let { runRepository.updateRun(it.copy(isSaved = true)) }
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
