package com.example.pace_ometer.calories

import com.example.pace_ometer.data.ActivityType

/**
 * Estimates calories burned using the standard MET formula:
 * calories = MET x weight(kg) x time(hours), with MET looked up (and linearly interpolated)
 * from speed per the ACSM Compendium of Physical Activities. Computing this per segment (rather
 * than once for the whole run) correctly reflects pace changes over time.
 *
 * Running and walking share one table (a single continuous walk/run speed range) since METs
 * scale with speed similarly for both. Cycling gets its own table -- at the same numeric speed,
 * cycling METs are much lower than running METs (efficient locomotion vs. foot-strike), so
 * reusing the running table would badly overestimate cycling calories.
 */
object CalorieEstimator {

    // (speed km/h, MET) control points, ascending by speed.
    private val runWalkMetBySpeedKmh = listOf(
        3.2 to 2.8,   // slow walk
        4.8 to 3.5,   // brisk walk
        6.4 to 6.0,   // walk/jog transition
        8.0 to 8.3,   // ~12:00/mi jog
        9.7 to 9.8,   // ~10:00/mi
        10.8 to 10.5, // ~9:00/mi
        12.1 to 11.8, // ~8:00/mi
        13.8 to 12.8, // ~7:00/mi
        16.1 to 14.8, // ~6:00/mi
        17.7 to 16.8, // ~5:20/mi
        19.3 to 19.0  // ~5:00/mi and faster
    )

    // (speed km/h, MET) control points for cycling, ascending by speed -- ACSM compendium
    // "bicycling, general" bands, much lower MET-per-speed than running/walking.
    private val cyclingMetBySpeedKmh = listOf(
        8.0 to 4.0,   // < 16 km/h, leisurely
        16.0 to 6.0,  // 16-19 km/h, light effort
        19.0 to 8.0,  // 19-22 km/h, moderate effort
        22.0 to 10.0, // 22-25 km/h, vigorous effort
        25.0 to 12.0, // 25-30 km/h, racing/fast
        30.0 to 15.8  // > 30 km/h, very fast/racing
    )

    private fun metTableFor(activityType: ActivityType) =
        if (activityType == ActivityType.CYCLING) cyclingMetBySpeedKmh else runWalkMetBySpeedKmh

    fun metForSpeedKmh(speedKmh: Double, activityType: ActivityType = ActivityType.RUNNING): Double {
        val table = metTableFor(activityType)
        if (speedKmh <= table.first().first) return table.first().second
        if (speedKmh >= table.last().first) return table.last().second

        for (i in 0 until table.size - 1) {
            val (speedLow, metLow) = table[i]
            val (speedHigh, metHigh) = table[i + 1]
            if (speedKmh in speedLow..speedHigh) {
                val fraction = (speedKmh - speedLow) / (speedHigh - speedLow)
                return metLow + fraction * (metHigh - metLow)
            }
        }
        return table.last().second
    }

    fun estimateSegmentCalories(
        speedKmh: Double,
        weightKg: Double,
        durationMs: Long,
        activityType: ActivityType = ActivityType.RUNNING
    ): Double {
        val hours = durationMs / 3_600_000.0
        return metForSpeedKmh(speedKmh, activityType) * weightKg * hours
    }
}
