package com.example.pace_ometer.ui.summary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pace_ometer.PaceometerApp
import com.example.pace_ometer.calories.CalorieEstimator
import com.example.pace_ometer.data.db.entity.RunEntity
import com.example.pace_ometer.data.db.entity.RunSampleEntity
import com.example.pace_ometer.data.settings.UserSettings
import com.example.pace_ometer.util.averagePaceSecPerKm
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Average pace is elapsed-time / distance, so it's numerically unstable (and not meaningfully
 * "an average" yet) for the first few meters of a run -- excluding those samples keeps an early
 * GPS-startup transient from dominating the chart's whole y-axis scale.
 */
private const val MIN_DISTANCE_METERS_FOR_AVERAGE_PACE = 50.0

/**
 * Samples are only ever recorded while RUNNING, so a gap between consecutive samples much wider
 * than a normal GPS/step update is almost certainly a pause, not real elapsed time -- clamping it
 * keeps a paused stretch from inflating the reconstructed moving-time denominator, matching how
 * the live tracker's movingDurationMs already excludes paused time.
 */
private const val MAX_PLAUSIBLE_SAMPLE_GAP_MS = 10_000L

class RunSummaryViewModel(application: Application, runId: Long) : AndroidViewModel(application) {

    private val app = application as PaceometerApp
    private val runRepository = app.runRepository

    val run: StateFlow<RunEntity?> = runRepository.observeRun(runId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val samples: StateFlow<List<RunSampleEntity>> = runRepository.observeSamples(runId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<UserSettings> = app.settingsRepository.userSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    /**
     * Cumulative calories burned vs. elapsed *moving* minutes, reconstructed from consecutive
     * samples' pace/time deltas (the same MET formula the live tracker uses) rather than stored
     * per-sample, since only the running total was ever persisted on the run itself. Gaps are
     * clamped the same way as [averagePaceOverDistance]: an unclamped gap spanning a pause would
     * otherwise both misattribute walking-MET calories to time spent stationary, and drift the
     * x-axis away from the moving-time-only totals shown elsewhere on this screen.
     */
    val caloriesOverTime: StateFlow<List<Pair<Float, Float>>> = combine(run, samples) { r, sampleList ->
        val weightKg = r?.bodyWeightKgAtRunTime
        if (weightKg == null || sampleList.size < 2) return@combine emptyList()
        var cumulativeCalories = 0.0
        var movingDurationMs = 0L
        val points = mutableListOf(0f to 0f)
        for (i in 1 until sampleList.size) {
            val previous = sampleList[i - 1]
            val current = sampleList[i]
            val durationMs = (current.timestampEpochMs - previous.timestampEpochMs)
                .coerceIn(0, MAX_PLAUSIBLE_SAMPLE_GAP_MS)
            movingDurationMs += durationMs
            val distanceDeltaMeters = current.cumulativeDistanceMeters - previous.cumulativeDistanceMeters
            val speedKmh = if (durationMs > 0) (distanceDeltaMeters / 1000.0) / (durationMs / 3_600_000.0) else 0.0
            cumulativeCalories += CalorieEstimator.estimateSegmentCalories(speedKmh, weightKg, durationMs)
            val elapsedMinutes = movingDurationMs / 1000f / 60f
            points += elapsedMinutes to cumulativeCalories.toFloat()
        }
        points
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Cumulative average pace (total distance / total *moving* time up to that point) vs.
     * distance -- distinct from the instantaneous "Pace over distance" chart, which shows
     * point-in-time speed rather than the running average. Moving time is reconstructed from
     * consecutive samples' timestamp deltas (clamped, see MAX_PLAUSIBLE_SAMPLE_GAP_MS) rather
     * than wall-clock time since the run started, so a pause doesn't drag this below the
     * moving-time-only average pace already shown elsewhere on this same screen.
     */
    val averagePaceOverDistance: StateFlow<List<Pair<Float, Float>>> = combine(run, samples) { r, sampleList ->
        if (r == null || sampleList.isEmpty()) return@combine emptyList()
        var movingDurationMs = 0L
        val points = mutableListOf<Pair<Float, Float>>()
        for (i in 1 until sampleList.size) {
            val previous = sampleList[i - 1]
            val current = sampleList[i]
            val deltaMs = (current.timestampEpochMs - previous.timestampEpochMs)
                .coerceIn(0, MAX_PLAUSIBLE_SAMPLE_GAP_MS)
            movingDurationMs += deltaMs
            if (current.cumulativeDistanceMeters < MIN_DISTANCE_METERS_FOR_AVERAGE_PACE) continue
            averagePaceSecPerKm(current.cumulativeDistanceMeters, movingDurationMs)?.let { avgPace ->
                points += (current.cumulativeDistanceMeters / 1000f).toFloat() to avgPace.toFloat()
            }
        }
        points
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
