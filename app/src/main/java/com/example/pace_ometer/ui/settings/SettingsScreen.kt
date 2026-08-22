package com.example.pace_ometer.ui.settings

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pace_ometer.data.settings.UnitSystem
import com.example.pace_ometer.sensors.ble.BleDeviceScanner
import com.example.pace_ometer.sensors.ble.BleServiceUuids
import com.example.pace_ometer.ui.common.permissions.isPermissionGranted
import com.example.pace_ometer.util.cmToDisplayHeight
import com.example.pace_ometer.util.displayHeightToCm
import com.example.pace_ometer.util.displayWeightToKg
import com.example.pace_ometer.util.kgToDisplayWeight

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()

    var weightText by remember(settings.bodyWeightKg, settings.unitSystem) {
        mutableStateOf("%.1f".format(kgToDisplayWeight(settings.bodyWeightKg, settings.unitSystem)))
    }
    var heightText by remember(settings.heightCm, settings.unitSystem) {
        mutableStateOf(
            settings.heightCm?.let { "%.1f".format(cmToDisplayHeight(it, settings.unitSystem)) } ?: ""
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Unit system", style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow {
                UnitSystem.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index, UnitSystem.entries.size),
                        selected = settings.unitSystem == option,
                        onClick = { viewModel.updateUnitSystem(option) }
                    ) { Text(if (option == UnitSystem.METRIC) "Metric" else "Imperial") }
                }
            }

            val weightUnitLabel = if (settings.unitSystem == UnitSystem.IMPERIAL) "lb" else "kg"
            OutlinedTextField(
                value = weightText,
                onValueChange = {
                    weightText = it
                    it.toFloatOrNull()?.let { value ->
                        viewModel.updateBodyWeightKg(displayWeightToKg(value, settings.unitSystem))
                    }
                },
                label = { Text("Body weight ($weightUnitLabel)") },
                modifier = Modifier.fillMaxWidth()
            )

            val heightUnitLabel = if (settings.unitSystem == UnitSystem.IMPERIAL) "in" else "cm"
            OutlinedTextField(
                value = heightText,
                onValueChange = {
                    heightText = it
                    viewModel.updateHeightCm(it.toFloatOrNull()?.let { value -> displayHeightToCm(value, settings.unitSystem) })
                },
                label = { Text("Height — optional ($heightUnitLabel)") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Sensors", style = MaterialTheme.typography.titleSmall)
            HeartRateSensorSection(
                deviceAddress = settings.heartRateDeviceAddress,
                onDeviceSelected = { viewModel.updateHeartRateDeviceAddress(it) },
                onForget = { viewModel.updateHeartRateDeviceAddress(null) }
            )
        }
    }
}

@Composable
private fun HeartRateSensorSection(
    deviceAddress: String?,
    onDeviceSelected: (String) -> Unit,
    onForget: () -> Unit
) {
    val context = LocalContext.current
    var showScanDialog by remember { mutableStateOf(false) }

    val bluetoothPermissions = if (Build.VERSION.SDK_INT >= 31) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else emptyArray()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) showScanDialog = true
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            deviceAddress?.let { "Heart rate monitor: $it" } ?: "No heart rate monitor connected",
            style = MaterialTheme.typography.bodyMedium
        )
        OutlinedButton(onClick = {
            val alreadyGranted = bluetoothPermissions.all { isPermissionGranted(context, it) }
            if (alreadyGranted) showScanDialog = true else permissionLauncher.launch(bluetoothPermissions)
        }) { Text("Scan for heart rate monitor") }

        if (deviceAddress != null) {
            OutlinedButton(onClick = onForget) { Text("Forget device") }
        }
    }

    if (showScanDialog) {
        HeartRateScanDialog(
            onDismiss = { showScanDialog = false },
            onDeviceSelected = {
                onDeviceSelected(it)
                showScanDialog = false
            }
        )
    }
}

@android.annotation.SuppressLint("MissingPermission")
@Composable
private fun HeartRateScanDialog(onDismiss: () -> Unit, onDeviceSelected: (String) -> Unit) {
    val context = LocalContext.current
    var devices by remember { mutableStateOf(listOf<BluetoothDevice>()) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        val scanner = BleDeviceScanner(context)
        scanner.scanForService(BleServiceUuids.HEART_RATE_SERVICE).collect { device ->
            if (devices.none { it.address == device.address }) {
                devices = devices + device
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Nearby heart rate monitors") },
        text = {
            if (devices.isEmpty()) {
                Text("Searching… make sure your monitor is on and nearby.")
            } else {
                LazyColumn {
                    items(devices, key = { it.address }) { device ->
                        Button(
                            onClick = { onDeviceSelected(device.address) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(device.name ?: device.address)
                        }
                    }
                }
            }
        }
    )
}
