package com.example.pace_ometer.ui.activerun

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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
import com.example.pace_ometer.data.settings.UnitSystem
import com.example.pace_ometer.service.RunPhase
import com.example.pace_ometer.service.RunState
import com.example.pace_ometer.util.formatDistanceMeters
import com.example.pace_ometer.util.formatDurationMs
import com.example.pace_ometer.util.formatElevationMeters
import com.example.pace_ometer.util.formatPaceSecPerKm
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ActiveRunScreen(
    onFinished: () -> Unit,
    viewModel: ActiveRunViewModel = viewModel()
) {
    val state by viewModel.runState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val unitSystem = settings.unitSystem

    LaunchedEffect(Unit) {
        if (state.phase == RunPhase.IDLE) viewModel.start()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(formatDistanceMeters(state.distanceMeters, unitSystem), style = MaterialTheme.typography.displayMedium)
            Text(formatDurationMs(state.movingDurationMs), style = MaterialTheme.typography.headlineSmall)

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

            HorizontalDivider()

            MetricsGrid(state = state, unitSystem = unitSystem)
        }
    }

    if (state.phase == RunPhase.STOPPED && state.runId != null) {
        val runId = state.runId!!
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Save this run?") },
            text = {
                Text(
                    "${formatDistanceMeters(state.distanceMeters, unitSystem)} in " +
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

/**
 * Shows every trackable metric during a run, regardless of which ones are toggled on for TTS
 * announcements -- the toggles in Settings only control what gets spoken aloud.
 */
@Composable
private fun MetricsGrid(state: RunState, unitSystem: UnitSystem) {
    val clockTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

    val elevationChangeLabel = state.elevationChangeLastSegmentMeters?.let { change ->
        val sign = if (change >= 0) "+" else "-"
        "$sign${formatElevationMeters(abs(change), unitSystem)}"
    } ?: "--"

    val metrics = listOf(
        "Segment pace" to formatPaceSecPerKm(state.segmentPaceSecPerKm, unitSystem),
        "Split (projected) pace" to formatPaceSecPerKm(state.currentPaceSecPerKm, unitSystem),
        "Elevation" to (state.elevationMeters?.let { formatElevationMeters(it, unitSystem) } ?: "--"),
        "Last segment elevation Δ" to elevationChangeLabel,
        "Heart rate" to (state.heartRateBpm?.let { "$it bpm" } ?: "--"),
        "Cadence" to (state.cadenceSpm?.let { "$it spm" } ?: "--"),
        "Calories" to "${state.caloriesBurned.roundToInt()} kcal",
        "Clock time" to clockTime
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        metrics.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { (label, value) ->
                    MetricCell(label = label, value = value, modifier = Modifier.weight(1f))
                }
                if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetricCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
