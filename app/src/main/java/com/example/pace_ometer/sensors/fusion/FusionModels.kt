package com.example.pace_ometer.sensors.fusion

/** A single raw GPS fix, decoupled from android.location.Location so the fusion engine is a plain JVM unit. */
data class GpsFix(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double?,
    val accuracyMeters: Float,
    val timestampMs: Long
)

data class FusedPoint(
    val timestampMs: Long,
    val latitude: Double?,
    val longitude: Double?,
    val elevationMeters: Double?,
    val cumulativeDistanceMeters: Double,
    val instantaneousPaceSecPerKm: Double?,
    val sourceFlags: Int
)
