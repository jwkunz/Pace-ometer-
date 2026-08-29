package com.example.pace_ometer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TimeFormatterTest {

    @Test
    fun `speakableDurationMs never contains a colon`() {
        // A "5:00"-shaped string is what made Android's TTS read this as a clock time ("five
        // o'clock") instead of a duration -- guard against ever producing that format again.
        for (durationMs in listOf(0L, 5_000L, 90_000L, 300_000L, 3_661_000L)) {
            val phrase = speakableDurationMs(durationMs)
            assertFalse("phrase '$phrase' contains a colon", phrase.contains(":"))
        }
    }

    @Test
    fun `speakableDurationMs formats whole minutes without a seconds clause`() {
        assertEquals("5 minutes", speakableDurationMs(300_000L))
    }

    @Test
    fun `speakableDurationMs includes seconds when not exactly on the minute`() {
        assertEquals("5 minutes 30 seconds", speakableDurationMs(330_000L))
    }

    @Test
    fun `speakableDurationMs includes hours when over an hour`() {
        assertEquals("1 hour 1 minute 1 second", speakableDurationMs(3_661_000L))
    }

    @Test
    fun `speakableDurationMs falls back to zero seconds for no elapsed time`() {
        assertEquals("0 seconds", speakableDurationMs(0L))
    }
}
