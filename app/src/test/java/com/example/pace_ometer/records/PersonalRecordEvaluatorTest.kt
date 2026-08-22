package com.example.pace_ometer.records

import com.example.pace_ometer.data.db.entity.PersonalRecordCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalRecordEvaluatorTest {

    /** 100 samples, 100m apart, 30s apart -> a steady 10km run at ~3.33 m/s (5:00/km). */
    private val steadySamples: List<Pair<Long, Double>> =
        (0..100).map { i -> (i * 30_000L) to (i * 100.0) }

    private fun candidate(candidates: List<PersonalRecordEvaluator.Candidate>, category: String) =
        candidates.firstOrNull { it.category == category }?.value

    @Test
    fun `computes longest distance and overall pace directly from run totals`() {
        val candidates = PersonalRecordEvaluator.evaluate(
            totalDistanceMeters = 10_000.0,
            movingDurationMs = 3_000_000L,
            samples = steadySamples
        )
        assertEquals(10_000.0, candidate(candidates, PersonalRecordCategory.LONGEST_DISTANCE)!!, 0.001)
        // 3,000,000ms / 1000 = 3000s over 10km -> 300 sec/km
        assertEquals(300.0, candidate(candidates, PersonalRecordCategory.FASTEST_OVERALL_PACE)!!, 0.001)
    }

    @Test
    fun `finds the best 1K and 5K splits via sliding window`() {
        val candidates = PersonalRecordEvaluator.evaluate(10_000.0, 3_000_000L, steadySamples)
        // Exactly 10 segments of 100m = 1000m, 30s apart each -> 300s.
        assertEquals(300.0, candidate(candidates, PersonalRecordCategory.FASTEST_1K)!!, 0.001)
        // Exactly 50 segments = 5000m -> 1500s.
        assertEquals(1500.0, candidate(candidates, PersonalRecordCategory.FASTEST_5K)!!, 0.001)
        // The whole run covers exactly 10000m -> 3000s.
        assertEquals(3000.0, candidate(candidates, PersonalRecordCategory.FASTEST_10K)!!, 0.001)
    }

    @Test
    fun `mile split rounds up to the nearest covered sample window`() {
        val candidates = PersonalRecordEvaluator.evaluate(10_000.0, 3_000_000L, steadySamples)
        // 1609.344m needs 17 segments of 100m (1700m >= target; 16 segments/1600m falls short) -> 17*30s.
        assertEquals(510.0, candidate(candidates, PersonalRecordCategory.FASTEST_MILE)!!, 0.001)
    }

    @Test
    fun `omits categories the run is too short to reach`() {
        val shortSamples = steadySamples.take(6) // 0..500m
        val candidates = PersonalRecordEvaluator.evaluate(500.0, 150_000L, shortSamples)
        assertNull(candidate(candidates, PersonalRecordCategory.FASTEST_1K))
        assertNull(candidate(candidates, PersonalRecordCategory.FASTEST_OVERALL_PACE))
        assertTrue(candidates.any { it.category == PersonalRecordCategory.LONGEST_DISTANCE })
    }
}
