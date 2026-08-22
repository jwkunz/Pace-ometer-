package com.example.pace_ometer.tts

/**
 * Fires whenever cumulative distance crosses the next multiple of the configured interval
 * (a simple threshold/watermark check) -- distance-triggered, not time-triggered.
 */
class AnnouncementScheduler(private val intervalMeters: Double) {
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

    fun reset() {
        nextTriggerMeters = intervalMeters
    }
}
