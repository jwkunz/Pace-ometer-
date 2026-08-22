package com.example.pace_ometer.ui.history

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pace_ometer.R
import com.example.pace_ometer.util.formatDistanceMeters
import com.example.pace_ometer.util.formatDurationMs
import com.example.pace_ometer.util.formatPaceSecPerKm
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onOpenRun: (Long) -> Unit,
    viewModel: HistoryViewModel = viewModel()
) {
    val runs by viewModel.runs.collectAsState()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportAllRunsTo(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Run History") },
                actions = {
                    if (runs.isNotEmpty()) {
                        IconButton(onClick = {
                            exportLauncher.launch("paceometer_export_${System.currentTimeMillis()}.json")
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_favorite),
                                contentDescription = "Export all runs"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (runs.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)
            ) {
                Text("No saved runs yet. Go for a run to see it here.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(runs, key = { it.id }) { run ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { onOpenRun(run.id) }) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                DateFormat.getDateTimeInstance().format(Date(run.startTimeEpochMs)),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(formatDistanceMeters(run.totalDistanceMeters))
                            Text(formatDurationMs(run.movingDurationMs))
                            Text("Avg pace: ${formatPaceSecPerKm(run.averagePaceSecPerKm)}")
                        }
                    }
                }
            }
        }
    }
}
