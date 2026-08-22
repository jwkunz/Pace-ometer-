package com.example.pace_ometer.data.repository

import com.example.pace_ometer.data.db.dao.RunDao
import com.example.pace_ometer.data.db.dao.RunSampleDao
import com.example.pace_ometer.data.db.entity.RunEntity
import com.example.pace_ometer.data.db.entity.RunSampleEntity
import kotlinx.coroutines.flow.Flow

class RunRepository(
    private val runDao: RunDao,
    private val runSampleDao: RunSampleDao
) {
    suspend fun startRun(startTimeEpochMs: Long, unitSystem: String, bodyWeightKg: Double?): Long =
        runDao.insert(
            RunEntity(
                startTimeEpochMs = startTimeEpochMs,
                unitSystemAtRunTime = unitSystem,
                bodyWeightKgAtRunTime = bodyWeightKg
            )
        )

    suspend fun updateRun(run: RunEntity) = runDao.update(run)

    suspend fun discardRun(runId: Long) = runDao.deleteById(runId)

    suspend fun getRun(runId: Long): RunEntity? = runDao.getById(runId)

    fun observeRun(runId: Long): Flow<RunEntity?> = runDao.observeById(runId)

    fun observeSavedRuns(): Flow<List<RunEntity>> = runDao.observeSavedRuns()

    suspend fun getSavedRuns(): List<RunEntity> = runDao.getSavedRuns()

    fun observeSamples(runId: Long): Flow<List<RunSampleEntity>> = runSampleDao.observeForRun(runId)

    suspend fun getSamples(runId: Long): List<RunSampleEntity> = runSampleDao.getForRun(runId)

    suspend fun addSample(sample: RunSampleEntity) = runSampleDao.insert(sample)
}
