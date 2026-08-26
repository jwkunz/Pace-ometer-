package com.example.pace_ometer.ui.settings

import android.Manifest
import android.bluetooth.BluetoothManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pace_ometer.data.settings.UnitSystem
import com.example.pace_ometer.data.settings.UserSettings
import com.example.pace_ometer.sensors.ble.BleDeviceScanner
import com.example.pace_ometer.sensors.ble.DiscoveredAthleticDevice
import com.example.pace_ometer.sensors.ble.HeartRateGattSensor
import com.example.pace_ometer.sensors.health.HealthConnectHeartRateSource
import com.example.pace_ometer.ui.common.permissions.isPermissionGranted
import com.example.pace_ometer.util.cmToDisplayHeight
import com.example.pace_ometer.util.displayHeightToCm
import com.example.pace_ometer.util.displayWeightToKg
import com.example.pace_ometer.util.kgToDisplayWeight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onResetComplete: () -> Unit,
    onOpenEquipment: () -> Unit,
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
            AthleticSensorSection(
                deviceAddress = settings.heartRateDeviceAddress,
                onDeviceSelected = { viewModel.updateHeartRateDeviceAddress(it) },
                onForget = { viewModel.updateHeartRateDeviceAddress(null) }
            )
            HealthConnectHeartRateSection(
                enabled = settings.useHealthConnectHeartRate,
                bleDeviceConnected = settings.heartRateDeviceAddress != null,
                onEnabledChanged = { viewModel.updateUseHealthConnectHeartRate(it) }
            )

            Text("Voice announcements", style = MaterialTheme.typography.titleSmall)
            VoiceAnnouncementSection(settings = settings, viewModel = viewModel)

            Text("Seasons", style = MaterialTheme.typography.titleSmall)
            SeasonSection(viewModel = viewModel)

            Text("Equipment", style = MaterialTheme.typography.titleSmall)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Track cumulative distance on gear like running shoes and retire them when worn out.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedButton(onClick = onOpenEquipment) { Text("Manage equipment") }
            }

            Text("Reset", style = MaterialTheme.typography.titleSmall)
            ResetDataSection(viewModel = viewModel, onResetComplete = onResetComplete)
        }
    }
}

@Composable
private fun ResetDataSection(viewModel: SettingsViewModel, onResetComplete: () -> Unit) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Deletes every saved run, personal record, season, and setting on this device, " +
                "and restarts the app as if freshly installed.",
            style = MaterialTheme.typography.bodySmall
        )
        Button(
            onClick = { showConfirmDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Reset all app data") }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Reset all app data?") },
            text = { Text("This permanently deletes all runs, records, seasons, and settings. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    viewModel.resetAllData(onResetComplete)
                }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { showConfirmDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SeasonSection(viewModel: SettingsViewModel) {
    val seasons by viewModel.seasons.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Seasons let personal records be tracked separately per time period, in addition to all-time.",
            style = MaterialTheme.typography.bodySmall
        )
        seasons.forEach { season ->
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "${season.name} (from ${
                        java.text.DateFormat.getDateInstance().format(java.util.Date(season.startEpochMs))
                    })"
                )
                TextButton(onClick = { viewModel.deleteSeason(season) }) { Text("Delete") }
            }
        }
        OutlinedButton(onClick = { showAddDialog = true }) { Text("Add season") }
    }

    if (showAddDialog) {
        AddSeasonDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, startEpochMs ->
                viewModel.addSeason(name, startEpochMs)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AddSeasonDialog(onDismiss: () -> Unit, onAdd: (String, Long) -> Unit) {
    var name by remember { mutableStateOf("") }
    var startEpochDay by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add season") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                OutlinedButton(onClick = { showDatePicker = true }) {
                    Text(
                        startEpochDay?.let {
                            java.text.DateFormat.getDateInstance().format(java.util.Date(it * 86_400_000L))
                        } ?: "Select start date"
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { startEpochDay?.let { onAdd(name.ifBlank { "Season" }, it * 86_400_000L) } },
                enabled = startEpochDay != null
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showDatePicker) {
        val datePickerState = androidx.compose.material3.rememberDatePickerState()
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { startEpochDay = it / 86_400_000L }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { androidx.compose.material3.DatePicker(state = datePickerState) }
    }
}

private data class AnnouncementToggle(
    val label: String,
    val isEnabled: (UserSettings) -> Boolean,
    val setEnabled: (UserSettings, Boolean) -> UserSettings
)

private val announcementToggles = listOf(
    AnnouncementToggle("Distance", { it.announceDistance }, { s, v -> s.copy(announceDistance = v) }),
    AnnouncementToggle("Elapsed time", { it.announceElapsedTime }, { s, v -> s.copy(announceElapsedTime = v) }),
    AnnouncementToggle("Current segment pace", { it.announceSegmentPace }, { s, v -> s.copy(announceSegmentPace = v) }),
    AnnouncementToggle("Split (projected) pace", { it.announceSplitPace }, { s, v -> s.copy(announceSplitPace = v) }),
    AnnouncementToggle("Elevation", { it.announceElevation }, { s, v -> s.copy(announceElevation = v) }),
    AnnouncementToggle(
        "Last segment elevation change",
        { it.announceElevationChangeLastSegment },
        { s, v -> s.copy(announceElevationChangeLastSegment = v) }
    ),
    AnnouncementToggle("Heart rate", { it.announceHeartRate }, { s, v -> s.copy(announceHeartRate = v) }),
    AnnouncementToggle(
        "Heart rate zone",
        { it.announceHeartRateZone },
        { s, v -> s.copy(announceHeartRateZone = v) }
    ),
    AnnouncementToggle("Cadence", { it.announceCadence }, { s, v -> s.copy(announceCadence = v) }),
    AnnouncementToggle("Calories burned", { it.announceCalories }, { s, v -> s.copy(announceCalories = v) }),
    AnnouncementToggle("Current clock time", { it.announceClockTime }, { s, v -> s.copy(announceClockTime = v) })
)

@Composable
private fun VoiceAnnouncementSection(settings: UserSettings, viewModel: SettingsViewModel) {
    var intervalText by remember(settings.announcementIntervalValue) {
        mutableStateOf(settings.announcementIntervalValue.toString())
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val intervalUnitLabel = if (settings.unitSystem == UnitSystem.IMPERIAL) "mi" else "km"
        OutlinedTextField(
            value = intervalText,
            onValueChange = {
                intervalText = it
                it.toFloatOrNull()?.let { value ->
                    viewModel.updateAnnouncementInterval(value, settings.unitSystem)
                }
            },
            label = { Text("Announce every ($intervalUnitLabel)") },
            modifier = Modifier.fillMaxWidth()
        )

        announcementToggles.forEach { toggle ->
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(toggle.label, style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = toggle.isEnabled(settings),
                    onCheckedChange = { checked ->
                        viewModel.updateAnnouncementToggle { toggle.setEnabled(it, checked) }
                    }
                )
            }
        }
    }
}

@Composable
private fun AthleticSensorSection(
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
        if (deviceAddress != null) {
            Text("Heart rate monitor: $deviceAddress", style = MaterialTheme.typography.bodyMedium)
            LiveHeartRateStatus(deviceAddress = deviceAddress)
        } else {
            Text("No heart rate monitor connected", style = MaterialTheme.typography.bodyMedium)
        }
        OutlinedButton(onClick = {
            val alreadyGranted = bluetoothPermissions.all { isPermissionGranted(context, it) }
            if (alreadyGranted) showScanDialog = true else permissionLauncher.launch(bluetoothPermissions)
        }) { Text("Scan for athletic sensors") }

        if (deviceAddress != null) {
            OutlinedButton(onClick = onForget) { Text("Forget device") }
        }
    }

    if (showScanDialog) {
        AthleticSensorScanDialog(
            onDismiss = { showScanDialog = false },
            onDeviceSelected = {
                onDeviceSelected(it)
                showScanDialog = false
            }
        )
    }
}

private const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"

/**
 * Fallback heart-rate source for wearables (e.g. Samsung Galaxy Watch via Samsung Health) that
 * don't expose a plain BLE GATT Heart Rate Service to third-party apps at all -- reads whatever
 * Health Connect has synced instead of scanning/connecting over Bluetooth directly.
 */
@Composable
private fun HealthConnectHeartRateSection(
    enabled: Boolean,
    bleDeviceConnected: Boolean,
    onEnabledChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val available = remember { HealthConnectHeartRateSource.isAvailable(context) }
    var hasPermission by remember { mutableStateOf<Boolean?>(null) }
    var lastCheckedBpm by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        HealthConnectHeartRateSource.requestPermissionActivityContract()
    ) { granted -> hasPermission = HealthConnectHeartRateSource.READ_HEART_RATE_PERMISSION in granted }

    LaunchedEffect(available) {
        if (available) hasPermission = HealthConnectHeartRateSource.hasPermission(context)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "For wearables that don't expose Bluetooth heart-rate directly (e.g. Samsung Galaxy " +
                "Watch via Samsung Health), Pace-ometer can read whatever Health Connect has " +
                "synced instead. Not real-time -- checked periodically during a run.",
            style = MaterialTheme.typography.bodySmall
        )
        when {
            !available -> OutlinedButton(onClick = {
                val uri = android.net.Uri.parse("market://details?id=$HEALTH_CONNECT_PACKAGE")
                context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
            }) { Text("Install Health Connect") }

            hasPermission == false -> OutlinedButton(onClick = {
                permissionLauncher.launch(setOf(HealthConnectHeartRateSource.READ_HEART_RATE_PERMISSION))
            }) { Text("Grant Health Connect access") }

            hasPermission == true -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Use Health Connect for heart rate" +
                            if (bleDeviceConnected) " (unused while a BLE monitor is paired)" else ""
                    )
                    Switch(checked = enabled, onCheckedChange = onEnabledChanged, enabled = !bleDeviceConnected)
                }
                if (enabled && !bleDeviceConnected) {
                    OutlinedButton(onClick = {
                        scope.launch {
                            lastCheckedBpm = HealthConnectHeartRateSource.readLatestBpm(context)
                                ?.let { "$it bpm" } ?: "No recent reading found in Health Connect"
                        }
                    }) { Text("Check now") }
                    lastCheckedBpm?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}

/** Connects just long enough to show a live BPM reading, confirming the paired monitor actually works. */
@android.annotation.SuppressLint("MissingPermission")
@Composable
private fun LiveHeartRateStatus(deviceAddress: String) {
    val context = LocalContext.current
    var bpm by remember(deviceAddress) { mutableStateOf<Int?>(null) }
    var connected by remember(deviceAddress) { mutableStateOf(false) }
    var serviceAvailable by remember(deviceAddress) { mutableStateOf<Boolean?>(null) }

    DisposableEffect(deviceAddress) {
        val hasBluetoothConnect = isPermissionGranted(context, Manifest.permission.BLUETOOTH_CONNECT)
        if (!hasBluetoothConnect) return@DisposableEffect onDispose {}

        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
        val device = adapter?.let { runCatching { it.getRemoteDevice(deviceAddress) }.getOrNull() }
        val sensor = HeartRateGattSensor(context)
        val scope = CoroutineScope(SupervisorJob())

        if (device != null) {
            sensor.connect(device)
            scope.launch { sensor.isConnected.collect { connected = it } }
            scope.launch { sensor.serviceAvailable.collect { serviceAvailable = it } }
            scope.launch { sensor.readings.collect { bpm = it.bpm } }
        }

        onDispose {
            sensor.disconnect()
            scope.cancel()
        }
    }

    Text(
        when {
            bpm != null -> "Current: $bpm bpm"
            serviceAvailable == false ->
                "This device doesn't expose standard Bluetooth heart-rate data — some smartwatches " +
                    "(e.g. Samsung Galaxy Watch) only share heart rate through their own companion app."
            connected -> "Connected — waiting for a reading…"
            else -> "Connecting…"
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary
    )
}

@android.annotation.SuppressLint("MissingPermission")
@Composable
private fun AthleticSensorScanDialog(onDismiss: () -> Unit, onDeviceSelected: (String) -> Unit) {
    val context = LocalContext.current
    var devices by remember { mutableStateOf(listOf<DiscoveredAthleticDevice>()) }

    LaunchedEffect(Unit) {
        val scanner = BleDeviceScanner(context)
        scanner.scanForServices().collect { found ->
            if (devices.none { it.device.address == found.device.address }) {
                devices = devices + found
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Nearby athletic sensors") },
        text = {
            if (devices.isEmpty()) {
                Text("Searching… make sure your sensor is on and nearby.")
            } else {
                LazyColumn {
                    items(devices, key = { it.device.address }) { found ->
                        // Not every device advertises its GATT services (some wearables only
                        // reveal them after connecting), so any named nearby device is
                        // selectable -- the label below is just a hint, not a gate.
                        val typeLabel = found.serviceLabels.joinToString(", ")
                            .ifEmpty { "Unknown type — tap to try connecting" }
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Button(
                                onClick = { onDeviceSelected(found.device.address) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(found.device.name ?: found.device.address)
                            }
                            Text(typeLabel, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    )
}
