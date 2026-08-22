package com.example.pace_ometer.ui.equipment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.example.pace_ometer.data.settings.UnitSystem
import com.example.pace_ometer.util.displayUnitDistanceToMeters

/** A pair of fields shared by the onboarding flow and Settings' equipment screen. */
@Composable
fun AddEquipmentDialog(
    unitSystem: UnitSystem,
    onDismiss: () -> Unit,
    onAdd: (name: String, type: String, startingDistanceMeters: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var startingDistanceText by remember { mutableStateOf("0") }
    val distanceUnitLabel = if (unitSystem == UnitSystem.IMPERIAL) "mi" else "km"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add equipment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text("Type (e.g. Running Shoes)") }
                )
                OutlinedTextField(
                    value = startingDistanceText,
                    onValueChange = { startingDistanceText = it },
                    label = { Text("Starting distance ($distanceUnitLabel) — if already used") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val startingDistanceMeters = displayUnitDistanceToMeters(
                        startingDistanceText.toDoubleOrNull() ?: 0.0,
                        unitSystem
                    )
                    onAdd(name.ifBlank { "Equipment" }, type.ifBlank { "Other" }, startingDistanceMeters)
                },
                enabled = name.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
