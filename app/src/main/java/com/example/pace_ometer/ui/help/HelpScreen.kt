package com.example.pace_ometer.ui.help

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

private data class HelpSection(val title: String, val body: String)

private val helpSections = listOf(
    HelpSection(
        "Unit system",
        "Choose Metric (kilometers, kilograms) or Imperial (miles, pounds) in Settings. " +
            "All distances, paces, and weights throughout the app follow this choice."
    ),
    HelpSection(
        "Body weight, height, birthdate, gender",
        "Body weight drives the calorie estimate for each run. Birthdate is used to estimate your " +
            "maximum heart rate and heart-rate training zones. Height is optional and only used to " +
            "show your BMI. Gender is stored for potential future refinements and isn't required."
    ),
    HelpSection(
        "Background location",
        "Pace-ometer requires \"Allow all the time\" location access so your run keeps tracking " +
            "accurately even with the screen off or the phone in a pocket. Without it, distance and " +
            "pace tracking will be unreliable."
    ),
    HelpSection(
        "Saving a run",
        "When you stop a run, you choose whether to save it to your run history or discard it. " +
            "Only saved runs count toward your run history and personal records."
    ),
    HelpSection(
        "Heart rate monitor",
        "In Settings, tap \"Scan for heart rate monitor\" to find a nearby Bluetooth heart-rate " +
            "strap and connect it. Once paired, your live BPM is shown during runs and its average " +
            "and max are saved with each run. A cadence (footpod) sensor isn't supported yet, but " +
            "the app is built to add one without needing to reconnect your other sensors."
    )
)

@Composable
fun HelpScreen(onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Help") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            helpSections.forEach { section ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(section.title, style = MaterialTheme.typography.titleMedium)
                    Text(section.body, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
