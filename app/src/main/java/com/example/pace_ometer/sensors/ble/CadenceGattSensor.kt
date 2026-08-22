package com.example.pace_ometer.sensors.ble

import android.bluetooth.BluetoothDevice
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/**
 * Bluetooth SIG Running Speed and Cadence Service (0x1814) / RSC Measurement (0x2A53).
 *
 * No cadence sensor is available to validate this against real hardware yet, so this is a
 * best-effort implementation of the spec's byte layout, wired the same way as
 * [HeartRateGattSensor] so a future cadence sensor can be paired without further rearchitecting.
 * TODO: validate field offsets/scaling against a real RSC-compatible sensor once one is available.
 */
class CadenceGattSensor(context: Context) : BleSensor<CadenceReading> {

    private val gattClient = GattClientManager(context)
    private val _readings = MutableSharedFlow<CadenceReading>(replay = 0, extraBufferCapacity = 4)

    override val serviceUuid: UUID = BleServiceUuids.RUNNING_SPEED_AND_CADENCE_SERVICE
    override val isConnected: StateFlow<Boolean> = gattClient.isConnected
    override val readings: Flow<CadenceReading> = _readings

    override fun connect(device: BluetoothDevice) {
        gattClient.connect(
            device = device,
            serviceUuid = BleServiceUuids.RUNNING_SPEED_AND_CADENCE_SERVICE,
            characteristicUuid = BleServiceUuids.RSC_MEASUREMENT,
            onValueChanged = { bytes ->
                parseRscMeasurement(bytes)?.let { reading ->
                    _readings.tryEmit(reading.copy(timestampMs = System.currentTimeMillis()))
                }
            }
        )
    }

    override fun disconnect() = gattClient.disconnect()

    companion object {
        fun parseRscMeasurement(data: ByteArray): CadenceReading? {
            if (data.size < 4) return null
            // Flags byte (data[0]) also gates optional stride-length/total-distance fields that
            // trail the cadence value; neither is needed by this app, so they're left unparsed.
            val rawSpeed = ((data[2].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
            val speedMps = rawSpeed / 256f
            val cadenceRpm = data[3].toInt() and 0xFF

            return CadenceReading(cadenceSpm = cadenceRpm, speedMps = speedMps, timestampMs = 0L)
        }
    }
}
