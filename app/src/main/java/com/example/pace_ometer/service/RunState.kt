package com.example.pace_ometer.service

import com.example.pace_ometer.data.ActivityType

enum class RunPhase { IDLE, RUNNING, PAUSED, STOPPED }

data class RunState(
    val phase: RunPhase = RunPhase.IDLE,
    val runId: Long? = null,
    val activityType: ActivityType = ActivityType.RUNNING,
    val distanceMeters: Double = 0.0,
    val movingDurationMs: Long = 0,
    val elevationMeters: Double? = null,
    val elevationGainMeters: Double = 0.0,
    val elevationLossMeters: Double = 0.0,
    val currentPaceSecPerKm: Double? = null,
    val segmentPaceSecPerKm: Double? = null,
    val elevationChangeLastSegmentMeters: Double? = null,
    val heartRateBpm: Int? = null,
    val cadenceSpm: Int? = null,
    val caloriesBurned: Double = 0.0,
    val stepCount: Int = 0,
    val strideLengthMeters: Double = 0.0
)
