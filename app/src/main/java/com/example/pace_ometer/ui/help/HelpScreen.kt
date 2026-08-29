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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat

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
        "Physical activity permission",
        "Optional but recommended -- without it, Android silently blocks the phone's step " +
            "sensor entirely, so Steps, Cadence, Stride length, and the accelerometer pace " +
            "fallback during GPS gaps won't work. A run still tracks fine via GPS alone without " +
            "this permission; it's only those specific step-based metrics that need it."
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
            "and max are saved with each run. A running cadence (footpod) or cycling cadence " +
            "sensor isn't supported yet, but the app is built to add either without needing to " +
            "reconnect your other sensors."
    ),
    HelpSection(
        "Auto-pause",
        "Optional -- when enabled in Settings, the run automatically pauses after a configurable " +
            "number of seconds (5 by default) without detected motion, and resumes once motion " +
            "has been sustained for a separately configurable number of seconds (3 by default) -- " +
            "a brief blip of motion while still stationary won't resume it early. Pausing or " +
            "resuming, whether automatic or by tapping the button yourself, always announces " +
            "\"Pausing run\" / \"Starting run\". A pause you trigger yourself is never " +
            "auto-resumed for you."
    ),
    HelpSection(
        "Voice announcements",
        "Choose how often (by distance) Pace-ometer speaks a status update during your run, and " +
            "which facts it includes -- distance, elapsed time, current, projected, and average pace, " +
            "elevation, heart rate, heart rate zone, cadence, calories, and clock time. Only " +
            "toggled-on items are spoken, and only if the corresponding data is available (e.g. " +
            "heart rate needs a connected monitor, and heart rate zone additionally needs a " +
            "birthdate on file). Speech rate controls how fast the announcements are spoken -- " +
            "1.0 is the engine's normal rate, higher speaks faster."
    ),
    HelpSection(
        "Seasons and personal records",
        "Every run you save is automatically checked against your personal records -- no separate " +
            "submission step. Records are tracked both All-Time and per season. Add a season in " +
            "Settings with a name and start date; a run belongs to whichever season most recently " +
            "started before it. View records for any scope from the Personal Records screen."
    ),
    HelpSection(
        "Exporting your data",
        "From Run History, tap the export icon to save all of your runs and their sample data as " +
            "a single JSON file you choose where to save. This is the only way data ever leaves " +
            "your device -- see Legal for the full privacy notice."
    )
)

@Composable
fun HelpScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val versionLabel = remember {
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            "Version ${info.versionName} (build ${PackageInfoCompat.getLongVersionCode(info)})"
        }.getOrDefault("Version unknown")
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Help") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                versionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            helpSections.forEach { section ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(section.title, style = MaterialTheme.typography.titleMedium)
                    Text(section.body, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
