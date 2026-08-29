package com.example.pace_ometer.util

fun formatDurationMs(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

/**
 * Speech-friendly duration phrasing, e.g. "5 minutes 30 seconds". Deliberately avoids
 * [formatDurationMs]'s "M:SS" display format: Android's TTS text-normalizer reads that shape as a
 * clock time (speaking "5:00" as "five o'clock") rather than a duration.
 */
fun speakableDurationMs(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val parts = mutableListOf<String>()
    if (hours > 0) parts += "$hours hour${if (hours == 1L) "" else "s"}"
    if (minutes > 0) parts += "$minutes minute${if (minutes == 1L) "" else "s"}"
    if (seconds > 0 || parts.isEmpty()) parts += "$seconds second${if (seconds == 1L) "" else "s"}"
    return parts.joinToString(" ")
}
