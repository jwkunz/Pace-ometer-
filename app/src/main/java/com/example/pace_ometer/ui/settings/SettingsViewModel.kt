package com.example.pace_ometer.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pace_ometer.PaceometerApp
import com.example.pace_ometer.data.settings.SettingsRepository
import com.example.pace_ometer.data.settings.UnitSystem
import com.example.pace_ometer.data.settings.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository: SettingsRepository = (application as PaceometerApp).settingsRepository

    val settings: StateFlow<UserSettings> = settingsRepository.userSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    fun updateUnitSystem(unitSystem: UnitSystem) {
        viewModelScope.launch { settingsRepository.updateUnitSystem(unitSystem) }
    }

    fun updateBodyWeightKg(kg: Float) {
        viewModelScope.launch { settingsRepository.updateBodyWeightKg(kg) }
    }

    fun updateHeightCm(cm: Float?) {
        viewModelScope.launch { settingsRepository.updateHeightCm(cm) }
    }
}
