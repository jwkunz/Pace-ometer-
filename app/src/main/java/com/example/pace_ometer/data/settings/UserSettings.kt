package com.example.pace_ometer.data.settings

enum class UnitSystem { METRIC, IMPERIAL }

enum class Gender { MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY }

data class UserSettings(
    val onboardingCompleted: Boolean = false,
    val birthDateEpochDay: Long? = null,
    val gender: Gender = Gender.PREFER_NOT_TO_SAY,
    val heightCm: Float? = null,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val bodyWeightKg: Float = 70f,
    val heartRateDeviceAddress: String? = null,
    val useHealthConnectHeartRate: Boolean = false,
    val autoPauseEnabled: Boolean = false,
    val announcementIntervalValue: Float = 1f,
    val announcementIntervalUnit: UnitSystem = UnitSystem.METRIC,
    val announceDistance: Boolean = true,
    val announceElapsedTime: Boolean = true,
    val announceElevation: Boolean = false,
    val announceHeartRate: Boolean = false,
    val announceHeartRateZone: Boolean = false,
    val announceCadence: Boolean = false,
    val announceSegmentPace: Boolean = true,
    val announceSplitPace: Boolean = false,
    val announceElevationChangeLastSegment: Boolean = false,
    val announceCalories: Boolean = false,
    val announceClockTime: Boolean = false
)
