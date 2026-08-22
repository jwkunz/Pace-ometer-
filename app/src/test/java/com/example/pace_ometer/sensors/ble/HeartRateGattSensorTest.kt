package com.example.pace_ometer.sensors.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HeartRateGattSensorTest {

    @Test
    fun `parses 8-bit heart rate value`() {
        // flags = 0x00 (8-bit format), bpm = 72
        val bpm = HeartRateGattSensor.parseHeartRate(byteArrayOf(0x00, 72))
        assertEquals(72, bpm)
    }

    @Test
    fun `parses 16-bit heart rate value little-endian`() {
        // flags = 0x01 (16-bit format), bpm = 300 (0x012C) -> low byte 0x2C, high byte 0x01
        val bpm = HeartRateGattSensor.parseHeartRate(byteArrayOf(0x01, 0x2C, 0x01))
        assertEquals(300, bpm)
    }

    @Test
    fun `returns null for empty or truncated data`() {
        assertNull(HeartRateGattSensor.parseHeartRate(byteArrayOf()))
        assertNull(HeartRateGattSensor.parseHeartRate(byteArrayOf(0x01, 0x2C))) // claims 16-bit but only 1 data byte
    }
}
