package com.example.pace_ometer.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/** One of the five standard heart-rate training zones, as a percentage band of HRmax. */
enum class HeartRateZone(val number: Int, val minPercentOfMax: Int, val maxPercentOfMax: Int, val label: String) {
    ZONE_1(1, 50, 60, "Very light"),
    ZONE_2(2, 60, 70, "Light"),
    ZONE_3(3, 70, 80, "Moderate"),
    ZONE_4(4, 80, 90, "Hard"),
    ZONE_5(5, 90, 101, "Maximum")
}

/**
 * Age from birthdate, estimated max heart rate (Tanaka formula -- more accurate across adult ages
 * than the old 220-age rule of thumb), and the 5-zone bands derived from it.
 */
object AgeAndHrZoneCalculator {

    fun ageYears(birthDateEpochDay: Long, atEpochMs: Long = System.currentTimeMillis()): Int {
        val birthDate = LocalDate.ofEpochDay(birthDateEpochDay)
        val today = Instant.ofEpochMilli(atEpochMs).atZone(ZoneId.systemDefault()).toLocalDate()
        return java.time.Period.between(birthDate, today).years
    }

    /** Tanaka formula: HRmax = 208 - 0.7 * age. */
    fun estimatedMaxHeartRateBpm(ageYears: Int): Int = (208.0 - 0.7 * ageYears).roundToInt()

    /** Current heart rate as a percentage of estimated max, e.g. 78 for 78%. */
    fun effortPercent(heartRateBpm: Int, maxHeartRateBpm: Int): Int {
        if (maxHeartRateBpm <= 0) return 0
        return ((heartRateBpm.toDouble() / maxHeartRateBpm) * 100).roundToInt()
    }

    /** Which of the 5 training zones [heartRateBpm] falls into, or null if below Zone 1 (under 50% of max). */
    fun zoneFor(heartRateBpm: Int, maxHeartRateBpm: Int): HeartRateZone? {
        val percent = effortPercent(heartRateBpm, maxHeartRateBpm)
        if (percent < HeartRateZone.ZONE_1.minPercentOfMax) return null
        return HeartRateZone.entries.lastOrNull { percent >= it.minPercentOfMax }
    }
}
