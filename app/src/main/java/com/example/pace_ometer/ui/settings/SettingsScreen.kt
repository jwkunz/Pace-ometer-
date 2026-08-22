package com.example.pace_ometer.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pace_ometer.data.settings.UnitSystem
import com.example.pace_ometer.util.cmToDisplayHeight
import com.example.pace_ometer.util.displayHeightToCm
import com.example.pace_ometer.util.displayWeightToKg
import com.example.pace_ometer.util.kgToDisplayWeight

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
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
        }
    }
}
