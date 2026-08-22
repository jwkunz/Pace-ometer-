package com.example.pace_ometer.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.pace_ometer.data.db.entity.SeasonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SeasonDao {
    @Insert
    suspend fun insert(season: SeasonEntity): Long

    @Delete
    suspend fun delete(season: SeasonEntity)

    @Query("SELECT * FROM seasons ORDER BY startEpochMs DESC")
    fun observeAll(): Flow<List<SeasonEntity>>

    @Query("SELECT * FROM seasons ORDER BY startEpochMs DESC")
    suspend fun getAll(): List<SeasonEntity>

    @Query("SELECT * FROM seasons WHERE startEpochMs <= :timestampMs ORDER BY startEpochMs DESC LIMIT 1")
    suspend fun findSeasonForTimestamp(timestampMs: Long): SeasonEntity?
}
