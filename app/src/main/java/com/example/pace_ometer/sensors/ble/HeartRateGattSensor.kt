package com.example.pace_ometer.sensors.ble

import android.bluetooth.BluetoothDevice
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/**
 * Bluetooth SIG Heart Rate Service (0x180D) / Heart Rate Measurement characteristic (0x2A37).
 * Per spec, the measurement's first byte is a flags field whose bit 0 selects whether the BPM
 * value is UINT8 (byte 1) or UINT16 little-endian (bytes 1-2); RR-interval and energy-expended
 * fields (also flag-gated) aren't needed here since calories are computed from pace, not HR.
 */
class HeartRateGattSensor(context: Context) : BleSensor<HeartRateReading> {

    private val gattClient = GattClientManager(context)
    private val _readings = MutableSharedFlow<HeartRateReading>(replay = 0, extraBufferCapacity = 4)

    override val serviceUuid: UUID = BleServiceUuids.HEART_RATE_SERVICE
    override val isConnected: StateFlow<Boolean> = gattClient.isConnected
    override val readings: Flow<HeartRateReading> = _readings

    /** Null while connecting/discovering; true/false once we know whether the device has 0x180D. */
    val serviceAvailable: StateFlow<Boolean?> = gattClient.serviceAvailable

    override fun connect(device: BluetoothDevice) {
        gattClient.connect(
            device = device,
            serviceUuid = BleServiceUuids.HEART_RATE_SERVICE,
            characteristicUuid = BleServiceUuids.HEART_RATE_MEASUREMENT,
            onValueChanged = { bytes ->
                parseHeartRate(bytes)?.let { bpm ->
                    _readings.tryEmit(HeartRateReading(bpm = bpm, timestampMs = System.currentTimeMillis()))
                }
            }
        )
    }

    override fun disconnect() = gattClient.disconnect()

    companion object {
        fun parseHeartRate(data: ByteArray): Int? {
            if (data.isEmpty()) return null
            val flags = data[0].toInt()
            val is16Bit = (flags and 0x01) != 0
            return if (is16Bit) {
                if (data.size < 3) return null
                ((data[2].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
            } else {
                if (data.size < 2) return null
                data[1].toInt() and 0xFF
            }
        }
    }
}
