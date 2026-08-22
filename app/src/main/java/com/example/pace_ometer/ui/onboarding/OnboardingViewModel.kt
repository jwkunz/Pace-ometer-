package com.example.pace_ometer.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pace_ometer.data.settings.Gender
import com.example.pace_ometer.data.settings.SettingsRepository
import com.example.pace_ometer.data.settings.UnitSystem
import kotlinx.coroutines.launch

class OnboardingViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    fun completeOnboarding(
        birthDateEpochDay: Long,
        gender: Gender,
        unitSystem: UnitSystem,
        bodyWeightKg: Float,
        heightCm: Float?,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            settingsRepository.completeOnboarding(birthDateEpochDay, gender, unitSystem, bodyWeightKg, heightCm)
            onDone()
        }
    }
}
