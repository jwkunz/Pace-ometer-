package com.example.pace_ometer.ui.equipment

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.example.pace_ometer.PaceometerApp
import com.example.pace_ometer.data.db.entity.RunEntity
import com.example.pace_ometer.ui.common.SimpleViewModelFactory
import com.example.pace_ometer.util.displayUnitDistanceToMeters
import com.example.pace_ometer.util.formatDistanceMeters
import com.example.pace_ometer.util.metersToDisplayUnitDistance
import java.text.DateFormat
import java.util.Date

@Composable
fun EquipmentDetailScreen(
    equipmentId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as PaceometerApp
    val viewModel: EquipmentDetailViewModel = viewModel(
        factory = SimpleViewModelFactory { EquipmentDetailViewModel(app, equipmentId) }
    )

    val equipment by viewModel.equipment.collectAsState()
    val totalDistanceMeters by viewModel.totalDistanceMeters.collectAsState()
    val assignedRuns by viewModel.assignedRuns.collectAsState()
    val allSavedRuns by viewModel.allSavedRuns.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val unitSystem = settings.unitSystem

    val current = equipment ?: return

    var nameText by remember(current.id, current.name) { mutableStateOf(current.name) }
    var typeText by remember(current.id, current.type) { mutableStateOf(current.type) }
    var startingDistanceText by remember(current.id, current.startingDistanceMeters, unitSystem) {
        mutableStateOf("%.2f".format(metersToDisplayUnitDistance(current.startingDistanceMeters, unitSystem)))
    }
    var showAssignDialog by remember { mutableStateOf(false) }
    var showRetireConfirm by remember { mutableStateOf(false) }

    val distanceUnitLabel = if (unitSystem == com.example.pace_ometer.data.settings.UnitSystem.IMPERIAL) "mi" else "km"

    Scaffold(topBar = { TopAppBar(title = { Text(current.name) }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (current.retired) {
                Text(
                    "Retired",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text("Total distance: ${formatDistanceMeters(totalDistanceMeters, unitSystem)}", style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(
                value = nameText,
                onValueChange = { nameText = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = typeText,
                onValueChange = { typeText = it },
                label = { Text("Type") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = startingDistanceText,
                onValueChange = { startingDistanceText = it },
                label = { Text("Starting distance ($distanceUnitLabel)") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    val startingDistanceMeters = displayUnitDistanceToMeters(
                        startingDistanceText.toDoubleOrNull() ?: 0.0,
                        unitSystem
                    )
                    viewModel.updateDetails(nameText.ifBlank { current.name }, typeText.ifBlank { current.type }, startingDistanceMeters)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save changes") }

            HorizontalDivider()

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Assigned runs (${assignedRuns.size})", style = MaterialTheme.typography.titleSmall)
                if (!current.retired) {
                    TextButton(onClick = { showAssignDialog = true }) { Text("Assign a run") }
                }
            }

            if (assignedRuns.isEmpty()) {
                Text("No saved runs assigned yet.", style = MaterialTheme.typography.bodySmall)
            } else {
                assignedRuns.forEach { run ->
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(DateFormat.getDateInstance().format(Date(run.startTimeEpochMs)))
                            Text(formatDistanceMeters(run.totalDistanceMeters, unitSystem), style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { viewModel.unassignRun(run.id) }) { Text("Remove") }
                    }
                }
            }

            HorizontalDivider()

            if (current.retired) {
                OutlinedButton(onClick = { viewModel.setRetired(false) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Unretire")
                }
            } else {
                Button(
                    onClick = { showRetireConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Retire equipment") }
            }
        }
    }

    if (showAssignDialog) {
        AssignRunDialog(
            allSavedRuns = allSavedRuns,
            assignedRunIds = assignedRuns.map { it.id }.toSet(),
            unitSystem = unitSystem,
            onDismiss = { showAssignDialog = false },
            onToggle = { runId, isAssigned ->
                if (isAssigned) viewModel.unassignRun(runId) else viewModel.assignRun(runId)
            }
        )
    }

    if (showRetireConfirm) {
        AlertDialog(
            onDismissRequest = { showRetireConfirm = false },
            title = { Text("Retire ${current.name}?") },
            text = { Text("Retired equipment is kept for its usage history but won't be offered for new runs. You can unretire it later.") },
            confirmButton = {
                TextButton(onClick = {
                    showRetireConfirm = false
                    viewModel.setRetired(true)
                }) { Text("Retire") }
            },
            dismissButton = { TextButton(onClick = { showRetireConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AssignRunDialog(
    allSavedRuns: List<RunEntity>,
    assignedRunIds: Set<Long>,
    unitSystem: com.example.pace_ometer.data.settings.UnitSystem,
    onDismiss: () -> Unit,
    onToggle: (runId: Long, isAssigned: Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = { Text("Assign saved runs") },
        text = {
            if (allSavedRuns.isEmpty()) {
                Text("No saved runs yet.")
            } else {
                LazyColumn {
                    items(allSavedRuns, key = { it.id }) { run ->
                        val isAssigned = run.id in assignedRunIds
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column {
                                Text(DateFormat.getDateInstance().format(Date(run.startTimeEpochMs)))
                                Text(formatDistanceMeters(run.totalDistanceMeters, unitSystem), style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { onToggle(run.id, isAssigned) }) {
                                Text(if (isAssigned) "Assigned ✓" else "Assign")
                            }
                        }
                    }
                }
            }
        }
    )
}
