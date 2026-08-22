package com.example.pace_ometer.ui.legal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LegalScreen(onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Legal") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Copyright", style = MaterialTheme.typography.titleMedium)
                Text("© 2026 Numerius Engineering LLC. All rights reserved.", style = MaterialTheme.typography.bodyMedium)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Privacy notice", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Pace-ometer stores all run data, sensor readings, and settings exclusively on this " +
                        "device in a local database. Nothing is transmitted to or stored on any remote " +
                        "server. The only way your data leaves this device is if you explicitly export it " +
                        "to a JSON file. Bluetooth and GPS are used solely to record your run locally.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
