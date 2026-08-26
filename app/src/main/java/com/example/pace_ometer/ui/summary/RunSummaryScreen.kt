package com.example.pace_ometer.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.pace_ometer.PaceometerApp
import com.example.pace_ometer.ui.common.SimpleViewModelFactory
import com.example.pace_ometer.ui.common.charts.LineChart
import com.example.pace_ometer.util.AgeAndHrZoneCalculator
import com.example.pace_ometer.util.formatDistanceMeters
import com.example.pace_ometer.util.formatDurationMs
import com.example.pace_ometer.util.formatPaceSecPerKm
import com.example.pace_ometer.util.formatStepsPerDistanceUnit
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.tileprovider.tilesource.TileSourceFactory

@Composable
fun RunSummaryScreen(
    runId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as PaceometerApp
    val viewModel: RunSummaryViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = SimpleViewModelFactory { RunSummaryViewModel(app, runId) }
    )

    val run by viewModel.run.collectAsState()
    val samples by viewModel.samples.collectAsState()
    val settings by viewModel.settings.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Run Summary") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            run?.let { r ->
                Card {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(formatDistanceMeters(r.totalDistanceMeters), style = MaterialTheme.typography.headlineSmall)
                        Text(formatDurationMs(r.movingDurationMs))
                        Text("Avg pace: ${formatPaceSecPerKm(r.averagePaceSecPerKm)}")
                        r.avgHeartRateBpm?.let { avgBpm ->
                            Text("Avg heart rate: $avgBpm bpm (max ${r.maxHeartRateBpm})")
                            val birthDateEpochDay = settings.birthDateEpochDay
                            if (birthDateEpochDay != null) {
                                val maxHr = AgeAndHrZoneCalculator.estimatedMaxHeartRateBpm(
                                    AgeAndHrZoneCalculator.ageYears(birthDateEpochDay)
                                )
                                val avgZone = AgeAndHrZoneCalculator.zoneFor(avgBpm, maxHr)
                                val avgEffort = AgeAndHrZoneCalculator.effortPercent(avgBpm, maxHr)
                                val zoneLabel = avgZone?.let { "Z${it.number} (${it.label})" } ?: "below Z1"
                                Text("Avg heart rate zone: $zoneLabel, $avgEffort% effort")
                            }
                        }
                        r.estimatedCalories?.let { Text("Calories: ${it.toInt()}") }
                        r.elevationGainMeters?.let { gain ->
                            Text("Elevation gain/loss: +${gain.toInt()}m / -${(r.elevationLossMeters ?: 0.0).toInt()}m")
                        }
                        r.stepCount?.takeIf { it > 0 }?.let { steps ->
                            val stepsPerDistance = formatStepsPerDistanceUnit(
                                steps, r.totalDistanceMeters, settings.unitSystem
                            )
                            Text("Steps: $steps ($stepsPerDistance)")
                        }
                    }
                }
            }

            val pathPoints = samples.mapNotNull { s ->
                if (s.latitude != null && s.longitude != null) GeoPoint(s.latitude, s.longitude) else null
            }
            if (pathPoints.size >= 2) {
                RunMap(points = pathPoints)
            }

            val startTime = run?.startTimeEpochMs ?: samples.firstOrNull()?.timestampEpochMs ?: 0L

            LineChart(
                title = "Pace over distance",
                points = samples.mapNotNull { s ->
                    s.instantaneousPaceSecPerKm?.let { (s.cumulativeDistanceMeters / 1000f).toFloat() to it.toFloat() }
                },
                valueFormatter = { "${it.toInt()}s/km" },
                xValueFormatter = { "%.1fkm".format(it) }
            )

            LineChart(
                title = "Elevation over distance",
                points = samples.mapNotNull { s ->
                    s.elevationMeters?.let { (s.cumulativeDistanceMeters / 1000f).toFloat() to it.toFloat() }
                },
                valueFormatter = { "${it.toInt()}m" },
                xValueFormatter = { "%.1fkm".format(it) }
            )

            val heartRatePoints = samples.mapNotNull { s ->
                s.heartRateBpm?.let { ((s.timestampEpochMs - startTime) / 1000f / 60f) to it.toFloat() }
            }
            if (heartRatePoints.isNotEmpty()) {
                LineChart(
                    title = "Heart rate over time",
                    points = heartRatePoints,
                    valueFormatter = { "${it.toInt()} bpm" },
                    xValueFormatter = { "%.1fmin".format(it) }
                )
            }

            val cadencePoints = samples.mapNotNull { s ->
                s.cadenceSpm?.let { (s.cumulativeDistanceMeters / 1000f).toFloat() to it.toFloat() }
            }
            if (cadencePoints.isNotEmpty()) {
                LineChart(
                    title = "Cadence over distance",
                    points = cadencePoints,
                    valueFormatter = { "${it.toInt()} spm" },
                    xValueFormatter = { "%.1fkm".format(it) }
                )
            }

            val caloriesOverTime by viewModel.caloriesOverTime.collectAsState()
            if (caloriesOverTime.size >= 2) {
                LineChart(
                    title = "Calories burned over time",
                    points = caloriesOverTime,
                    valueFormatter = { "${it.toInt()} kcal" },
                    xValueFormatter = { "%.1fmin".format(it) }
                )
            }
        }
    }
}

@Composable
private fun RunMap(points: List<GeoPoint>) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        factory = { context ->
            MapView(context).apply {
                // tile.openstreetmap.org's usage policy explicitly excludes "distributing a
                // mobile application" and 403-blocks non-compliant traffic; Wikimedia's tile
                // service similarly 403s with "restricted to Wikimedia and affiliated sites
                // only" (confirmed live via logcat). OpenTopoMap is the tile source actually
                // reachable from a sideloaded, peer-distributed app without an API key.
                setTileSource(TileSourceFactory.OpenTopo)
                setMultiTouchControls(true)
            }
        },
        update = { mapView ->
            mapView.overlays.clear()
            val polyline = Polyline().apply { setPoints(points) }
            mapView.overlays.add(polyline)
            val boundingBox = org.osmdroid.util.BoundingBox.fromGeoPoints(points)
            mapView.post {
                mapView.zoomToBoundingBox(boundingBox, false, 50)
                // A near-zero-area box (e.g. a run that barely moved) makes zoomToBoundingBox
                // compute a zoom level far past what the tile source actually serves, so every
                // tile request fails outright -- clamp back to a sane maximum.
                val maxSupportedZoom = mapView.tileProvider.tileSource.maximumZoomLevel.toDouble()
                if (mapView.zoomLevelDouble > maxSupportedZoom) {
                    mapView.controller.setZoom(maxSupportedZoom)
                }
            }
            mapView.invalidate()
        }
    )
}
