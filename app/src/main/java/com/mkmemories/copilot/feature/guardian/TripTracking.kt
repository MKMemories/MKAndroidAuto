package com.mkmemories.copilot.feature.guardian

import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * « Voyage suivi » — le partage de trajet zéro-cloud : un SMS de position aux
 * proches à intervalle régulier pendant la conduite. Logique pure, cadencée
 * par le flux de position du service.
 */
class TripTracking(private val intervalMillis: Long = DEFAULT_INTERVAL_MS) {

    private var lastSentAt: Long? = null

    /** Retourne le message à envoyer maintenant, ou null si trop tôt. */
    fun onLocation(
        nowMillis: Long,
        latitude: Double,
        longitude: Double,
        nextStopName: String?,
        time: LocalTime,
    ): String? {
        // Premier point du trajet : envoi immédiat, puis cadence régulière
        lastSentAt?.let { if (nowMillis - it < intervalMillis) return null }
        lastSentAt = nowMillis
        val eta = nextStopName?.let { " En route vers $it." } ?: ""
        val clock = time.format(DateTimeFormatter.ofPattern("HH'h'mm"))
        return "MK Copilot ($clock) : tout va bien, position " +
            "https://maps.google.com/?q=$latitude,$longitude.$eta"
    }

    companion object {
        const val DEFAULT_INTERVAL_MS = 30 * 60_000L
    }
}
