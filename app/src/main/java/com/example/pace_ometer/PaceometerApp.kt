package com.example.pace_ometer

import android.app.Application
import android.preference.PreferenceManager
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.example.pace_ometer.data.db.PaceometerDatabase
import com.example.pace_ometer.data.repository.EquipmentRepository
import com.example.pace_ometer.data.repository.PersonalRecordRepository
import com.example.pace_ometer.data.repository.RunRepository
import com.example.pace_ometer.data.repository.SeasonRepository
import com.example.pace_ometer.data.settings.SettingsRepository
import org.osmdroid.config.Configuration

private val Application.dataStore by preferencesDataStore(name = "paceometer_settings")

class PaceometerApp : Application() {

    val database: PaceometerDatabase by lazy {
        Room.databaseBuilder(this, PaceometerDatabase::class.java, PaceometerDatabase.DATABASE_NAME)
            .addMigrations(PaceometerDatabase.MIGRATION_2_3, PaceometerDatabase.MIGRATION_3_4)
            // Pre-release app; only a real migration path is kept for versions already
            // installed on the test device, anything older falls back to a fresh start.
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

    val equipmentRepository: EquipmentRepository by lazy {
        EquipmentRepository(database.equipmentDao())
    }

    override fun onCreate() {
        super.onCreate()
        // osmdroid must be initialized via load() (which wires up its internal Context
        // reference for path resolution) before any per-field overrides are set, or its
        // tile providers silently fail to construct.
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        // A descriptive, contactable User-Agent -- required by essentially every free tile
        // provider's usage policy, and generic/default values (including the literal
        // applicationId) get silently 403-blocked.
        Configuration.getInstance().userAgentValue =
            "Pace-ometer/1.3 (https://github.com/jwkunz/Pace-ometer) osmdroid"
        Configuration.getInstance().osmdroidTileCache = java.io.File(cacheDir, "osmdroid")
    }
}
