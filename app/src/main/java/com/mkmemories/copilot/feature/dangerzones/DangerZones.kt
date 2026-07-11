package com.mkmemories.copilot.feature.dangerzones

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Zones de danger — implémentation conforme à l'article R413-15 du Code de la
 * route : on ne signale JAMAIS la position exacte d'un radar, uniquement un
 * segment de route (300 m en ville, 2 km sur route, 4 km sur autoroute) dans
 * lequel un danger peut se trouver.
 *
 * Source de données : open data officiel des radars fixes (data.gouv.fr),
 * transformé en zones conformes. Fonction activée uniquement dans les pays où
 * elle est légale (géofencing pays — interdite p. ex. en Suisse et Allemagne).
 */
data class DangerZone(
    val centerLatitude: Double,
    val centerLongitude: Double,
    val roadType: RoadType,
    val speedLimitKmh: Int?,
) {
    val radiusMeters: Int get() = roadType.zoneLengthMeters / 2
}

enum class RoadType(val zoneLengthMeters: Int, val label: String) {
    URBAN(300, "en ville"),
    ROAD(2_000, "sur route"),
    HIGHWAY(4_000, "sur autoroute"),
}

object RadarOpenDataRepository {
    // TODO v1 : télécharger et mettre en cache le jeu de données officiel
    // "Radars automatiques" de data.gouv.fr, puis le convertir en DangerZone.
    fun zones(): List<DangerZone> = emptyList()
}

/** Moteur d'alerte : à brancher sur le flux de localisation pendant la conduite. */
class ZoneAlertEngine(private val zones: () -> List<DangerZone>) {

    private var lastAlerted: DangerZone? = null

    /** Retourne le texte d'alerte TTS si on entre dans une nouvelle zone, sinon null. */
    fun onLocation(latitude: Double, longitude: Double): String? {
        val zone = zones().firstOrNull {
            distanceMeters(latitude, longitude, it.centerLatitude, it.centerLongitude) <= it.radiusMeters
        }
        if (zone == null || zone == lastAlerted) {
            if (zone == null) lastAlerted = null
            return null
        }
        lastAlerted = zone
        val limit = zone.speedLimitKmh?.let { ", limite à $it kilomètres heure" } ?: ""
        return "Zone de danger ${zone.roadType.label}$limit. Prudence."
    }

    companion object {
        /** Distance haversine en mètres. */
        fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val earthRadius = 6_371_000.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
            return earthRadius * 2 * atan2(sqrt(a), sqrt(1 - a))
        }
    }
}
