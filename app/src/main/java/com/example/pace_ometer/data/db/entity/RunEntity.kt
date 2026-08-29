package com.example.pace_ometer.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "runs")
data class RunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTimeEpochMs: Long,
    val endTimeEpochMs: Long? = null,
    val totalDistanceMeters: Double = 0.0,
    val totalDurationMs: Long = 0,
    val movingDurationMs: Long = 0,
    val averagePaceSecPerKm: Double? = null,
    val elevationGainMeters: Double? = null,
    val elevationLossMeters: Double? = null,
    val avgHeartRateBpm: Int? = null,
    val maxHeartRateBpm: Int? = null,
    val avgCadenceSpm: Int? = null,
    val estimatedCalories: Double? = null,
    val stepCount: Int? = null,
    val bodyWeightKgAtRunTime: Double? = null,
    val unitSystemAtRunTime: String = "METRIC",
    val activityType: String = "RUNNING",
    val isSaved: Boolean = false,
    val notes: String? = null
)
