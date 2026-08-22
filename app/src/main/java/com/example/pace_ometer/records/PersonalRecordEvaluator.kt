package com.example.pace_ometer.records

import com.example.pace_ometer.data.db.entity.PersonalRecordCategory

/**
 * Computes personal-record candidates for a completed run. "Best split" categories (fastest 1K/
 * mile/5K/10K) find the minimum-duration contiguous window anywhere within the run whose
 * cumulative distance spans at least the target distance -- a standard sliding-window "best
 * effort" search -- so e.g. a 10K run can also set a fastest-5K record from its best 5K split,
 * not just runs that exactly match a category's distance.
 */
object PersonalRecordEvaluator {

    data class Candidate(val category: String, val value: Double)

    private data class DistanceTarget(val category: String, val meters: Double)

    private val distanceTargets = listOf(
        DistanceTarget(PersonalRecordCategory.FASTEST_1K, 1000.0),
        DistanceTarget(PersonalRecordCategory.FASTEST_MILE, 1609.344),
        DistanceTarget(PersonalRecordCategory.FASTEST_5K, 5000.0),
        DistanceTarget(PersonalRecordCategory.FASTEST_10K, 10000.0)
    )

    private const val MIN_DISTANCE_FOR_OVERALL_PACE_METERS = 1000.0

    /**
     * @param samples ascending (timestampMs, cumulativeDistanceMeters) pairs for the run.
     * @return one candidate per category the run has enough data to evaluate; value is seconds
     *   for pace/time categories, meters for [PersonalRecordCategory.LONGEST_DISTANCE].
     */
    fun evaluate(
        totalDistanceMeters: Double,
        movingDurationMs: Long,
        samples: List<Pair<Long, Double>>
    ): List<Candidate> {
        val candidates = mutableListOf(Candidate(PersonalRecordCategory.LONGEST_DISTANCE, totalDistanceMeters))

        if (totalDistanceMeters >= MIN_DISTANCE_FOR_OVERALL_PACE_METERS && movingDurationMs > 0) {
            val avgPaceSecPerKm = (movingDurationMs / 1000.0) / (totalDistanceMeters / 1000.0)
            candidates += Candidate(PersonalRecordCategory.FASTEST_OVERALL_PACE, avgPaceSecPerKm)
        }

        for (target in distanceTargets) {
            bestSplitDurationMs(samples, target.meters)?.let { durationMs ->
                candidates += Candidate(target.category, durationMs / 1000.0)
            }
        }
        return candidates
    }

    /** Minimum-duration window (ms) covering at least [targetMeters], or null if none reaches it. */
    private fun bestSplitDurationMs(samples: List<Pair<Long, Double>>, targetMeters: Double): Long? {
        if (samples.size < 2) return null
        var best: Long? = null
        var start = 0
        for (end in samples.indices) {
            while (start < end && samples[end].second - samples[start + 1].second >= targetMeters) {
                start++
            }
            val windowDistance = samples[end].second - samples[start].second
            if (windowDistance >= targetMeters) {
                val duration = samples[end].first - samples[start].first
                if (best == null || duration < best) best = duration
            }
        }
        return best
    }
}
