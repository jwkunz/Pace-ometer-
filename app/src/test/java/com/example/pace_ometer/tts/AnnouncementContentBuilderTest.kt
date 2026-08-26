package com.example.pace_ometer.tts

import com.example.pace_ometer.data.settings.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnnouncementContentBuilderTest {

    private val snapshot = AnnouncementSnapshot(
        distanceMeters = 1000.0,
        elapsedDurationMs = 5 * 60 * 1000L,
        elevationMeters = 50.0,
        elevationChangeLastSegmentMeters = -10.0,
        heartRateBpm = 150,
        cadenceSpm = 170,
        segmentPaceSecPerKm = 300.0,
        splitPaceSecPerKm = 305.0,
        cumulativeCalories = 87.4,
        clockTimeEpochMs = System.currentTimeMillis()
    )

    @Test
    fun `only includes toggled-on items`() {
        val settings = UserSettings(
            announceDistance = true,
            announceElapsedTime = false,
            announceElevation = false,
            announceHeartRate = false,
            announceCadence = false,
            announceSegmentPace = false,
            announceSplitPace = false,
            announceElevationChangeLastSegment = false,
            announceCalories = false,
            announceClockTime = false
        )
        val phrases = AnnouncementContentBuilder.build(settings, snapshot)
        assertEquals(1, phrases.size)
        assertTrue(phrases[0].startsWith("Distance"))
    }

    @Test
    fun `includes heart rate and calories when toggled on and present`() {
        val settings = UserSettings(
            announceDistance = false,
            announceElapsedTime = false,
            announceSegmentPace = false,
            announceHeartRate = true,
            announceCalories = true
        )
        val phrases = AnnouncementContentBuilder.build(settings, snapshot)
        assertTrue(phrases.any { it.contains("150") })
        assertTrue(phrases.any { it.contains("87") })
    }

    @Test
    fun `includes heart rate zone when toggled on with a birthdate on file`() {
        val settings = UserSettings(
            announceDistance = false,
            announceElapsedTime = false,
            announceSegmentPace = false,
            announceHeartRateZone = true,
            birthDateEpochDay = java.time.LocalDate.of(1990, 1, 1).toEpochDay()
        )
        val phrases = AnnouncementContentBuilder.build(settings, snapshot)
        assertTrue(phrases.any { it.contains("Heart rate zone") })
    }

    @Test
    fun `omits heart rate zone phrase when toggled on but no birthdate on file`() {
        val settings = UserSettings(
            announceDistance = false,
            announceElapsedTime = false,
            announceSegmentPace = false,
            announceHeartRateZone = true,
            birthDateEpochDay = null
        )
        val phrases = AnnouncementContentBuilder.build(settings, snapshot)
        assertFalse(phrases.any { it.contains("Heart rate zone") })
    }

    @Test
    fun `omits heart rate phrase when toggled on but no reading present`() {
        val settings = UserSettings(
            announceDistance = false,
            announceElapsedTime = false,
            announceSegmentPace = false,
            announceHeartRate = true
        )
        val phrases = AnnouncementContentBuilder.build(settings, snapshot.copy(heartRateBpm = null))
        assertFalse(phrases.any { it.contains("beats per minute") })
    }
}
