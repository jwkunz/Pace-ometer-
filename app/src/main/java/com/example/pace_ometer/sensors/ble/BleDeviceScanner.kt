package com.example.pace_ometer.sensors.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.UUID

/** One nearby BLE peripheral found while scanning, labeled by whichever athletic services it advertises. */
data class DiscoveredAthleticDevice(val device: BluetoothDevice, val serviceLabels: List<String>)

/**
 * Scans for nearby, named BLE peripherals, labeling each by whichever athletic services (if any)
 * it advertises. Deliberately unfiltered by service UUID -- many wearables (e.g. Samsung Galaxy
 * Watch models) don't advertise their GATT services at all, only revealing them once connected
 * via service discovery, so filtering scan results by advertised UUID would hide devices that
 * work fine once connected. [serviceUuids] only controls the informational label, not inclusion.
 */
class BleDeviceScanner(private val context: Context) {

    @SuppressLint("MissingPermission")
    fun scanForServices(serviceUuids: List<UUID> = BleServiceUuids.ATHLETIC_SERVICES): Flow<DiscoveredAthleticDevice> =
        callbackFlow {
            val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
            val scanner = bluetoothManager?.adapter?.bluetoothLeScanner
            if (scanner == null) {
                close()
                return@callbackFlow
            }

            val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()

            val callback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    val name = result.scanRecord?.deviceName ?: result.device.name ?: return
                    if (name.isBlank()) return
                    val advertised = result.scanRecord?.serviceUuids.orEmpty().map { it.uuid }
                    val labels = serviceUuids.filter { it in advertised }.map { BleServiceUuids.label(it) }
                    trySend(DiscoveredAthleticDevice(result.device, labels))
                }
            }

            scanner.startScan(emptyList(), settings, callback)
            awaitClose { scanner.stopScan(callback) }
        }.distinctUntilChanged { old, new -> old.device.address == new.device.address }
}
