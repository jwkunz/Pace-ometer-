package com.example.pace_ometer.tts

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech

/** Wraps android.speech.tts.TextToSpeech's async-init lifecycle for queued run announcements. */
class TtsAnnouncer(context: Context) {

    private var ready = false
    private val pendingPhrases = mutableListOf<String>()

    private var tts: TextToSpeech? = TextToSpeech(context) { status ->
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            tts?.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            pendingPhrases.forEach { speakNow(it) }
            pendingPhrases.clear()
        }
    }

    fun speakAll(phrases: List<String>) {
        phrases.forEach { phrase -> if (ready) speakNow(phrase) else pendingPhrases.add(phrase) }
    }

    private fun speakNow(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, text.hashCode().toString())
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
