package com.example.pace_ometer.sensors.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant

/**
 * Reads recent heart-rate samples from Health Connect -- the officially supported way to get
 * heart-rate data from wearables (e.g. a Samsung Galaxy Watch synced via Samsung Health) that
 * don't expose a plain BLE GATT Heart Rate Service to third-party apps at all. Health Connect is
 * a synced data store, not a live stream, so this is polled periodically during a run rather
 * than subscribed to like [com.example.pace_ometer.sensors.ble.BleSensor].
 */
object HealthConnectHeartRateSource {

    val READ_HEART_RATE_PERMISSION: String = HealthPermission.getReadPermission(HeartRateRecord::class)

    fun requestPermissionActivityContract() = PermissionController.createRequestPermissionResultContract()

    /** True once Health Connect is installed and up to date enough to use. */
    fun isAvailable(context: Context): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    suspend fun hasPermission(context: Context): Boolean {
        if (!isAvailable(context)) return false
        val granted = HealthConnectClient.getOrCreate(context).permissionController.getGrantedPermissions()
        return READ_HEART_RATE_PERMISSION in granted
    }

    /** The most recent heart-rate sample within [withinLast], or null if none / unavailable. */
    suspend fun readLatestBpm(context: Context, withinLast: Duration = Duration.ofMinutes(2)): Int? {
        if (!isAvailable(context)) return null
        val client = HealthConnectClient.getOrCreate(context)
        val now = Instant.now()
        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(now.minus(withinLast), now)
            )
        )
        return response.records
            .flatMap { it.samples }
            .maxByOrNull { it.time }
            ?.beatsPerMinute
            ?.toInt()
    }
}
