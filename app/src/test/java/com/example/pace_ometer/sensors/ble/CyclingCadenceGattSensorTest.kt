package com.example.pace_ometer.sensors.ble

import com.example.pace_ometer.sensors.ble.CyclingCadenceGattSensor.Companion.CrankState
import com.example.pace_ometer.sensors.ble.CyclingCadenceGattSensor.Companion.parseCscMeasurement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CyclingCadenceGattSensorTest {

    private fun cscMeasurement(crankRevolutions: Int, eventTime1024ths: Int): ByteArray {
        // flags = 0x02 (crank data present, no wheel data)
        return byteArrayOf(
            0x02,
            (crankRevolutions and 0xFF).toByte(),
            ((crankRevolutions shr 8) and 0xFF).toByte(),
            (eventTime1024ths and 0xFF).toByte(),
            ((eventTime1024ths shr 8) and 0xFF).toByte()
        )
    }

    @Test
    fun `first reading after connect has no previous state to diff against`() {
        val (state, reading) = parseCscMeasurement(cscMeasurement(100, 1024), previous = null)
        assertNull(reading)
        assertEquals(CrankState(100, 1024), state)
    }

    @Test
    fun `derives cadence rpm from the delta against the previous reading`() {
        // 10 revolutions over exactly 1 second (1024 units of 1/1024s) -> 600 rpm... use a
        // smaller, realistic delta: 1 revolution per 1024 units (1s) -> 60 rpm.
        val previous = CrankState(cumulativeRevolutions = 100, eventTime1024ths = 1024)
        val (_, reading) = parseCscMeasurement(cscMeasurement(101, 2048), previous)
        assertEquals(60, reading?.cadenceSpm)
    }

    @Test
    fun `returns null reading when no crank data is present`() {
        // flags = 0x00 -> crank bit not set
        val (state, reading) = parseCscMeasurement(byteArrayOf(0x00), previous = null)
        assertNull(reading)
        assertNull(state)
    }

    @Test
    fun `handles 16-bit wraparound of the revolution counter`() {
        val previous = CrankState(cumulativeRevolutions = 65530, eventTime1024ths = 1024)
        // Wraps past 65535 back to 4 -> a delta of 10 revolutions.
        val (_, reading) = parseCscMeasurement(cscMeasurement(4, 2048), previous)
        assertEquals(600, reading?.cadenceSpm)
    }

    @Test
    fun `returns null when the event time delta is zero`() {
        val previous = CrankState(cumulativeRevolutions = 100, eventTime1024ths = 1024)
        val (_, reading) = parseCscMeasurement(cscMeasurement(101, 1024), previous)
        assertNull(reading)
    }
}
