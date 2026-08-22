package com.example.pace_ometer.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.pace_ometer.data.db.dao.PersonalRecordDao
import com.example.pace_ometer.data.db.dao.RunDao
import com.example.pace_ometer.data.db.dao.RunSampleDao
import com.example.pace_ometer.data.db.dao.SeasonDao
import com.example.pace_ometer.data.db.entity.PersonalRecordEntity
import com.example.pace_ometer.data.db.entity.RunEntity
import com.example.pace_ometer.data.db.entity.RunSampleEntity
import com.example.pace_ometer.data.db.entity.SeasonEntity

@Database(
    entities = [RunEntity::class, RunSampleEntity::class, SeasonEntity::class, PersonalRecordEntity::class],
    version = 2,
    exportSchema = true
)
abstract class PaceometerDatabase : RoomDatabase() {
    abstract fun runDao(): RunDao
    abstract fun runSampleDao(): RunSampleDao
    abstract fun seasonDao(): SeasonDao
    abstract fun personalRecordDao(): PersonalRecordDao

    companion object {
        const val DATABASE_NAME = "paceometer.db"
    }
}
