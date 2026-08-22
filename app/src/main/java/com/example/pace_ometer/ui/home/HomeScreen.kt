package com.example.pace_ometer.ui.home

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.pace_ometer.ui.common.permissions.rememberPermissionGrantedState

@Composable
fun HomeScreen(
    onStartRun: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenRecords: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenLegal: () -> Unit
) {
    val context = LocalContext.current
    var showBackgroundLocationRationale by remember { mutableStateOf(false) }

    val notificationsGranted = if (Build.VERSION.SDK_INT >= 33) {
        rememberPermissionGrantedState(Manifest.permission.POST_NOTIFICATIONS).value
    } else true
    val fineLocationGranted = rememberPermissionGrantedState(Manifest.permission.ACCESS_FINE_LOCATION).value
    val backgroundLocationGranted = if (Build.VERSION.SDK_INT >= 29) {
        rememberPermissionGrantedState(Manifest.permission.ACCESS_BACKGROUND_LOCATION).value
    } else true

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val fineLocationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val backgroundLocationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) showBackgroundLocationRationale = true
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Pace-ometer") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Ready to run?", style = MaterialTheme.typography.headlineSmall)

            if (!notificationsGranted) {
                PermissionPrompt(
                    message = "Allow notifications so you can see live run status.",
                    buttonLabel = "Allow notifications"
                ) { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
            } else if (!fineLocationGranted) {
                PermissionPrompt(
                    message = "Location access is required to track your run's distance and route.",
                    buttonLabel = "Allow location"
                ) { fineLocationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
            } else if (!backgroundLocationGranted) {
                PermissionPrompt(
                    message = "Pace-ometer needs \"Allow all the time\" location access to keep tracking " +
                        "accurately when your screen is off or the phone is in your pocket. Without it, " +
                        "your run will not be tracked correctly.",
                    buttonLabel = "Allow background location"
                ) {
                    if (Build.VERSION.SDK_INT >= 29) {
                        backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }
                }
            } else {
                Button(onClick = onStartRun, modifier = Modifier.fillMaxWidth()) {
                    Text("Start Run")
                }
            }

            OutlinedButton(onClick = onOpenHistory, modifier = Modifier.fillMaxWidth()) { Text("Run History") }
            OutlinedButton(onClick = onOpenRecords, modifier = Modifier.fillMaxWidth()) { Text("Personal Records") }
            OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) { Text("Settings") }
            OutlinedButton(onClick = onOpenHelp, modifier = Modifier.fillMaxWidth()) { Text("Help") }
            OutlinedButton(onClick = onOpenLegal, modifier = Modifier.fillMaxWidth()) { Text("Legal") }
        }
    }

    if (showBackgroundLocationRationale) {
        AlertDialog(
            onDismissRequest = { showBackgroundLocationRationale = false },
            confirmButton = {
                TextButton(onClick = {
                    showBackgroundLocationRationale = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) { Text("Open Settings") }
            },
            dismissButton = { TextButton(onClick = { showBackgroundLocationRationale = false }) { Text("Cancel") } },
            title = { Text("Background location required") },
            text = { Text("Please allow \"Allow all the time\" location access in Settings for accurate run tracking.") }
        )
    }
}

@Composable
private fun PermissionPrompt(message: String, buttonLabel: String, onRequest: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(message, style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onRequest) { Text(buttonLabel) }
    }
}
