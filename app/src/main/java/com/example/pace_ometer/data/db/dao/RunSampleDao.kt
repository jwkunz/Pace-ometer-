package com.example.pace_ometer.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pace_ometer.data.db.entity.RunSampleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RunSampleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sample: RunSampleEntity): Long

    @Query("SELECT * FROM run_samples WHERE runId = :runId ORDER BY timestampEpochMs ASC")
    fun observeForRun(runId: Long): Flow<List<RunSampleEntity>>

    @Query("SELECT * FROM run_samples WHERE runId = :runId ORDER BY timestampEpochMs ASC")
    suspend fun getForRun(runId: Long): List<RunSampleEntity>

    @Query("SELECT * FROM run_samples WHERE runId = :runId ORDER BY timestampEpochMs DESC LIMIT 1")
    suspend fun getLatestForRun(runId: Long): RunSampleEntity?
}
