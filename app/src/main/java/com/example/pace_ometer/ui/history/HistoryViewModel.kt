package com.example.pace_ometer.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pace_ometer.PaceometerApp
import com.example.pace_ometer.data.db.entity.RunEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val runRepository = (application as PaceometerApp).runRepository

    val runs: StateFlow<List<RunEntity>> = runRepository.observeSavedRuns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
