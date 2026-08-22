package com.example.pace_ometer.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.pace_ometer.data.db.dao.RunDao
import com.example.pace_ometer.data.db.dao.RunSampleDao
import com.example.pace_ometer.data.db.entity.RunEntity
import com.example.pace_ometer.data.db.entity.RunSampleEntity

@Database(
    entities = [RunEntity::class, RunSampleEntity::class],
    version = 1,
    exportSchema = true
)
abstract class PaceometerDatabase : RoomDatabase() {
    abstract fun runDao(): RunDao
    abstract fun runSampleDao(): RunSampleDao

    companion object {
        const val DATABASE_NAME = "paceometer.db"
    }
}
