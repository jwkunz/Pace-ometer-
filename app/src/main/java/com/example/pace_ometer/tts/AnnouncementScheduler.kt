package com.example.pace_ometer.tts

/**
 * Fires whenever cumulative distance crosses the next multiple of the configured interval
 * (a simple threshold/watermark check) -- distance-triggered, not time-triggered.
 */
class AnnouncementScheduler(private var intervalMeters: Double) {
    private var nextTriggerMeters: Double = intervalMeters

    /** Returns true if [cumulativeDistanceMeters] has crossed one or more trigger points, advancing past all of them. */
    fun checkAndAdvance(cumulativeDistanceMeters: Double): Boolean {
        if (intervalMeters <= 0) return false
        var triggered = false
        while (cumulativeDistanceMeters >= nextTriggerMeters) {
            nextTriggerMeters += intervalMeters
            triggered = true
        }
        return triggered
    }

    /** The next whole-interval distance the split/projected pace should target. */
    fun nextTriggerDistanceMeters(): Double = nextTriggerMeters

    /**
     * Applies a mid-run change to the announcement interval (e.g. the user edits it in Settings
     * while a run is active), re-targeting the next trigger from the current distance so the
     * new interval takes effect immediately instead of waiting for the run to end.
     */
    fun updateInterval(newIntervalMeters: Double, currentDistanceMeters: Double) {
        if (newIntervalMeters == intervalMeters) return
        intervalMeters = newIntervalMeters
        nextTriggerMeters = currentDistanceMeters + newIntervalMeters
    }

    fun reset() {
        nextTriggerMeters = intervalMeters
    }
}
