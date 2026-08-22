package com.example.pace_ometer.ui.activerun

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pace_ometer.service.RunPhase
import com.example.pace_ometer.util.formatDistanceMeters
import com.example.pace_ometer.util.formatDurationMs
import com.example.pace_ometer.util.formatPaceSecPerKm

@Composable
fun ActiveRunScreen(
    onFinished: () -> Unit,
    viewModel: ActiveRunViewModel = viewModel()
) {
    val state by viewModel.runState.collectAsState()

    LaunchedEffect(Unit) {
        if (state.phase == RunPhase.IDLE) viewModel.start()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(formatDistanceMeters(state.distanceMeters), style = MaterialTheme.typography.displayMedium)
            Text(formatDurationMs(state.movingDurationMs), style = MaterialTheme.typography.headlineSmall)
            Text(
                "Pace: ${formatPaceSecPerKm(state.currentPaceSecPerKm)}",
                style = MaterialTheme.typography.bodyLarge
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                when (state.phase) {
                    RunPhase.RUNNING -> OutlinedButton(onClick = { viewModel.pause() }) { Text("Pause") }
                    RunPhase.PAUSED -> OutlinedButton(onClick = { viewModel.resume() }) { Text("Resume") }
                    else -> {}
                }
                if (state.phase == RunPhase.RUNNING || state.phase == RunPhase.PAUSED) {
                    Button(onClick = { viewModel.stop() }) { Text("Stop") }
                }
            }
        }
    }

    if (state.phase == RunPhase.STOPPED && state.runId != null) {
        val runId = state.runId!!
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Save this run?") },
            text = {
                Text(
                    "${formatDistanceMeters(state.distanceMeters)} in " +
                        formatDurationMs(state.movingDurationMs)
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.saveRun(runId, onFinished) }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.discardRun(runId, onFinished) }) { Text("Discard") }
            }
        )
    }
}
