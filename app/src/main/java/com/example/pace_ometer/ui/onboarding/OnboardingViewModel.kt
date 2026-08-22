package com.example.pace_ometer.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pace_ometer.data.repository.EquipmentRepository
import com.example.pace_ometer.data.settings.Gender
import com.example.pace_ometer.data.settings.SettingsRepository
import com.example.pace_ometer.data.settings.UnitSystem
import kotlinx.coroutines.launch

data class PendingEquipment(val name: String, val type: String, val startingDistanceMeters: Double)

class OnboardingViewModel(
    private val settingsRepository: SettingsRepository,
    private val equipmentRepository: EquipmentRepository
) : ViewModel() {

    fun completeOnboarding(
        birthDateEpochDay: Long,
        gender: Gender,
        unitSystem: UnitSystem,
        bodyWeightKg: Float,
        heightCm: Float?,
        equipmentToAdd: List<PendingEquipment>,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            settingsRepository.completeOnboarding(birthDateEpochDay, gender, unitSystem, bodyWeightKg, heightCm)
            equipmentToAdd.forEach { pending ->
                equipmentRepository.addEquipment(pending.name, pending.type, pending.startingDistanceMeters)
            }
            onDone()
        }
    }
}
