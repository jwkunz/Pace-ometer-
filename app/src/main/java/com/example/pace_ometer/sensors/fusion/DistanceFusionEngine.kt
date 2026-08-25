package com.example.pace_ometer.sensors.fusion

import com.example.pace_ometer.data.db.entity.SampleSource
import com.example.pace_ometer.util.haversineMeters
import kotlin.math.roundToInt

private const val CADENCE_WINDOW_MS = 10_000L
private const val CADENCE_MIN_SAMPLES = 3

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
 * double-integrating raw acceleration, which drifts too fast to be usable. Pace during a gap is
 * likewise derived from the accelerometer -- each step's interval since the previous one implies
 * a speed (stride length / interval), fed into the same smoothing filter GPS fixes use -- rather
 * than staying frozen at the last GPS-derived value for the whole gap.
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
    private var lastGapStepTimestampMs: Long? = null
    private val recentStepTimestampsMs = ArrayDeque<Long>()

    val distanceMeters: Double get() = cumulativeDistanceMeters
    val currentStrideLengthMeters: Double get() = strideLengthMeters
    val lastFixTimestampMs: Long? get() = lastAcceptedFix?.timestampMs
    val stepCount: Int get() = totalSteps

    /** Steps-per-minute over a short rolling window of recent step timestamps, from whichever
     *  source (hardware step sensor or accelerometer peak detection) is currently feeding steps
     *  -- independent of GPS state, so this works even during a GPS gap. Null until enough
     *  recent steps have been observed to make the estimate meaningful. */
    val cadenceSpm: Int?
        get() {
            if (recentStepTimestampsMs.size < CADENCE_MIN_SAMPLES) return null
            val windowSeconds = (recentStepTimestampsMs.last() - recentStepTimestampsMs.first()) / 1000.0
            if (windowSeconds <= 0) return null
            return ((recentStepTimestampsMs.size - 1) / windowSeconds * 60).roundToInt()
        }

    private fun recordStepForCadence(stepTimestampMs: Long) {
        recentStepTimestampsMs.addLast(stepTimestampMs)
        while (recentStepTimestampsMs.size > 1 &&
            stepTimestampMs - recentStepTimestampsMs.first() > CADENCE_WINDOW_MS
        ) {
            recentStepTimestampsMs.removeFirst()
        }
    }

    /**
     * True once no GPS fix has been accepted for [gpsGapThresholdMs] -- also true when no fix
     * has EVER been accepted yet (e.g. a run started indoors/without signal), since that's
     * exactly the condition the accelerometer fallback needs to cover, not just a fix going
     * stale mid-run.
     */
    fun isInGpsGap(nowMs: Long): Boolean {
        val last = lastAcceptedFix?.timestampMs ?: return true
        return nowMs - last > gpsGapThresholdMs
    }

    fun onGpsFix(fix: GpsFix): FusedPoint {
        val previous = lastAcceptedFix

        if (fix.accuracyMeters > maxAcceptableAccuracyMeters) {
            return currentFusedPoint(fix.timestampMs)
        }

        if (previous == null) {
            lastAcceptedFix = fix
            lastGapStepTimestampMs = null
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
            lastGapStepTimestampMs = null
            return currentFusedPoint(fix.timestampMs, elevation = fix.altitudeMeters)
        }

        smoothedSpeedMps = speedSmoothingAlpha * impliedSpeedMps + (1 - speedSmoothingAlpha) * smoothedSpeedMps
        cumulativeDistanceMeters += deltaMeters
        calibrationDistanceMeters += deltaMeters
        lastAcceptedFix = fix
        lastGapStepTimestampMs = null

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
    fun onStepDuringGoodGps(stepTimestampMs: Long) {
        totalSteps += 1
        recordStepForCadence(stepTimestampMs)
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
        recordStepForCadence(stepTimestampMs)
        cumulativeDistanceMeters += strideLengthMeters

        val previousStepMs = lastGapStepTimestampMs
        if (previousStepMs != null) {
            val stepIntervalSeconds = (stepTimestampMs - previousStepMs) / 1000.0
            if (stepIntervalSeconds > 0) {
                val stepSpeedMps = strideLengthMeters / stepIntervalSeconds
                if (stepSpeedMps <= maxPlausibleSpeedMps) {
                    smoothedSpeedMps = speedSmoothingAlpha * stepSpeedMps + (1 - speedSmoothingAlpha) * smoothedSpeedMps
                }
            }
        }
        lastGapStepTimestampMs = stepTimestampMs

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
