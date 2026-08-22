package com.example.pace_ometer.data.repository

import com.example.pace_ometer.data.db.dao.SeasonDao
import com.example.pace_ometer.data.db.entity.SeasonEntity
import kotlinx.coroutines.flow.Flow

class SeasonRepository(private val seasonDao: SeasonDao) {
    fun observeSeasons(): Flow<List<SeasonEntity>> = seasonDao.observeAll()

    suspend fun addSeason(name: String, startEpochMs: Long): Long =
        seasonDao.insert(SeasonEntity(name = name, startEpochMs = startEpochMs))

    suspend fun deleteSeason(season: SeasonEntity) = seasonDao.delete(season)
}
