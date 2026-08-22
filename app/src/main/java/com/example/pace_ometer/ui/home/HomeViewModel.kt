package com.example.pace_ometer.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.pace_ometer.service.RunPhase
import com.example.pace_ometer.service.RunServiceConnection
import com.example.pace_ometer.service.RunState
import kotlinx.coroutines.flow.StateFlow

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val connection = RunServiceConnection(application)

    val runState: StateFlow<RunState> = connection.runState

    init {
        connection.bind()
    }

    override fun onCleared() {
        super.onCleared()
        connection.unbind()
    }
}

val RunState.isActive: Boolean
    get() = phase == RunPhase.RUNNING || phase == RunPhase.PAUSED || phase == RunPhase.STOPPED
