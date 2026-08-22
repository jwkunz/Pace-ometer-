package com.example.pace_ometer.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.pace_ometer.data.db.dao.EquipmentDao
import com.example.pace_ometer.data.db.dao.PersonalRecordDao
import com.example.pace_ometer.data.db.dao.RunDao
import com.example.pace_ometer.data.db.dao.RunSampleDao
import com.example.pace_ometer.data.db.dao.SeasonDao
import com.example.pace_ometer.data.db.entity.EquipmentEntity
import com.example.pace_ometer.data.db.entity.PersonalRecordEntity
import com.example.pace_ometer.data.db.entity.RunEntity
import com.example.pace_ometer.data.db.entity.RunEquipmentCrossRef
import com.example.pace_ometer.data.db.entity.RunSampleEntity
import com.example.pace_ometer.data.db.entity.SeasonEntity

@Database(
    entities = [
        RunEntity::class,
        RunSampleEntity::class,
        SeasonEntity::class,
        PersonalRecordEntity::class,
        EquipmentEntity::class,
        RunEquipmentCrossRef::class
    ],
    version = 3,
    exportSchema = true
)
abstract class PaceometerDatabase : RoomDatabase() {
    abstract fun runDao(): RunDao
    abstract fun runSampleDao(): RunSampleDao
    abstract fun seasonDao(): SeasonDao
    abstract fun personalRecordDao(): PersonalRecordDao
    abstract fun equipmentDao(): EquipmentDao

    companion object {
        const val DATABASE_NAME = "paceometer.db"

        /** Adds equipment tracking without touching existing runs/records already on-device. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `equipment` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`type` TEXT NOT NULL, " +
                        "`startingDistanceMeters` REAL NOT NULL, " +
                        "`createdAtEpochMs` INTEGER NOT NULL, " +
                        "`retired` INTEGER NOT NULL, " +
                        "`retiredAtEpochMs` INTEGER)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `run_equipment` (" +
                        "`runId` INTEGER NOT NULL, " +
                        "`equipmentId` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`runId`, `equipmentId`), " +
                        "FOREIGN KEY(`runId`) REFERENCES `runs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                        "FOREIGN KEY(`equipmentId`) REFERENCES `equipment`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_run_equipment_runId` ON `run_equipment` (`runId`)")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_run_equipment_equipmentId` ON `run_equipment` (`equipmentId`)"
                )
            }
        }
    }
}
