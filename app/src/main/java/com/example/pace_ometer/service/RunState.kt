package com.example.pace_ometer.service

enum class RunPhase { IDLE, RUNNING, PAUSED, STOPPED }

data class RunState(
    val phase: RunPhase = RunPhase.IDLE,
    val runId: Long? = null,
    val distanceMeters: Double = 0.0,
    val movingDurationMs: Long = 0,
    val elevationMeters: Double? = null,
    val elevationGainMeters: Double = 0.0,
    val elevationLossMeters: Double = 0.0,
    val currentPaceSecPerKm: Double? = null,
    val heartRateBpm: Int? = null,
    val cadenceSpm: Int? = null,
    val caloriesBurned: Double = 0.0
)
