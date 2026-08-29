package com.example.pace_ometer.util

import com.example.pace_ometer.data.settings.UnitSystem
import kotlin.math.roundToInt

private const val METERS_PER_MILE = 1609.344
private const val METERS_PER_KM = 1000.0

fun metersToDisplayUnitDistance(meters: Double, unitSystem: UnitSystem): Double =
    if (unitSystem == UnitSystem.IMPERIAL) meters / METERS_PER_MILE else meters / METERS_PER_KM

fun displayUnitDistanceToMeters(value: Double, unitSystem: UnitSystem): Double =
    if (unitSystem == UnitSystem.IMPERIAL) value * METERS_PER_MILE else value * METERS_PER_KM

fun formatDistanceMeters(meters: Double, unitSystem: UnitSystem = UnitSystem.METRIC): String {
    val value = metersToDisplayUnitDistance(meters, unitSystem)
    val unit = if (unitSystem == UnitSystem.IMPERIAL) "mi" else "km"
    return "%.2f %s".format(value, unit)
}

/** Average pace (sec/km) over the whole run so far, or null before any distance has accrued. */
fun averagePaceSecPerKm(distanceMeters: Double, durationMs: Long): Double? =
    if (distanceMeters > 0) (durationMs / 1000.0) / (distanceMeters / 1000.0) else null

/** Formats a pace given in seconds-per-km into the display unit's "m:ss / unit" form. */
fun formatPaceSecPerKm(secPerKm: Double?, unitSystem: UnitSystem = UnitSystem.METRIC): String {
    if (secPerKm == null || secPerKm.isInfinite() || secPerKm.isNaN()) return "--:--"
    val secPerUnit = if (unitSystem == UnitSystem.IMPERIAL) secPerKm * (METERS_PER_MILE / METERS_PER_KM) else secPerKm
    val totalSeconds = secPerUnit.roundToInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val unitLabel = if (unitSystem == UnitSystem.IMPERIAL) "mi" else "km"
    return "%d:%02d /%s".format(minutes, seconds, unitLabel)
}

/**
 * Speech-friendly pace phrasing, e.g. "6 minutes 30 seconds per kilometer". Deliberately avoids
 * the "6:30" display format: Android's TTS text-normalizer reads an "M:SS"-shaped string as a
 * clock time (speaking "6:00" as "six o'clock") rather than a duration.
 */
fun speakablePaceSecPerKm(secPerKm: Double?, unitSystem: UnitSystem = UnitSystem.METRIC): String? {
    if (secPerKm == null || secPerKm.isInfinite() || secPerKm.isNaN()) return null
    val secPerUnit = if (unitSystem == UnitSystem.IMPERIAL) secPerKm * (METERS_PER_MILE / METERS_PER_KM) else secPerKm
    val totalSeconds = secPerUnit.roundToInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val unitLabel = if (unitSystem == UnitSystem.IMPERIAL) "mile" else "kilometer"
    val minutePart = "$minutes minute${if (minutes == 1) "" else "s"}"
    val secondPart = if (seconds > 0) " $seconds second${if (seconds == 1) "" else "s"}" else ""
    return "$minutePart$secondPart per $unitLabel"
}

/** Formats a pace given in seconds-per-km as a speed in the display unit's "X.X km/h"/"X.X mph" form -- the natural way to express cycling speed, vs. running-style min:sec pace. */
fun formatSpeedFromPaceSecPerKm(secPerKm: Double?, unitSystem: UnitSystem = UnitSystem.METRIC): String {
    val unitLabel = if (unitSystem == UnitSystem.IMPERIAL) "mph" else "km/h"
    if (secPerKm == null || secPerKm <= 0 || secPerKm.isInfinite() || secPerKm.isNaN()) return "-- $unitLabel"
    val speedKmh = 3600.0 / secPerKm
    val value = if (unitSystem == UnitSystem.IMPERIAL) speedKmh / (METERS_PER_MILE / METERS_PER_KM) else speedKmh
    return "%.1f %s".format(value, unitLabel)
}

/** Speech-friendly speed phrasing (e.g. "24.1 kilometers per hour"), for activities like cycling
 *  where speed reads more naturally than running-style pace. */
fun speakableSpeedFromPaceSecPerKm(secPerKm: Double?, unitSystem: UnitSystem = UnitSystem.METRIC): String? {
    if (secPerKm == null || secPerKm <= 0 || secPerKm.isInfinite() || secPerKm.isNaN()) return null
    val speedKmh = 3600.0 / secPerKm
    val value = if (unitSystem == UnitSystem.IMPERIAL) speedKmh / (METERS_PER_MILE / METERS_PER_KM) else speedKmh
    val unitLabel = if (unitSystem == UnitSystem.IMPERIAL) "miles per hour" else "kilometers per hour"
    return "%.1f %s".format(value, unitLabel)
}

fun kgToDisplayWeight(kg: Float, unitSystem: UnitSystem): Float =
    if (unitSystem == UnitSystem.IMPERIAL) kg * 2.20462f else kg

fun displayWeightToKg(value: Float, unitSystem: UnitSystem): Float =
    if (unitSystem == UnitSystem.IMPERIAL) value / 2.20462f else value

fun metersToDisplayElevation(meters: Double, unitSystem: UnitSystem): Double =
    if (unitSystem == UnitSystem.IMPERIAL) meters * 3.28084 else meters

fun formatElevationMeters(meters: Double, unitSystem: UnitSystem = UnitSystem.METRIC): String {
    val value = metersToDisplayElevation(meters, unitSystem)
    val unit = if (unitSystem == UnitSystem.IMPERIAL) "ft" else "m"
    return "%.0f %s".format(value, unit)
}

fun formatStrideLengthMeters(meters: Double, unitSystem: UnitSystem): String {
    val value = metersToDisplayElevation(meters, unitSystem)
    val unit = if (unitSystem == UnitSystem.IMPERIAL) "ft" else "m"
    return "%.2f %s".format(value, unit)
}

/** Steps per distance unit (e.g. "1780 /mi"), a stride-rate metric independent of pace. */
fun formatStepsPerDistanceUnit(steps: Int, distanceMeters: Double, unitSystem: UnitSystem): String {
    if (distanceMeters <= 0) return "--"
    val distanceInUnit = metersToDisplayUnitDistance(distanceMeters, unitSystem)
    val unitLabel = if (unitSystem == UnitSystem.IMPERIAL) "mi" else "km"
    return "%.0f /%s".format(steps / distanceInUnit, unitLabel)
}

fun cmToDisplayHeight(cm: Float, unitSystem: UnitSystem): Float =
    if (unitSystem == UnitSystem.IMPERIAL) cm / 2.54f else cm

fun displayHeightToCm(value: Float, unitSystem: UnitSystem): Float =
    if (unitSystem == UnitSystem.IMPERIAL) value * 2.54f else value
