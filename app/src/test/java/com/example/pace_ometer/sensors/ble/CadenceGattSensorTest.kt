package com.example.pace_ometer.sensors.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CadenceGattSensorTest {

    @Test
    fun `parses speed and cadence from a minimal RSC measurement`() {
        // flags = 0x00, speed raw = 768 (0x0300) -> 768/256 = 3.0 m/s, cadence = 170 spm
        val reading = CadenceGattSensor.parseRscMeasurement(byteArrayOf(0x00, 0x00, 0x03, 170.toByte()))
        assertEquals(3.0f, reading?.speedMps)
        assertEquals(170, reading?.cadenceSpm)
    }

    @Test
    fun `returns null when data is too short`() {
        assertNull(CadenceGattSensor.parseRscMeasurement(byteArrayOf(0x00, 0x00)))
    }
}
