package com.example.pace_ometer.export

import com.example.pace_ometer.data.db.entity.RunEntity
import com.example.pace_ometer.data.db.entity.RunSampleEntity
import com.example.pace_ometer.data.repository.RunRepository
import kotlinx.serialization.json.Json

class RunJsonExporter(private val runRepository: RunRepository) {

    // encodeDefaults must be on, or exportSchemaVersion (currently always its default of 1)
    // would be silently omitted from every export -- defeating the point of a version field.
    private val json = Json { prettyPrint = true; encodeDefaults = true }

    suspend fun exportSavedRuns(): String {
        val runs = runRepository.getSavedRuns()
        val entries = runs.map { run -> run.toExportEntry(runRepository.getSamples(run.id)) }
        val export = RunExport(exportedAtEpochMs = System.currentTimeMillis(), runs = entries)
        return json.encodeToString(RunExport.serializer(), export)
    }

    suspend fun exportRun(runId: Long): String? {
        val run = runRepository.getRun(runId) ?: return null
        val export = RunExport(
            exportedAtEpochMs = System.currentTimeMillis(),
            runs = listOf(run.toExportEntry(runRepository.getSamples(runId)))
        )
        return json.encodeToString(RunExport.serializer(), export)
    }

    private fun RunEntity.toExportEntry(samples: List<RunSampleEntity>): RunExportEntry = RunExportEntry(
        id = id,
        startTimeEpochMs = startTimeEpochMs,
        endTimeEpochMs = endTimeEpochMs,
        totalDistanceMeters = totalDistanceMeters,
        totalDurationMs = totalDurationMs,
        movingDurationMs = movingDurationMs,
        averagePaceSecPerKm = averagePaceSecPerKm,
        elevationGainMeters = elevationGainMeters,
        elevationLossMeters = elevationLossMeters,
        avgHeartRateBpm = avgHeartRateBpm,
        maxHeartRateBpm = maxHeartRateBpm,
        avgCadenceSpm = avgCadenceSpm,
        estimatedCalories = estimatedCalories,
        stepCount = stepCount,
        unitSystemAtRunTime = unitSystemAtRunTime,
        samples = samples.map {
            RunSampleExportEntry(
                timestampEpochMs = it.timestampEpochMs,
                latitude = it.latitude,
                longitude = it.longitude,
                elevationMeters = it.elevationMeters,
                cumulativeDistanceMeters = it.cumulativeDistanceMeters,
                instantaneousPaceSecPerKm = it.instantaneousPaceSecPerKm,
                heartRateBpm = it.heartRateBpm,
                cadenceSpm = it.cadenceSpm
            )
        }
    )
}
