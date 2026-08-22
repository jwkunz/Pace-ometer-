package com.example.pace_ometer.calories

/**
 * Estimates calories burned using the standard MET formula:
 * calories = MET x weight(kg) x time(hours), with MET looked up (and linearly interpolated)
 * from running speed per the ACSM Compendium of Physical Activities. Computing this per segment
 * (rather than once for the whole run) correctly reflects pace changes over time.
 */
object CalorieEstimator {

    // (speed km/h, MET) control points, ascending by speed.
    private val metBySpeedKmh = listOf(
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

    fun metForSpeedKmh(speedKmh: Double): Double {
        if (speedKmh <= metBySpeedKmh.first().first) return metBySpeedKmh.first().second
        if (speedKmh >= metBySpeedKmh.last().first) return metBySpeedKmh.last().second

        for (i in 0 until metBySpeedKmh.size - 1) {
            val (speedLow, metLow) = metBySpeedKmh[i]
            val (speedHigh, metHigh) = metBySpeedKmh[i + 1]
            if (speedKmh in speedLow..speedHigh) {
                val fraction = (speedKmh - speedLow) / (speedHigh - speedLow)
                return metLow + fraction * (metHigh - metLow)
            }
        }
        return metBySpeedKmh.last().second
    }

    fun estimateSegmentCalories(speedKmh: Double, weightKg: Double, durationMs: Long): Double {
        val hours = durationMs / 3_600_000.0
        return metForSpeedKmh(speedKmh) * weightKg * hours
    }
}
