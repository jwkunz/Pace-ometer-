package com.example.pace_ometer.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pace_ometer.PaceometerApp
import com.example.pace_ometer.data.db.entity.SeasonEntity
import com.example.pace_ometer.data.repository.SeasonRepository
import com.example.pace_ometer.data.settings.SettingsRepository
import com.example.pace_ometer.data.settings.UnitSystem
import com.example.pace_ometer.data.settings.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app: PaceometerApp = application as PaceometerApp
    private val settingsRepository: SettingsRepository = app.settingsRepository
    private val seasonRepository: SeasonRepository = app.seasonRepository

    val settings: StateFlow<UserSettings> = settingsRepository.userSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    val seasons: StateFlow<List<SeasonEntity>> = seasonRepository.observeSeasons()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addSeason(name: String, startEpochMs: Long) {
        viewModelScope.launch { seasonRepository.addSeason(name, startEpochMs) }
    }

    fun deleteSeason(season: SeasonEntity) {
        viewModelScope.launch { seasonRepository.deleteSeason(season) }
    }

    fun updateUnitSystem(unitSystem: UnitSystem) {
        viewModelScope.launch { settingsRepository.updateUnitSystem(unitSystem) }
    }

    fun updateBodyWeightKg(kg: Float) {
        viewModelScope.launch { settingsRepository.updateBodyWeightKg(kg) }
    }

    fun updateHeightCm(cm: Float?) {
        viewModelScope.launch { settingsRepository.updateHeightCm(cm) }
    }

    fun updateHeartRateDeviceAddress(address: String?) {
        viewModelScope.launch { settingsRepository.updateHeartRateDeviceAddress(address) }
    }

    fun updateUseHealthConnectHeartRate(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateUseHealthConnectHeartRate(enabled) }
    }

    fun updateAutoPauseEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateAutoPauseEnabled(enabled) }
    }

    fun updateAutoPauseIdleThresholdSeconds(seconds: Int) {
        viewModelScope.launch { settingsRepository.updateAutoPauseIdleThresholdSeconds(seconds) }
    }

    fun updateAnnouncementInterval(value: Float, unit: UnitSystem) {
        viewModelScope.launch { settingsRepository.updateAnnouncementInterval(value, unit) }
    }

    fun updateTtsSpeechRate(rate: Float) {
        viewModelScope.launch { settingsRepository.updateTtsSpeechRate(rate) }
    }

    fun updateAnnouncementToggle(update: (UserSettings) -> UserSettings) {
        viewModelScope.launch { settingsRepository.updateAnnouncementToggles(update) }
    }

    /** Wipes all runs/samples/seasons/records and every stored setting, back to a first-launch state. */
    fun resetAllData(onDone: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { app.database.clearAllTables() }
            settingsRepository.resetToDefaults()
            onDone()
        }
    }
}
