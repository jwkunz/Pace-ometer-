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
    version = 5,
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

        /** Adds step-count tracking without touching existing runs already on-device. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `runs` ADD COLUMN `stepCount` INTEGER")
            }
        }

        /**
         * Adds activity-type support (Running/Walking/Cycling). Every run recorded before this
         * migration was, in effect, a run -- so existing rows backfill as RUNNING rather than an
         * unknown/unset state, matching what the app has only ever tracked until now.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `runs` ADD COLUMN `activityType` TEXT NOT NULL DEFAULT 'RUNNING'")
            }
        }
    }
}
