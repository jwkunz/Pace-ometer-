package com.example.pace_ometer.util

import com.example.pace_ometer.data.settings.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class UnitFormatterTest {

    @Test
    fun `speakablePaceSecPerKm never contains a colon`() {
        // A "6:00"-shaped string is what made Android's TTS read this as a clock time ("six
        // o'clock") instead of a pace -- guard against ever producing that format again.
        for (secPerKm in listOf(0.0, 30.0, 90.0, 300.0, 305.0, 3600.0)) {
            val phrase = speakablePaceSecPerKm(secPerKm, UnitSystem.METRIC)
            assertFalse("phrase '$phrase' contains a colon", phrase!!.contains(":"))
        }
    }

    @Test
    fun `speakablePaceSecPerKm formats whole minutes without a seconds clause`() {
        assertEquals("5 minutes per kilometer", speakablePaceSecPerKm(300.0, UnitSystem.METRIC))
    }

    @Test
    fun `speakablePaceSecPerKm includes seconds when not exactly on the minute`() {
        assertEquals("5 minutes 30 seconds per kilometer", speakablePaceSecPerKm(330.0, UnitSystem.METRIC))
    }

    @Test
    fun `speakablePaceSecPerKm uses singular units for one minute one second`() {
        assertEquals("1 minute 1 second per kilometer", speakablePaceSecPerKm(61.0, UnitSystem.METRIC))
    }

    @Test
    fun `speakablePaceSecPerKm converts to miles for imperial`() {
        // 300 sec/km * 1.609344 km/mi ~= 483 sec/mi = 8:03/mi.
        assertEquals("8 minutes 3 seconds per mile", speakablePaceSecPerKm(300.0, UnitSystem.IMPERIAL))
    }

    @Test
    fun `speakablePaceSecPerKm returns null for an invalid pace`() {
        assertNull(speakablePaceSecPerKm(null, UnitSystem.METRIC))
        assertNull(speakablePaceSecPerKm(Double.NaN, UnitSystem.METRIC))
        assertNull(speakablePaceSecPerKm(Double.POSITIVE_INFINITY, UnitSystem.METRIC))
    }
}
