package com.example.pace_ometer.ui.equipment

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pace_ometer.PaceometerApp
import com.example.pace_ometer.data.db.entity.EquipmentEntity
import com.example.pace_ometer.data.db.entity.RunEntity
import com.example.pace_ometer.data.repository.EquipmentRepository
import com.example.pace_ometer.data.repository.RunRepository
import com.example.pace_ometer.data.settings.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EquipmentDetailViewModel(application: Application, private val equipmentId: Long) :
    AndroidViewModel(application) {

    private val app: PaceometerApp = application as PaceometerApp
    private val equipmentRepository: EquipmentRepository = app.equipmentRepository
    private val runRepository: RunRepository = app.runRepository

    val equipment: StateFlow<EquipmentEntity?> = equipmentRepository.observeById(equipmentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val totalDistanceMeters: StateFlow<Double> = equipmentRepository.observeTotalDistanceMeters(equipmentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val assignedRuns: StateFlow<List<RunEntity>> = equipmentRepository.observeAssignedRuns(equipmentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSavedRuns: StateFlow<List<RunEntity>> = runRepository.observeSavedRuns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<UserSettings> = app.settingsRepository.userSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    fun updateDetails(name: String, type: String, startingDistanceMeters: Double) {
        val current = equipment.value ?: return
        viewModelScope.launch {
            equipmentRepository.updateEquipment(
                current.copy(name = name, type = type, startingDistanceMeters = startingDistanceMeters)
            )
        }
    }

    fun setRetired(retired: Boolean) {
        val current = equipment.value ?: return
        viewModelScope.launch { equipmentRepository.setRetired(current, retired) }
    }

    fun assignRun(runId: Long) {
        viewModelScope.launch { equipmentRepository.assignRun(runId, equipmentId) }
    }

    fun unassignRun(runId: Long) {
        viewModelScope.launch { equipmentRepository.unassignRun(runId, equipmentId) }
    }
}
