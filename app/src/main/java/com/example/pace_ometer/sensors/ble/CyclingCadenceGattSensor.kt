package com.example.pace_ometer.sensors.ble

import android.bluetooth.BluetoothDevice
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/**
 * Bluetooth SIG Cycling Speed and Cadence Service (0x1816) / CSC Measurement (0x2A5B).
 *
 * No CSC-compatible sensor is available to validate this against real hardware yet, so this is a
 * best-effort implementation of the spec's byte layout, wired the same way as its running
 * counterpart, [CadenceGattSensor], so a future sensor can be paired without further
 * rearchitecting. Unlike RSC's instantaneous cadence field, CSC only reports cumulative crank
 * revolutions plus a 1/1024s event timestamp -- crank RPM has to be derived from the delta
 * against the previous notification, so the first reading after a (re)connect can't produce a
 * cadence yet (nothing to diff against).
 * TODO: validate field offsets/scaling against a real CSC-compatible sensor once one is available.
 */
class CyclingCadenceGattSensor(context: Context) : BleSensor<CadenceReading> {

    private val gattClient = GattClientManager(context)
    private val _readings = MutableSharedFlow<CadenceReading>(replay = 0, extraBufferCapacity = 4)

    private var previousCrankState: CrankState? = null

    override val serviceUuid: UUID = BleServiceUuids.CYCLING_SPEED_AND_CADENCE_SERVICE
    override val isConnected: StateFlow<Boolean> = gattClient.isConnected
    override val readings: Flow<CadenceReading> = _readings

    override fun connect(device: BluetoothDevice) {
        previousCrankState = null
        gattClient.connect(
            device = device,
            serviceUuid = BleServiceUuids.CYCLING_SPEED_AND_CADENCE_SERVICE,
            characteristicUuid = BleServiceUuids.CSC_MEASUREMENT,
            onValueChanged = { bytes ->
                val (nextState, reading) = parseCscMeasurement(bytes, previousCrankState)
                previousCrankState = nextState
                reading?.let { _readings.tryEmit(it.copy(timestampMs = System.currentTimeMillis())) }
            }
        )
    }

    override fun disconnect() {
        gattClient.disconnect()
        previousCrankState = null
    }

    companion object {
        data class CrankState(val cumulativeRevolutions: Int, val eventTime1024ths: Int)

        /**
         * Derives crank RPM from the delta against [previous]. Returns a null reading (but a
         * non-null next state, when crank data was present) when the sensor reports no crank
         * data (e.g. a wheel-speed-only sensor), on the first notification after a (re)connect
         * (nothing to diff against yet), or when the event-time delta is zero. Both
         * cumulative-revolutions and event-time fields are 16-bit and wrap around, so deltas are
         * masked to 16 bits rather than computed as a plain subtraction.
         */
        fun parseCscMeasurement(data: ByteArray, previous: CrankState?): Pair<CrankState?, CadenceReading?> {
            if (data.isEmpty()) return previous to null
            val flags = data[0].toInt()
            val wheelDataPresent = flags and 0x01 != 0
            val crankDataPresent = flags and 0x02 != 0
            if (!crankDataPresent) return previous to null

            // Wheel data (if present) is a 4-byte cumulative count + 2-byte event time, ahead of
            // the crank fields this app actually needs.
            val crankOffset = if (wheelDataPresent) 7 else 1
            if (data.size < crankOffset + 4) return previous to null

            val cumulativeCrankRevolutions =
                ((data[crankOffset + 1].toInt() and 0xFF) shl 8) or (data[crankOffset].toInt() and 0xFF)
            val lastCrankEventTime1024ths =
                ((data[crankOffset + 3].toInt() and 0xFF) shl 8) or (data[crankOffset + 2].toInt() and 0xFF)
            val nextState = CrankState(cumulativeCrankRevolutions, lastCrankEventTime1024ths)

            if (previous == null) return nextState to null

            val revolutionDelta = (cumulativeCrankRevolutions - previous.cumulativeRevolutions) and 0xFFFF
            val eventTimeDelta1024ths = (lastCrankEventTime1024ths - previous.eventTime1024ths) and 0xFFFF
            if (eventTimeDelta1024ths == 0) return nextState to null

            val cadenceRpm = (revolutionDelta * 1024.0 * 60.0 / eventTimeDelta1024ths).toInt()
            return nextState to CadenceReading(cadenceSpm = cadenceRpm, speedMps = null, timestampMs = 0L)
        }
    }
}
