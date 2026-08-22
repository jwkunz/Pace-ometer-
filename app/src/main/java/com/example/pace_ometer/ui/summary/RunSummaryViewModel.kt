package com.example.pace_ometer.ui.summary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pace_ometer.PaceometerApp
import com.example.pace_ometer.data.db.entity.RunEntity
import com.example.pace_ometer.data.db.entity.RunSampleEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class RunSummaryViewModel(application: Application, runId: Long) : AndroidViewModel(application) {

    private val runRepository = (application as PaceometerApp).runRepository

    val run: StateFlow<RunEntity?> = runRepository.observeRun(runId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val samples: StateFlow<List<RunSampleEntity>> = runRepository.observeSamples(runId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
