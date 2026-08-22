package com.example.pace_ometer.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A piece of gear (running shoes, a bike, etc.) whose cumulative usage the user wants tracked
 * across saved runs. [startingDistanceMeters] lets the user account for mileage the item already
 * had before it was added to the app.
 */
@Entity(tableName = "equipment")
data class EquipmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val startingDistanceMeters: Double = 0.0,
    val createdAtEpochMs: Long,
    val retired: Boolean = false,
    val retiredAtEpochMs: Long? = null
)
