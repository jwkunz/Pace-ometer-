package com.example.pace_ometer.calories

import org.junit.Assert.assertEquals
import org.junit.Test

class CalorieEstimatorTest {

    @Test
    fun `known control point returns its exact MET value`() {
        assertEquals(9.8, CalorieEstimator.metForSpeedKmh(9.7), 0.001)
    }

    @Test
    fun `interpolates between two control points`() {
        // Halfway between 8.0->8.3 MET and 9.7->9.8 MET
        val midSpeed = (8.0 + 9.7) / 2
        val expectedMet = (8.3 + 9.8) / 2
        assertEquals(expectedMet, CalorieEstimator.metForSpeedKmh(midSpeed), 0.05)
    }

    @Test
    fun `clamps below the slowest control point`() {
        assertEquals(2.8, CalorieEstimator.metForSpeedKmh(0.0), 0.001)
    }

    @Test
    fun `clamps above the fastest control point`() {
        assertEquals(19.0, CalorieEstimator.metForSpeedKmh(30.0), 0.001)
    }

    @Test
    fun `estimates segment calories using MET times weight times hours`() {
        // 9.7 km/h -> MET 9.8; 70kg; 30 minutes (0.5h) -> 9.8 * 70 * 0.5 = 343
        val calories = CalorieEstimator.estimateSegmentCalories(9.7, 70.0, 30 * 60 * 1000L)
        assertEquals(343.0, calories, 0.5)
    }
}
