package com.example.pace_ometer.data

/**
 * What kind of activity a run/session is. Stored on [com.example.pace_ometer.data.db.entity.RunEntity]
 * as this enum's [name] (plain TEXT column, matching how UserSettings enums are already persisted).
 *
 * [noun] is the word used in spoken/written phrasing ("Starting your $noun", "Ready to $noun?").
 * [usesStepSensing] gates step-detector/dead-reckoning and step-derived metrics (cadence in
 * steps/min, stride length, step count) -- meaningless for an activity with no discrete steps.
 * [usesPaceDisplay] controls whether live/announced speed is shown as running-style pace
 * (min:sec per distance unit) or as a raw speed (distance unit per hour); cycling speeds read
 * far more naturally as the latter.
 */
enum class ActivityType(
    val displayName: String,
    val noun: String,
    val usesStepSensing: Boolean,
    val usesPaceDisplay: Boolean
) {
    RUNNING("Running", "run", usesStepSensing = true, usesPaceDisplay = true),
    WALKING("Walking", "walk", usesStepSensing = true, usesPaceDisplay = true),
    CYCLING("Cycling", "ride", usesStepSensing = false, usesPaceDisplay = false)
}
