package com.example.pace_ometer

import android.app.Application
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.example.pace_ometer.data.db.PaceometerDatabase
import com.example.pace_ometer.data.repository.PersonalRecordRepository
import com.example.pace_ometer.data.repository.RunRepository
import com.example.pace_ometer.data.repository.SeasonRepository
import com.example.pace_ometer.data.settings.SettingsRepository
import org.osmdroid.config.Configuration

private val Application.dataStore by preferencesDataStore(name = "paceometer_settings")

class PaceometerApp : Application() {

    val database: PaceometerDatabase by lazy {
        Room.databaseBuilder(this, PaceometerDatabase::class.java, PaceometerDatabase.DATABASE_NAME)
            // Pre-release app, no installed base to migrate yet.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    val runRepository: RunRepository by lazy {
        RunRepository(database.runDao(), database.runSampleDao())
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(dataStore)
    }

    val seasonRepository: SeasonRepository by lazy {
        SeasonRepository(database.seasonDao())
    }

    val personalRecordRepository: PersonalRecordRepository by lazy {
        PersonalRecordRepository(database.personalRecordDao(), database.seasonDao())
    }

    override fun onCreate() {
        super.onCreate()
        // osmdroid requires a distinct user agent and a configured cache dir before first use.
        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().osmdroidTileCache = java.io.File(cacheDir, "osmdroid")
    }
}
