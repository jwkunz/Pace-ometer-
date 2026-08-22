package com.example.pace_ometer.sensors.motion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.sqrt

/**
 * Emits a timestamp (ms) each time a step is detected, for use as the accelerometer-based
 * fallback signal during GPS gaps. Prefers the hardware TYPE_STEP_DETECTOR sensor (low-power,
 * vendor-tuned); falls back to simple peak detection on the raw accelerometer magnitude when
 * a device has no dedicated step sensor.
 */
class StepDetector(private val sensorManager: SensorManager) {

    fun steps(): Flow<Long> {
        val hardwareStepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        return if (hardwareStepSensor != null) hardwareSteps(hardwareStepSensor) else accelerometerPeakSteps()
    }

    private fun hardwareSteps(sensor: Sensor): Flow<Long> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(System.currentTimeMillis())
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose { sensorManager.unregisterListener(listener) }
    }

    private fun accelerometerPeakSteps(): Flow<Long> = callbackFlow {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer == null) {
            close()
            return@callbackFlow
        }

        var smoothedMagnitude = SensorManager.GRAVITY_EARTH
        var lastStepTimestampMs = 0L
        val minStepIntervalMs = 250L
        val peakThreshold = 1.2f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val (x, y, z) = Triple(event.values[0], event.values[1], event.values[2])
                val magnitude = sqrt(x * x + y * y + z * z)
                smoothedMagnitude = smoothedMagnitude * 0.9f + magnitude * 0.1f
                val delta = magnitude - smoothedMagnitude

                val now = System.currentTimeMillis()
                if (delta > peakThreshold && now - lastStepTimestampMs > minStepIntervalMs) {
                    lastStepTimestampMs = now
                    trySend(now)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
