package com.example.pace_ometer.sensors.fusion

import com.example.pace_ometer.data.db.entity.SampleSource
import com.example.pace_ometer.util.haversineMeters

/**
 * Combines GPS fixes (primary source) with accelerometer step events (fallback during GPS
 * gaps only) into a single authoritative distance/pace stream.
 *
 * GPS handling: fixes with poor accuracy or an implausible implied speed (likely a jump
 * artifact) are rejected; accepted fixes are accumulated via haversine distance and their
 * speed is smoothed with an exponential moving average (a lightweight "Kalman-lite" filter,
 * not a full Kalman implementation).
 *
 * Gap handling: when no GPS fix has arrived for [gpsGapThresholdMs], callers should switch to
 * feeding step events via [onStepDetected], which advances distance using a stride length
 * continuously calibrated from recent good-GPS pace (see [onStepDuringGoodGps]) rather than by
 * double-integrating raw acceleration, which drifts too fast to be usable.
 */
class DistanceFusionEngine(
    private val maxAcceptableAccuracyMeters: Float = 20f,
    private val maxPlausibleSpeedMps: Double = 7.0,
    private val gpsGapThresholdMs: Long = 8000L,
    private val speedSmoothingAlpha: Double = 0.3,
    defaultStrideLengthMeters: Double = 0.75,
    private val strideCalibrationStepCount: Int = 20
) {
    private var lastAcceptedFix: GpsFix? = null
    private var smoothedSpeedMps: Double = 0.0
    private var cumulativeDistanceMeters: Double = 0.0
    private var strideLengthMeters: Double = defaultStrideLengthMeters

    private var calibrationDistanceMeters: Double = 0.0
    private var calibrationSteps: Int = 0
    private var totalSteps: Int = 0

    val distanceMeters: Double get() = cumulativeDistanceMeters
    val currentStrideLengthMeters: Double get() = strideLengthMeters
    val lastFixTimestampMs: Long? get() = lastAcceptedFix?.timestampMs
    val stepCount: Int get() = totalSteps

    fun isInGpsGap(nowMs: Long): Boolean {
        val last = lastAcceptedFix?.timestampMs ?: return false
        return nowMs - last > gpsGapThresholdMs
    }

    fun onGpsFix(fix: GpsFix): FusedPoint {
        val previous = lastAcceptedFix

        if (fix.accuracyMeters > maxAcceptableAccuracyMeters) {
            return currentFusedPoint(fix.timestampMs)
        }

        if (previous == null) {
            lastAcceptedFix = fix
            return currentFusedPoint(fix.timestampMs, elevation = fix.altitudeMeters)
        }

        val deltaSeconds = (fix.timestampMs - previous.timestampMs) / 1000.0
        if (deltaSeconds <= 0) {
            return currentFusedPoint(fix.timestampMs, elevation = fix.altitudeMeters)
        }

        val deltaMeters = haversineMeters(previous.latitude, previous.longitude, fix.latitude, fix.longitude)
        val impliedSpeedMps = deltaMeters / deltaSeconds

        if (impliedSpeedMps > maxPlausibleSpeedMps) {
            // Likely a GPS jump artifact: don't accumulate distance, but keep this fix as the
            // new reference point so a single bad sample doesn't permanently wedge the filter.
            lastAcceptedFix = fix
            return currentFusedPoint(fix.timestampMs, elevation = fix.altitudeMeters)
        }

        smoothedSpeedMps = speedSmoothingAlpha * impliedSpeedMps + (1 - speedSmoothingAlpha) * smoothedSpeedMps
        cumulativeDistanceMeters += deltaMeters
        calibrationDistanceMeters += deltaMeters
        lastAcceptedFix = fix

        return FusedPoint(
            timestampMs = fix.timestampMs,
            latitude = fix.latitude,
            longitude = fix.longitude,
            elevationMeters = fix.altitudeMeters,
            cumulativeDistanceMeters = cumulativeDistanceMeters,
            instantaneousPaceSecPerKm = paceSecPerKmOrNull(),
            sourceFlags = SampleSource.GPS_FIX
        )
    }

    /** Feed step events observed while GPS is fresh, to continuously calibrate [strideLengthMeters]. */
    fun onStepDuringGoodGps() {
        totalSteps += 1
        calibrationSteps += 1
        if (calibrationSteps >= strideCalibrationStepCount && calibrationDistanceMeters > 0) {
            strideLengthMeters = calibrationDistanceMeters / calibrationSteps
            calibrationDistanceMeters = 0.0
            calibrationSteps = 0
        }
    }

    /** Feed step events while [isInGpsGap] is true, to dead-reckon distance until GPS resumes. */
    fun onStepDetected(stepTimestampMs: Long): FusedPoint {
        totalSteps += 1
        cumulativeDistanceMeters += strideLengthMeters
        return FusedPoint(
            timestampMs = stepTimestampMs,
            latitude = null,
            longitude = null,
            elevationMeters = null,
            cumulativeDistanceMeters = cumulativeDistanceMeters,
            instantaneousPaceSecPerKm = paceSecPerKmOrNull(),
            sourceFlags = SampleSource.ACCEL_DEAD_RECKON
        )
    }

    private fun paceSecPerKmOrNull(): Double? = if (smoothedSpeedMps > 0.2) 1000.0 / smoothedSpeedMps else null

    private fun currentFusedPoint(timestampMs: Long, elevation: Double? = null): FusedPoint = FusedPoint(
        timestampMs = timestampMs,
        latitude = lastAcceptedFix?.latitude,
        longitude = lastAcceptedFix?.longitude,
        elevationMeters = elevation,
        cumulativeDistanceMeters = cumulativeDistanceMeters,
        instantaneousPaceSecPerKm = paceSecPerKmOrNull(),
        sourceFlags = SampleSource.GPS_FIX
    )
}
