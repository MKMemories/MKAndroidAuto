package com.mkmemories.copilot.feature.voice

import java.text.Normalizer

/**
 * Commandes vocales — grammaire pure, tolérante aux accents et aux
 * formulations naturelles. La reconnaissance (SpeechRecognizer) fournit le
 * texte ; ici on décide de l'action.
 */
enum class VoiceCommand {
    NEXT_STOP, // « prochaine étape » → navigation
    WHERE_IS_CAR, // « où est ma voiture » → guidage parking
    NOTIFY_ARRIVAL, // « préviens que j'arrive » → SMS aux proches
    PLAY_BRIEFING, // « briefing » / « météo »
}

object VoiceCommands {

    fun match(utterance: String): VoiceCommand? {
        val text = normalize(utterance)
        return when {
            listOf("prochaine etape", "etape suivante", "navigue", "en route").any { it in text } ->
                VoiceCommand.NEXT_STOP
            listOf("ou est ma voiture", "ou est la voiture", "ma voiture", "parking").any { it in text } ->
                VoiceCommand.WHERE_IS_CAR
            listOf("previens", "j arrive", "jarrive", "dis que j arrive").any { it in text } ->
                VoiceCommand.NOTIFY_ARRIVAL
            listOf("briefing", "meteo", "quel temps").any { it in text } ->
                VoiceCommand.PLAY_BRIEFING
            else -> null
        }
    }

    internal fun normalize(utterance: String): String =
        Normalizer.normalize(utterance.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}
