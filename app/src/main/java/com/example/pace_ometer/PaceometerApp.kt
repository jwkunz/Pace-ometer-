package com.example.pace_ometer

import android.app.Application
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.example.pace_ometer.data.db.PaceometerDatabase
import com.example.pace_ometer.data.repository.RunRepository
import com.example.pace_ometer.data.settings.SettingsRepository

private val Application.dataStore by preferencesDataStore(name = "paceometer_settings")

class PaceometerApp : Application() {

    val database: PaceometerDatabase by lazy {
        Room.databaseBuilder(this, PaceometerDatabase::class.java, PaceometerDatabase.DATABASE_NAME)
            .build()
    }

    val runRepository: RunRepository by lazy {
        RunRepository(database.runDao(), database.runSampleDao())
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(dataStore)
    }
}
