package com.mkmemories.copilot.feature.guide

import com.mkmemories.copilot.feature.dangerzones.ZoneAlertEngine

/**
 * Guide du territoire — cadence pure du narrateur : au plus une histoire
 * toutes les [minIntervalMillis], seulement si on a parcouru au moins
 * [minDistanceMeters] depuis la précédente, et jamais deux fois le même site.
 */
class TouristGuide(
    private val minIntervalMillis: Long = MIN_INTERVAL_MS,
    private val minDistanceMeters: Double = MIN_DISTANCE_M,
) {

    private var lastTold: Long? = null
    private var lastLat: Double? = null
    private var lastLng: Double? = null
    private val toldTitles = mutableSetOf<String>()

    /** Peut-on raconter quelque chose ici et maintenant ? */
    fun shouldNarrate(nowMillis: Long, latitude: Double, longitude: Double): Boolean {
        // Première histoire : dès le début du trajet, puis cadence stricte
        lastTold?.let { if (nowMillis - it < minIntervalMillis) return false }
        val lat = lastLat
        val lng = lastLng
        if (lat != null && lng != null &&
            ZoneAlertEngine.distanceMeters(latitude, longitude, lat, lng) < minDistanceMeters
        ) {
            return false
        }
        return true
    }

    /** Choisit un site jamais raconté ; enregistre la narration. */
    fun pick(nowMillis: Long, latitude: Double, longitude: Double, candidates: List<String>): String? {
        val fresh = candidates.firstOrNull { it !in toldTitles } ?: return null
        toldTitles.add(fresh)
        lastTold = nowMillis
        lastLat = latitude
        lastLng = longitude
        return fresh
    }

    companion object {
        const val MIN_INTERVAL_MS = 10 * 60_000L
        const val MIN_DISTANCE_M = 5_000.0
    }
}
