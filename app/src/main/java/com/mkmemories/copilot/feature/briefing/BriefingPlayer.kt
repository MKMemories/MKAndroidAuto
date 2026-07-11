package com.mkmemories.copilot.feature.briefing

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Lecture vocale du briefing via le moteur TTS natif d'Android (gratuit) —
 * le son sort dans les haut-parleurs de la voiture quand le téléphone est
 * connecté à Android Auto.
 */
class BriefingPlayer(context: Context) {

    private var ready = false
    private var pendingText: String? = null

    private val tts = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            ready = true
            pendingText?.let { speak(it) }
            pendingText = null
        }
    }.also { it.language = Locale.FRENCH }

    fun speak(text: String) {
        if (!ready) {
            pendingText = text
            return
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "briefing")
    }

    fun release() {
        tts.stop()
        tts.shutdown()
    }
}
