package com.example.pace_ometer.sensors.ble

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/** Common shape for a BLE GATT peripheral this app consumes readings from during a run. */
interface BleSensor<T> {
    val serviceUuid: UUID
    val isConnected: StateFlow<Boolean>
    val readings: Flow<T>
    fun connect(device: BluetoothDevice)
    fun disconnect()
}

data class HeartRateReading(val bpm: Int, val timestampMs: Long)

/**
 * Cadence in steps-per-minute plus the raw wheel/crank-style counters the RSC Measurement
 * characteristic exposes, kept for a future, hardware-validated implementation.
 */
data class CadenceReading(val cadenceSpm: Int?, val speedMps: Float?, val timestampMs: Long)

object BleServiceUuids {
    val HEART_RATE_SERVICE: UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb")
    val HEART_RATE_MEASUREMENT: UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb")

    val RUNNING_SPEED_AND_CADENCE_SERVICE: UUID = UUID.fromString("00001814-0000-1000-8000-00805f9b34fb")
    val RSC_MEASUREMENT: UUID = UUID.fromString("00002A53-0000-1000-8000-00805f9b34fb")

    val CLIENT_CHARACTERISTIC_CONFIG: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
}
