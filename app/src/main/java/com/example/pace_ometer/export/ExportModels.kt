package com.example.pace_ometer.export

import kotlinx.serialization.Serializable

/**
 * Export DTOs, deliberately decoupled from the Room entities so the on-disk DB schema can
 * evolve without breaking this contract. Always expressed in fixed SI units (meters/seconds/
 * epoch-ms) regardless of the user's display unit setting, so the export stays consistently
 * machine-parseable for external tools.
 */
@Serializable
data class RunExport(
    val exportSchemaVersion: Int = 1,
    val exportedAtEpochMs: Long,
    val runs: List<RunExportEntry>
)

@Serializable
data class RunExportEntry(
    val id: Long,
    val startTimeEpochMs: Long,
    val endTimeEpochMs: Long?,
    val totalDistanceMeters: Double,
    val totalDurationMs: Long,
    val movingDurationMs: Long,
    val averagePaceSecPerKm: Double?,
    val elevationGainMeters: Double?,
    val elevationLossMeters: Double?,
    val avgHeartRateBpm: Int?,
    val maxHeartRateBpm: Int?,
    val avgCadenceSpm: Int?,
    val estimatedCalories: Double?,
    val unitSystemAtRunTime: String,
    val samples: List<RunSampleExportEntry>
)

@Serializable
data class RunSampleExportEntry(
    val timestampEpochMs: Long,
    val latitude: Double?,
    val longitude: Double?,
    val elevationMeters: Double?,
    val cumulativeDistanceMeters: Double,
    val instantaneousPaceSecPerKm: Double?,
    val heartRateBpm: Int?,
    val cadenceSpm: Int?
)
