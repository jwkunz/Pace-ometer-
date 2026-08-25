package com.example.pace_ometer.sensors.fusion

import com.example.pace_ometer.data.db.entity.SampleSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

private const val EARTH_RADIUS_METERS = 6_371_000.0

/** One degree of latitude offset (from 0,0) corresponding to roughly [meters] of northward travel. */
private fun latDegreesFor(meters: Double): Double = Math.toDegrees(meters / EARTH_RADIUS_METERS)

class DistanceFusionEngineTest {

    @Test
    fun `accumulates distance across evenly spaced good fixes`() {
        val engine = DistanceFusionEngine()
        val stepLat = latDegreesFor(100.0)
        var lastPoint: FusedPoint? = null

        // 10 fixes, 100m apart, 30s apart -> ~3.33 m/s -> ~300 sec/km once the EMA settles.
        for (i in 0 until 10) {
            lastPoint = engine.onGpsFix(
                GpsFix(
                    latitude = stepLat * i,
                    longitude = 0.0,
                    altitudeMeters = null,
                    accuracyMeters = 5f,
                    timestampMs = i * 30_000L
                )
            )
        }

        assertEquals(900.0, engine.distanceMeters, 1.0)
        val pace = lastPoint!!.instantaneousPaceSecPerKm ?: 0.0
        assertTrue("expected pace to converge near 300 sec/km, was $pace", abs(pace - 300.0) < 25.0)
    }

    @Test
    fun `rejects a fix with poor accuracy without moving the reference point`() {
        val engine = DistanceFusionEngine()
        val stepLat = latDegreesFor(100.0)

        engine.onGpsFix(GpsFix(0.0, 0.0, null, 5f, 0L))
        // Bad-accuracy fix far away; should be ignored entirely.
        engine.onGpsFix(GpsFix(stepLat * 50, 0.0, null, 50f, 15_000L))
        val afterGoodFix = engine.onGpsFix(GpsFix(stepLat, 0.0, null, 5f, 30_000L))

        // Distance should reflect fix1 -> fix3 (~100m), not fix2 -> fix3.
        assertEquals(100.0, afterGoodFix.cumulativeDistanceMeters, 1.0)
    }

    @Test
    fun `rejects an implausible speed jump as an outlier`() {
        val engine = DistanceFusionEngine(maxPlausibleSpeedMps = 7.0)
        engine.onGpsFix(GpsFix(0.0, 0.0, null, 5f, 0L))
        // 5km in 2 seconds is ~2500 m/s -- clearly a jump artifact.
        val jumpPoint = engine.onGpsFix(GpsFix(latDegreesFor(5000.0), 0.0, null, 5f, 2_000L))

        assertEquals(0.0, jumpPoint.cumulativeDistanceMeters, 0.001)
    }

    @Test
    fun `dead reckons distance from steps during a gps gap`() {
        val engine = DistanceFusionEngine(gpsGapThresholdMs = 8_000L, defaultStrideLengthMeters = 0.8)
        engine.onGpsFix(GpsFix(0.0, 0.0, null, 5f, 0L))

        assertTrue(engine.isInGpsGap(nowMs = 9_000L))

        repeat(5) { engine.onStepDetected(stepTimestampMs = 9_000L + it * 500L) }

        assertEquals(4.0, engine.distanceMeters, 0.001) // 5 steps * 0.8m
        assertTrue(!engine.isInGpsGap(nowMs = 1_000L)) // shortly after the fix, not yet in a gap
    }

    @Test
    fun `derives pace from step cadence during a gps gap instead of freezing it`() {
        val engine = DistanceFusionEngine(gpsGapThresholdMs = 8_000L, defaultStrideLengthMeters = 0.8)
        engine.onGpsFix(GpsFix(0.0, 0.0, null, 5f, 0L))

        // 0.8m stride every 500ms -> 1.6 m/s -> 625 sec/km; run enough steps for the EMA to converge.
        var lastPoint: FusedPoint? = null
        repeat(40) { i -> lastPoint = engine.onStepDetected(stepTimestampMs = 9_000L + i * 500L) }

        val pace = lastPoint!!.instantaneousPaceSecPerKm ?: 0.0
        assertTrue("expected pace to converge near 625 sec/km, was $pace", abs(pace - 625.0) < 25.0)
    }

    @Test
    fun `calibrates stride length from recent good-gps pace`() {
        val engine = DistanceFusionEngine(defaultStrideLengthMeters = 0.75, strideCalibrationStepCount = 5)
        val stepLat = latDegreesFor(100.0)

        engine.onGpsFix(GpsFix(0.0, 0.0, null, 5f, 0L))
        engine.onGpsFix(GpsFix(stepLat, 0.0, null, 5f, 30_000L)) // 100m covered

        // 5 steps observed while covering that 100m -> calibrated stride should become 20m/step.
        repeat(5) { engine.onStepDuringGoodGps() }

        assertEquals(20.0, engine.currentStrideLengthMeters, 0.01)
    }

    @Test
    fun `first fix seeds the reference point without contributing distance`() {
        val engine = DistanceFusionEngine()
        val point = engine.onGpsFix(GpsFix(1.0, 1.0, 10.0, 5f, 0L))

        assertEquals(0.0, point.cumulativeDistanceMeters, 0.0001)
        assertEquals(SampleSource.GPS_FIX, point.sourceFlags)
    }
}
