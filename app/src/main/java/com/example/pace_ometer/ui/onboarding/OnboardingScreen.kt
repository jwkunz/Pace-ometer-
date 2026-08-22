package com.example.pace_ometer.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pace_ometer.PaceometerApp
import com.example.pace_ometer.data.settings.Gender
import com.example.pace_ometer.data.settings.UnitSystem
import com.example.pace_ometer.ui.common.SimpleViewModelFactory
import com.example.pace_ometer.util.displayHeightToCm
import com.example.pace_ometer.util.displayWeightToKg
import java.time.Instant
import java.time.ZoneOffset

private fun Gender.label(): String = when (this) {
    Gender.MALE -> "Male"
    Gender.FEMALE -> "Female"
    Gender.OTHER -> "Other"
    Gender.PREFER_NOT_TO_SAY -> "Prefer not to say"
}

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as PaceometerApp
    val viewModel: OnboardingViewModel = viewModel(
        factory = SimpleViewModelFactory { OnboardingViewModel(app.settingsRepository) }
    )

    var unitSystem by remember { mutableStateOf(UnitSystem.METRIC) }
    var gender by remember { mutableStateOf(Gender.PREFER_NOT_TO_SAY) }
    var birthDateEpochDay by remember { mutableStateOf<Long?>(null) }
    var weightText by remember { mutableStateOf("70") }
    var heightText by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    val weightUnitLabel = if (unitSystem == UnitSystem.IMPERIAL) "lb" else "kg"
    val heightUnitLabel = if (unitSystem == UnitSystem.IMPERIAL) "in" else "cm"

    Scaffold(topBar = { TopAppBar(title = { Text("Welcome to Pace-ometer") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "A few quick details help us estimate calories, heart-rate zones, and " +
                    "show your stats in the units you prefer. You can change any of this later in Settings.",
                style = MaterialTheme.typography.bodyMedium
            )

            Text("Unit system", style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow {
                UnitSystem.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index, UnitSystem.entries.size),
                        selected = unitSystem == option,
                        onClick = { unitSystem = option }
                    ) { Text(if (option == UnitSystem.METRIC) "Metric" else "Imperial") }
                }
            }

            Text("Birthdate (used for heart-rate zones)", style = MaterialTheme.typography.titleSmall)
            TextButton(onClick = { showDatePicker = true }) {
                Text(
                    birthDateEpochDay?.let {
                        Instant.ofEpochSecond(it * 86400).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    } ?: "Select birthdate"
                )
            }

            Text("Gender", style = MaterialTheme.typography.titleSmall)
            Column {
                Gender.entries.forEach { option ->
                    GenderRadioRow(gender, option) { gender = option }
                }
            }

            OutlinedTextField(
                value = weightText,
                onValueChange = { weightText = it },
                label = { Text("Body weight ($weightUnitLabel)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = heightText,
                onValueChange = { heightText = it },
                label = { Text("Height — optional ($heightUnitLabel)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val day = birthDateEpochDay ?: return@Button
                    val weightDisplay = weightText.toFloatOrNull() ?: return@Button
                    val heightDisplay = heightText.toFloatOrNull()
                    viewModel.completeOnboarding(
                        birthDateEpochDay = day,
                        gender = gender,
                        unitSystem = unitSystem,
                        bodyWeightKg = displayWeightToKg(weightDisplay, unitSystem),
                        heightCm = heightDisplay?.let { displayHeightToCm(it, unitSystem) },
                        onDone = onComplete
                    )
                },
                enabled = birthDateEpochDay != null && weightText.toFloatOrNull() != null,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Get started") }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis -> birthDateEpochDay = millis / 86_400_000L }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }
}

@Composable
private fun GenderRadioRow(selected: Gender, option: Gender, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        RadioButton(selected = selected == option, onClick = onClick)
        Text(option.label())
    }
}
