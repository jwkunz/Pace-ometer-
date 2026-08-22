package com.example.pace_ometer.ui.equipment

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pace_ometer.PaceometerApp
import com.example.pace_ometer.data.db.entity.EquipmentEntity
import com.example.pace_ometer.data.repository.EquipmentRepository
import com.example.pace_ometer.data.settings.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EquipmentViewModel(application: Application) : AndroidViewModel(application) {

    private val app: PaceometerApp = application as PaceometerApp
    private val equipmentRepository: EquipmentRepository = app.equipmentRepository

    val equipment: StateFlow<List<EquipmentEntity>> = equipmentRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<UserSettings> = app.settingsRepository.userSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    fun addEquipment(name: String, type: String, startingDistanceMeters: Double) {
        viewModelScope.launch { equipmentRepository.addEquipment(name, type, startingDistanceMeters) }
    }
}
