package com.example.pace_ometer.ui.equipment

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.example.pace_ometer.data.db.entity.EquipmentEntity
import com.example.pace_ometer.util.formatDistanceMeters

@Composable
fun EquipmentScreen(
    onBack: () -> Unit,
    onOpenEquipment: (Long) -> Unit,
    viewModel: EquipmentViewModel = viewModel()
) {
    val equipment by viewModel.equipment.collectAsState()
    val settings by viewModel.settings.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val (active, retired) = equipment.partition { !it.retired }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Equipment") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
        }
    ) { padding ->
        if (equipment.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                Text(
                    "Track usage on gear like running shoes -- add a piece of equipment and " +
                        "assign saved runs to it to see cumulative distance."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (active.isNotEmpty()) {
                    item { Text("Active", style = MaterialTheme.typography.titleSmall) }
                    items(active, key = { it.id }) { item ->
                        EquipmentCard(item, settings.unitSystem, onClick = { onOpenEquipment(item.id) })
                    }
                }
                if (retired.isNotEmpty()) {
                    item { Text("Retired", style = MaterialTheme.typography.titleSmall) }
                    items(retired, key = { it.id }) { item ->
                        EquipmentCard(item, settings.unitSystem, onClick = { onOpenEquipment(item.id) })
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddEquipmentDialog(
            unitSystem = settings.unitSystem,
            onDismiss = { showAddDialog = false },
            onAdd = { name, type, startingDistanceMeters ->
                viewModel.addEquipment(name, type, startingDistanceMeters)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun EquipmentCard(
    equipment: EquipmentEntity,
    unitSystem: com.example.pace_ometer.data.settings.UnitSystem,
    onClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(equipment.name, style = MaterialTheme.typography.titleMedium)
            Text(equipment.type, style = MaterialTheme.typography.bodySmall)
            EquipmentTotalDistanceText(equipmentId = equipment.id, unitSystem = unitSystem)
            if (equipment.retired) {
                Text(
                    "Retired",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun EquipmentTotalDistanceText(
    equipmentId: Long,
    unitSystem: com.example.pace_ometer.data.settings.UnitSystem
) {
    val context = LocalContext.current
    val app = context.applicationContext as PaceometerApp
    val totalDistanceMeters by remember(equipmentId) {
        app.equipmentRepository.observeTotalDistanceMeters(equipmentId)
    }.collectAsState(initial = 0.0)

    Text(
        "Total: ${formatDistanceMeters(totalDistanceMeters, unitSystem)}",
        style = MaterialTheme.typography.bodyMedium
    )
}
