package com.example.pace_ometer.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.pace_ometer.data.db.entity.RunEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RunDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(run: RunEntity): Long

    @Update
    suspend fun update(run: RunEntity)

    @Delete
    suspend fun delete(run: RunEntity)

    @Query("SELECT * FROM runs WHERE id = :runId")
    suspend fun getById(runId: Long): RunEntity?

    @Query("SELECT * FROM runs WHERE id = :runId")
    fun observeById(runId: Long): Flow<RunEntity?>

    @Query("SELECT * FROM runs WHERE isSaved = 1 ORDER BY startTimeEpochMs DESC")
    fun observeSavedRuns(): Flow<List<RunEntity>>

    @Query("SELECT * FROM runs WHERE isSaved = 1 ORDER BY startTimeEpochMs DESC")
    suspend fun getSavedRuns(): List<RunEntity>

    @Query("DELETE FROM runs WHERE id = :runId")
    suspend fun deleteById(runId: Long)
}
