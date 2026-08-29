package com.example.pace_ometer.tts

import com.example.pace_ometer.data.settings.UserSettings
import com.example.pace_ometer.util.AgeAndHrZoneCalculator
import com.example.pace_ometer.util.averagePaceSecPerKm
import com.example.pace_ometer.util.formatDistanceMeters
import com.example.pace_ometer.util.formatDurationMs
import com.example.pace_ometer.util.formatElevationMeters
import com.example.pace_ometer.util.speakablePaceSecPerKm
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class AnnouncementSnapshot(
    val distanceMeters: Double,
    val elapsedDurationMs: Long,
    val elevationMeters: Double?,
    val elevationChangeLastSegmentMeters: Double?,
    val heartRateBpm: Int?,
    val cadenceSpm: Int?,
    val segmentPaceSecPerKm: Double?,
    val splitPaceSecPerKm: Double?,
    val cumulativeCalories: Double?,
    val clockTimeEpochMs: Long
)

/** Assembles the spoken phrases for one announcement, in a fixed order, honoring per-item toggles. */
object AnnouncementContentBuilder {

    fun build(settings: UserSettings, snapshot: AnnouncementSnapshot): List<String> {
        val phrases = mutableListOf<String>()
        val unit = settings.unitSystem

        if (settings.announceDistance) {
            phrases += "Distance: ${formatDistanceMeters(snapshot.distanceMeters, unit)}"
        }
        if (settings.announceElapsedTime) {
            phrases += "Time: ${formatDurationMs(snapshot.elapsedDurationMs)}"
        }
        if (settings.announceSegmentPace) {
            speakablePaceSecPerKm(snapshot.segmentPaceSecPerKm, unit)?.let { phrases += "Current pace: $it" }
        }
        if (settings.announceSplitPace) {
            speakablePaceSecPerKm(snapshot.splitPaceSecPerKm, unit)?.let { phrases += "Projected split pace: $it" }
        }
        if (settings.announceAveragePace) {
            val avgPace = averagePaceSecPerKm(snapshot.distanceMeters, snapshot.elapsedDurationMs)
            speakablePaceSecPerKm(avgPace, unit)?.let { phrases += "Average pace: $it" }
        }
        if (settings.announceElevation && snapshot.elevationMeters != null) {
            phrases += "Elevation: ${formatElevationMeters(snapshot.elevationMeters, unit)}"
        }
        if (settings.announceElevationChangeLastSegment && snapshot.elevationChangeLastSegmentMeters != null) {
            val change = snapshot.elevationChangeLastSegmentMeters
            val direction = if (change >= 0) "up" else "down"
            phrases += "Last segment: $direction ${formatElevationMeters(kotlin.math.abs(change), unit)}"
        }
        if (settings.announceHeartRate && snapshot.heartRateBpm != null) {
            phrases += "Heart rate: ${snapshot.heartRateBpm} beats per minute"
        }
        val birthDateEpochDay = settings.birthDateEpochDay
        if (settings.announceHeartRateZone && snapshot.heartRateBpm != null && birthDateEpochDay != null) {
            val maxHr = AgeAndHrZoneCalculator.estimatedMaxHeartRateBpm(
                AgeAndHrZoneCalculator.ageYears(birthDateEpochDay)
            )
            val zone = AgeAndHrZoneCalculator.zoneFor(snapshot.heartRateBpm, maxHr)
            phrases += if (zone != null) "Heart rate zone: ${zone.number}" else "Heart rate zone: below zone 1"
        }
        if (settings.announceCadence && snapshot.cadenceSpm != null) {
            phrases += "Cadence: ${snapshot.cadenceSpm} steps per minute"
        }
        if (settings.announceCalories && snapshot.cumulativeCalories != null) {
            phrases += "Calories: ${snapshot.cumulativeCalories.roundToInt()}"
        }
        if (settings.announceClockTime) {
            val formatted = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(snapshot.clockTimeEpochMs))
            phrases += "Time now: $formatted"
        }
        return phrases
    }
}
