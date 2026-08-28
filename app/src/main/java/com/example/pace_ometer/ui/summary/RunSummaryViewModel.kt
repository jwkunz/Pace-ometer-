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
     * Cumulative calories burned vs. elapsed minutes, reconstructed from consecutive samples'
     * pace/time deltas (the same MET formula the live tracker uses) rather than stored per-sample,
     * since only the running total was ever persisted on the run itself.
     */
    val caloriesOverTime: StateFlow<List<Pair<Float, Float>>> = combine(run, samples) { r, sampleList ->
        val weightKg = r?.bodyWeightKgAtRunTime
        if (weightKg == null || sampleList.size < 2) return@combine emptyList()
        val startTime = r.startTimeEpochMs
        var cumulativeCalories = 0.0
        val points = mutableListOf(0f to 0f)
        for (i in 1 until sampleList.size) {
            val previous = sampleList[i - 1]
            val current = sampleList[i]
            val durationMs = current.timestampEpochMs - previous.timestampEpochMs
            val distanceDeltaMeters = current.cumulativeDistanceMeters - previous.cumulativeDistanceMeters
            val speedKmh = if (durationMs > 0) (distanceDeltaMeters / 1000.0) / (durationMs / 3_600_000.0) else 0.0
            cumulativeCalories += CalorieEstimator.estimateSegmentCalories(speedKmh, weightKg, durationMs)
            val elapsedMinutes = (current.timestampEpochMs - startTime) / 1000f / 60f
            points += elapsedMinutes to cumulativeCalories.toFloat()
        }
        points
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Cumulative average pace (total distance / total elapsed time up to that point) vs.
     * distance -- distinct from the instantaneous "Pace over distance" chart, which shows
     * point-in-time speed rather than the running average.
     */
    val averagePaceOverDistance: StateFlow<List<Pair<Float, Float>>> = combine(run, samples) { r, sampleList ->
        val startTime = r?.startTimeEpochMs ?: return@combine emptyList()
        sampleList.mapNotNull { sample ->
            val elapsedMs = sample.timestampEpochMs - startTime
            averagePaceSecPerKm(sample.cumulativeDistanceMeters, elapsedMs)?.let { avgPace ->
                (sample.cumulativeDistanceMeters / 1000f).toFloat() to avgPace.toFloat()
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
