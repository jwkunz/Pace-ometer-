package com.example.pace_ometer.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-defined season boundary. A run belongs to the season with the latest [startEpochMs]
 * that is still <= the run's start time -- no explicit end date needed, since the next season's
 * start implicitly bounds the previous one.
 */
@Entity(tableName = "seasons")
data class SeasonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startEpochMs: Long
)
