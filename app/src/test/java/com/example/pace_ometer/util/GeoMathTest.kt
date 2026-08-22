package com.example.pace_ometer.util

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoMathTest {
    @Test
    fun `one degree of longitude at the equator matches the mean-earth-radius great circle length`() {
        // R * (pi / 180), for the same mean radius (6,371 km) used by haversineMeters.
        val distance = haversineMeters(0.0, 0.0, 0.0, 1.0)
        assertEquals(111_194.9, distance, 1.0)
    }

    @Test
    fun `identical points have zero distance`() {
        assertEquals(0.0, haversineMeters(37.0, -122.0, 37.0, -122.0), 0.0001)
    }
}
