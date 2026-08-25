package com.example.pace_ometer.sensors.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/**
 * Centralizes the connect/discoverServices/enable-notifications/characteristic-changed
 * boilerplate shared by every BLE GATT peripheral this app talks to, so each concrete
 * [BleSensor] only needs to supply its service/characteristic UUIDs and a byte parser.
 */
class GattClientManager(private val context: Context) {

    private var gatt: BluetoothGatt? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    /**
     * Null until service discovery completes, then true/false depending on whether the connected
     * device's GATT server actually exposes the requested service -- e.g. some smartwatches (like
     * Samsung Galaxy Watch models) connect fine but expose no standard BLE Heart Rate Service at
     * all, sharing heart rate only through their own companion app instead.
     */
    private val _serviceAvailable = MutableStateFlow<Boolean?>(null)
    val serviceAvailable: StateFlow<Boolean?> = _serviceAvailable

    @SuppressLint("MissingPermission")
    fun connect(
        device: BluetoothDevice,
        serviceUuid: UUID,
        characteristicUuid: UUID,
        onValueChanged: (ByteArray) -> Unit
    ) {
        _serviceAvailable.value = null
        gatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        _isConnected.value = true
                        g.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> _isConnected.value = false
                }
            }

            override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                val characteristic = g.getService(serviceUuid)?.getCharacteristic(characteristicUuid)
                if (characteristic == null) {
                    _serviceAvailable.value = false
                    return
                }
                _serviceAvailable.value = true
                g.setCharacteristicNotification(characteristic, true)
                val cccd = characteristic.getDescriptor(BleServiceUuids.CLIENT_CHARACTERISTIC_CONFIG) ?: return
                if (Build.VERSION.SDK_INT >= 33) {
                    g.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                } else {
                    @Suppress("DEPRECATION")
                    cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    @Suppress("DEPRECATION")
                    g.writeDescriptor(cccd)
                }
            }

            @Suppress("DEPRECATION")
            override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                if (Build.VERSION.SDK_INT < 33) onValueChanged(characteristic.value)
            }

            override fun onCharacteristicChanged(
                g: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray
            ) {
                onValueChanged(value)
            }
        }, BluetoothDevice.TRANSPORT_LE)
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        _isConnected.value = false
        _serviceAvailable.value = null
    }
}
