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
 * Cadence -- steps-per-minute from [CadenceGattSensor] (running), or crank revolutions-per-minute
 * from [CyclingCadenceGattSensor] (cycling) -- plus the speed the sensor itself reports, if any.
 * Shared shape between both since a run only ever pairs one cadence source at a time.
 */
data class CadenceReading(val cadenceSpm: Int?, val speedMps: Float?, val timestampMs: Long)

object BleServiceUuids {
    val HEART_RATE_SERVICE: UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb")
    val HEART_RATE_MEASUREMENT: UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb")

    val RUNNING_SPEED_AND_CADENCE_SERVICE: UUID = UUID.fromString("00001814-0000-1000-8000-00805f9b34fb")
    val RSC_MEASUREMENT: UUID = UUID.fromString("00002A53-0000-1000-8000-00805f9b34fb")

    val CYCLING_SPEED_AND_CADENCE_SERVICE: UUID = UUID.fromString("00001816-0000-1000-8000-00805f9b34fb")
    val CSC_MEASUREMENT: UUID = UUID.fromString("00002A5B-0000-1000-8000-00805f9b34fb")
    val CYCLING_POWER_SERVICE: UUID = UUID.fromString("00001818-0000-1000-8000-00805f9b34fb")
    val FITNESS_MACHINE_SERVICE: UUID = UUID.fromString("00001826-0000-1000-8000-00805f9b34fb")

    val CLIENT_CHARACTERISTIC_CONFIG: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    /** Every standard BLE service this app can recognize when scanning for nearby athletic sensors. */
    val ATHLETIC_SERVICES: List<UUID> = listOf(
        HEART_RATE_SERVICE,
        RUNNING_SPEED_AND_CADENCE_SERVICE,
        CYCLING_SPEED_AND_CADENCE_SERVICE,
        CYCLING_POWER_SERVICE,
        FITNESS_MACHINE_SERVICE
    )

    fun label(uuid: UUID): String = when (uuid) {
        HEART_RATE_SERVICE -> "Heart rate"
        RUNNING_SPEED_AND_CADENCE_SERVICE -> "Running cadence"
        CYCLING_SPEED_AND_CADENCE_SERVICE -> "Cycling speed/cadence"
        CYCLING_POWER_SERVICE -> "Cycling power"
        FITNESS_MACHINE_SERVICE -> "Fitness machine"
        else -> "Unknown"
    }
}
