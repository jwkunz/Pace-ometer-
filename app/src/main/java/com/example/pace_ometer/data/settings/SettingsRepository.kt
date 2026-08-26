package com.example.pace_ometer.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private object Keys {
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    val BIRTH_DATE_EPOCH_DAY = longPreferencesKey("birth_date_epoch_day")
    val GENDER = stringPreferencesKey("gender")
    val HEIGHT_CM = floatPreferencesKey("height_cm")
    val UNIT_SYSTEM = stringPreferencesKey("unit_system")
    val BODY_WEIGHT_KG = floatPreferencesKey("body_weight_kg")
    val HEART_RATE_DEVICE_ADDRESS = stringPreferencesKey("heart_rate_device_address")
    val USE_HEALTH_CONNECT_HEART_RATE = booleanPreferencesKey("use_health_connect_heart_rate")
    val AUTO_PAUSE_ENABLED = booleanPreferencesKey("auto_pause_enabled")
    val ANNOUNCEMENT_INTERVAL_VALUE = floatPreferencesKey("announcement_interval_value")
    val ANNOUNCEMENT_INTERVAL_UNIT = stringPreferencesKey("announcement_interval_unit")
    val ANNOUNCE_DISTANCE = booleanPreferencesKey("announce_distance")
    val ANNOUNCE_ELAPSED_TIME = booleanPreferencesKey("announce_elapsed_time")
    val ANNOUNCE_ELEVATION = booleanPreferencesKey("announce_elevation")
    val ANNOUNCE_HEART_RATE = booleanPreferencesKey("announce_heart_rate")
    val ANNOUNCE_HEART_RATE_ZONE = booleanPreferencesKey("announce_heart_rate_zone")
    val ANNOUNCE_CADENCE = booleanPreferencesKey("announce_cadence")
    val ANNOUNCE_SEGMENT_PACE = booleanPreferencesKey("announce_segment_pace")
    val ANNOUNCE_SPLIT_PACE = booleanPreferencesKey("announce_split_pace")
    val ANNOUNCE_ELEVATION_CHANGE_LAST_SEGMENT = booleanPreferencesKey("announce_elevation_change_last_segment")
    val ANNOUNCE_CALORIES = booleanPreferencesKey("announce_calories")
    val ANNOUNCE_CLOCK_TIME = booleanPreferencesKey("announce_clock_time")
}

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val userSettings: Flow<UserSettings> = dataStore.data.map { prefs -> prefs.toUserSettings() }

    suspend fun completeOnboarding(
        birthDateEpochDay: Long,
        gender: Gender,
        unitSystem: UnitSystem,
        bodyWeightKg: Float,
        heightCm: Float?
    ) {
        dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETED] = true
            prefs[Keys.BIRTH_DATE_EPOCH_DAY] = birthDateEpochDay
            prefs[Keys.GENDER] = gender.name
            prefs[Keys.UNIT_SYSTEM] = unitSystem.name
            prefs[Keys.BODY_WEIGHT_KG] = bodyWeightKg
            if (heightCm != null) prefs[Keys.HEIGHT_CM] = heightCm
        }
    }

    /** Wipes every stored preference, restoring first-launch defaults (including onboarding gating). */
    suspend fun resetToDefaults() {
        dataStore.edit { prefs -> prefs.clear() }
    }

    suspend fun updateUnitSystem(unitSystem: UnitSystem) {
        dataStore.edit { prefs -> prefs[Keys.UNIT_SYSTEM] = unitSystem.name }
    }

    suspend fun updateBodyWeightKg(bodyWeightKg: Float) {
        dataStore.edit { prefs -> prefs[Keys.BODY_WEIGHT_KG] = bodyWeightKg }
    }

    suspend fun updateHeartRateDeviceAddress(address: String?) {
        dataStore.edit { prefs ->
            if (address != null) prefs[Keys.HEART_RATE_DEVICE_ADDRESS] = address
            else prefs.remove(Keys.HEART_RATE_DEVICE_ADDRESS)
        }
    }

    suspend fun updateUseHealthConnectHeartRate(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.USE_HEALTH_CONNECT_HEART_RATE] = enabled }
    }

    suspend fun updateAutoPauseEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.AUTO_PAUSE_ENABLED] = enabled }
    }

    suspend fun updateHeightCm(heightCm: Float?) {
        dataStore.edit { prefs ->
            if (heightCm != null) prefs[Keys.HEIGHT_CM] = heightCm else prefs.remove(Keys.HEIGHT_CM)
        }
    }

    suspend fun updateBirthDateEpochDay(epochDay: Long) {
        dataStore.edit { prefs -> prefs[Keys.BIRTH_DATE_EPOCH_DAY] = epochDay }
    }

    suspend fun updateGender(gender: Gender) {
        dataStore.edit { prefs -> prefs[Keys.GENDER] = gender.name }
    }

    suspend fun updateAnnouncementInterval(value: Float, unit: UnitSystem) {
        dataStore.edit { prefs ->
            prefs[Keys.ANNOUNCEMENT_INTERVAL_VALUE] = value
            prefs[Keys.ANNOUNCEMENT_INTERVAL_UNIT] = unit.name
        }
    }

    suspend fun updateAnnouncementToggles(update: (UserSettings) -> UserSettings) {
        dataStore.edit { prefs ->
            val updated = update(prefs.toUserSettings())
            prefs[Keys.ANNOUNCE_DISTANCE] = updated.announceDistance
            prefs[Keys.ANNOUNCE_ELAPSED_TIME] = updated.announceElapsedTime
            prefs[Keys.ANNOUNCE_ELEVATION] = updated.announceElevation
            prefs[Keys.ANNOUNCE_HEART_RATE] = updated.announceHeartRate
            prefs[Keys.ANNOUNCE_HEART_RATE_ZONE] = updated.announceHeartRateZone
            prefs[Keys.ANNOUNCE_CADENCE] = updated.announceCadence
            prefs[Keys.ANNOUNCE_SEGMENT_PACE] = updated.announceSegmentPace
            prefs[Keys.ANNOUNCE_SPLIT_PACE] = updated.announceSplitPace
            prefs[Keys.ANNOUNCE_ELEVATION_CHANGE_LAST_SEGMENT] = updated.announceElevationChangeLastSegment
            prefs[Keys.ANNOUNCE_CALORIES] = updated.announceCalories
            prefs[Keys.ANNOUNCE_CLOCK_TIME] = updated.announceClockTime
        }
    }

    private fun Preferences.toUserSettings(): UserSettings {
        val prefs = this
        return UserSettings(
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            birthDateEpochDay = prefs[Keys.BIRTH_DATE_EPOCH_DAY],
            gender = prefs[Keys.GENDER]?.let { runCatching { Gender.valueOf(it) }.getOrNull() }
                ?: Gender.PREFER_NOT_TO_SAY,
            heightCm = prefs[Keys.HEIGHT_CM],
            unitSystem = prefs[Keys.UNIT_SYSTEM]?.let { runCatching { UnitSystem.valueOf(it) }.getOrNull() }
                ?: UnitSystem.METRIC,
            bodyWeightKg = prefs[Keys.BODY_WEIGHT_KG] ?: 70f,
            heartRateDeviceAddress = prefs[Keys.HEART_RATE_DEVICE_ADDRESS],
            useHealthConnectHeartRate = prefs[Keys.USE_HEALTH_CONNECT_HEART_RATE] ?: false,
            autoPauseEnabled = prefs[Keys.AUTO_PAUSE_ENABLED] ?: false,
            announcementIntervalValue = prefs[Keys.ANNOUNCEMENT_INTERVAL_VALUE] ?: 1f,
            announcementIntervalUnit = prefs[Keys.ANNOUNCEMENT_INTERVAL_UNIT]?.let {
                runCatching { UnitSystem.valueOf(it) }.getOrNull()
            } ?: UnitSystem.METRIC,
            announceDistance = prefs[Keys.ANNOUNCE_DISTANCE] ?: true,
            announceElapsedTime = prefs[Keys.ANNOUNCE_ELAPSED_TIME] ?: true,
            announceElevation = prefs[Keys.ANNOUNCE_ELEVATION] ?: false,
            announceHeartRate = prefs[Keys.ANNOUNCE_HEART_RATE] ?: false,
            announceHeartRateZone = prefs[Keys.ANNOUNCE_HEART_RATE_ZONE] ?: false,
            announceCadence = prefs[Keys.ANNOUNCE_CADENCE] ?: false,
            announceSegmentPace = prefs[Keys.ANNOUNCE_SEGMENT_PACE] ?: true,
            announceSplitPace = prefs[Keys.ANNOUNCE_SPLIT_PACE] ?: false,
            announceElevationChangeLastSegment = prefs[Keys.ANNOUNCE_ELEVATION_CHANGE_LAST_SEGMENT] ?: false,
            announceCalories = prefs[Keys.ANNOUNCE_CALORIES] ?: false,
            announceClockTime = prefs[Keys.ANNOUNCE_CLOCK_TIME] ?: false
        )
    }
}
