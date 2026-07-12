package com.mkmemories.copilot.feature.messaging

import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Messagerie apaisée en conduite — logique pure :
 *  - lecture vocale des SMS entrants ;
 *  - réponse automatique avec heure d'arrivée estimée, limitée à UNE réponse
 *    par contact par demi-heure (jamais de tempête de SMS).
 */
class DriveMessaging(private val replyIntervalMillis: Long = REPLY_INTERVAL_MS) {

    private val lastReplyAt = mutableMapOf<String, Long>()

    /** Texte à lire à voix haute pour un SMS entrant. */
    fun spokenAnnouncement(sender: String, body: String): String {
        val trimmed = if (body.length > MAX_SPOKEN_CHARS) body.take(MAX_SPOKEN_CHARS) + "… message tronqué" else body
        return "Message de $sender : $trimmed"
    }

    /** Réponse automatique à envoyer, ou null (déjà répondu récemment). */
    fun autoReply(sender: String, nowMillis: Long, eta: LocalTime?, nextStopName: String?): String? {
        // Premier message d'un contact : réponse immédiate, puis anti-tempête
        lastReplyAt[sender]?.let { if (nowMillis - it < replyIntervalMillis) return null }
        lastReplyAt[sender] = nowMillis

        val arrival = when {
            eta != null && nextStopName != null ->
                " J'arrive à $nextStopName vers ${eta.format(HOUR_FORMAT)}."
            eta != null -> " J'arrive vers ${eta.format(HOUR_FORMAT)}."
            else -> ""
        }
        return "Je conduis et je lirai votre message à l'arrêt.$arrival — Réponse automatique MK Copilot"
    }

    companion object {
        const val REPLY_INTERVAL_MS = 30 * 60_000L
        const val MAX_SPOKEN_CHARS = 220
        private val HOUR_FORMAT = DateTimeFormatter.ofPattern("HH'h'mm")
    }
}
