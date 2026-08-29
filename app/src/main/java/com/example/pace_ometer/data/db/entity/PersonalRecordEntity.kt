package com.example.pace_ometer.data.db.entity

import androidx.room.Entity

object PersonalRecordScope {
    const val ALL_TIME = "ALL_TIME"
    fun season(seasonId: Long) = "SEASON_$seasonId"
}

object PersonalRecordCategory {
    const val FASTEST_1K = "FASTEST_1K"
    const val FASTEST_5K = "FASTEST_5K"
    const val FASTEST_10K = "FASTEST_10K"
    const val FASTEST_MILE = "FASTEST_MILE"
    const val LONGEST_DISTANCE = "LONGEST_DISTANCE"
    const val FASTEST_OVERALL_PACE = "FASTEST_OVERALL_PACE"
}

/**
 * One row per (category, scope, activityType) triple -- the current best value for that
 * category, within that scope (either [PersonalRecordScope.ALL_TIME] or a specific season), for
 * that activity. Activity type is part of the identity (not just a filter) because the
 * categories aren't comparable across activities -- e.g. a cycling "Fastest 5K" is trivially
 * always faster than a running one, so mixing them into one record would permanently blow away
 * the running PR the moment a bike ride is saved. [value] is seconds for pace/time-based
 * categories, meters for distance-based ones.
 */
@Entity(tableName = "personal_records", primaryKeys = ["category", "scope", "activityType"])
data class PersonalRecordEntity(
    val category: String,
    val scope: String,
    val activityType: String,
    val value: Double,
    val runId: Long,
    val achievedAtEpochMs: Long
)
