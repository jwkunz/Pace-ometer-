package com.example.pace_ometer.data.repository

import com.example.pace_ometer.data.db.dao.PersonalRecordDao
import com.example.pace_ometer.data.db.dao.SeasonDao
import com.example.pace_ometer.data.db.entity.PersonalRecordCategory
import com.example.pace_ometer.data.db.entity.PersonalRecordEntity
import com.example.pace_ometer.data.db.entity.PersonalRecordScope
import com.example.pace_ometer.data.db.entity.RunEntity
import com.example.pace_ometer.data.db.entity.RunSampleEntity
import com.example.pace_ometer.records.PersonalRecordEvaluator
import kotlinx.coroutines.flow.Flow

class PersonalRecordRepository(
    private val personalRecordDao: PersonalRecordDao,
    private val seasonDao: SeasonDao
) {
    fun observeForScope(scope: String): Flow<List<PersonalRecordEntity>> =
        personalRecordDao.observeForScope(scope)

    fun observeScopes(): Flow<List<String>> = personalRecordDao.observeScopes()

    /** Evaluates a just-saved run against All-Time and its season, upserting any new bests. */
    suspend fun evaluateAndUpsert(run: RunEntity, samples: List<RunSampleEntity>) {
        val samplePairs = samples.map { it.timestampEpochMs to it.cumulativeDistanceMeters }
        val candidates = PersonalRecordEvaluator.evaluate(run.totalDistanceMeters, run.movingDurationMs, samplePairs)

        val scopes = mutableListOf(PersonalRecordScope.ALL_TIME)
        seasonDao.findSeasonForTimestamp(run.startTimeEpochMs)?.let { scopes += PersonalRecordScope.season(it.id) }

        for (scope in scopes) {
            for (candidate in candidates) {
                val existing = personalRecordDao.get(candidate.category, scope)
                if (existing == null || isBetter(candidate.category, candidate.value, existing.value)) {
                    personalRecordDao.upsert(
                        PersonalRecordEntity(
                            category = candidate.category,
                            scope = scope,
                            value = candidate.value,
                            runId = run.id,
                            achievedAtEpochMs = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    private fun isBetter(category: String, newValue: Double, oldValue: Double): Boolean =
        if (category == PersonalRecordCategory.LONGEST_DISTANCE) newValue > oldValue else newValue < oldValue
}
