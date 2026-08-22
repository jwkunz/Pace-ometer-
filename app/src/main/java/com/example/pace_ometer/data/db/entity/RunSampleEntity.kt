package com.example.pace_ometer.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

object SampleSource {
    const val GPS_FIX = 1
    const val ACCEL_DEAD_RECKON = 1 shl 1
    const val BLE_HEART_RATE = 1 shl 2
    const val BLE_CADENCE = 1 shl 3
}

@Entity(
    tableName = "run_samples",
    foreignKeys = [
        ForeignKey(
            entity = RunEntity::class,
            parentColumns = ["id"],
            childColumns = ["runId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("runId")]
)
data class RunSampleEntity(
    @PrimaryKey(autoGenerate = true) val sampleId: Long = 0,
    val runId: Long,
    val timestampEpochMs: Long,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val elevationMeters: Double? = null,
    val cumulativeDistanceMeters: Double,
    val instantaneousPaceSecPerKm: Double? = null,
    val heartRateBpm: Int? = null,
    val cadenceSpm: Int? = null,
    val sourceFlags: Int
)
