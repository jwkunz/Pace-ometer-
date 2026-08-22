package com.example.pace_ometer.ui.history

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pace_ometer.PaceometerApp
import com.example.pace_ometer.data.db.entity.RunEntity
import com.example.pace_ometer.export.RunJsonExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val runRepository = (application as PaceometerApp).runRepository
    private val exporter = RunJsonExporter(runRepository)

    val runs: StateFlow<List<RunEntity>> = runRepository.observeSavedRuns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun exportAllRunsTo(uri: Uri) {
        val context = getApplication<PaceometerApp>()
        viewModelScope.launch {
            val json = exporter.exportSavedRuns()
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            }
        }
    }
}
