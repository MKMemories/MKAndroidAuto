package com.mkmemories.copilot.feature.roadtrip

import com.mkmemories.copilot.feature.dangerzones.ZoneAlertEngine
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Départ intelligent : heure de départ conseillée pour le premier rendez-vous
 * du jour. Estimation locale : distance à vol d'oiseau × 1,3 (facteur route),
 * 65 km/h de moyenne mixte, + 15 min de marge.
 */
object DepartureAdvisor {

    /** Conseil pour le premier rendez-vous à venir, ou null. */
    fun advice(
        fromLat: Double,
        fromLng: Double,
        stops: List<TripStop>,
        now: LocalTime,
    ): String? {
        val target = stops.firstOrNull { !it.visited && it.time != null } ?: return null
        val time = target.time ?: return null
        if (time <= now) return null

        val travel = estimatedTravel(fromLat, fromLng, target.latitude, target.longitude)
        val departure = time.minus(travel).minusMinutes(MARGIN_MINUTES)
        if (departure <= now) {
            return "Pour être à ${target.name} à ${time.format(HOUR)}, il est temps de partir."
        }
        return "Pour ${target.name} à ${time.format(HOUR)}, départ conseillé à ${departure.format(HOUR)} " +
            "(${travel.toMinutes()} minutes de route estimées, marge comprise)."
    }

    internal fun estimatedTravel(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double): Duration {
        val meters = ZoneAlertEngine.distanceMeters(fromLat, fromLng, toLat, toLng) * ROAD_FACTOR
        val minutes = (meters / 1000.0) / AVG_SPEED_KMH * 60.0
        return Duration.ofMinutes(minutes.toLong().coerceAtLeast(1))
    }

    private const val ROAD_FACTOR = 1.3
    private const val AVG_SPEED_KMH = 65.0
    private const val MARGIN_MINUTES = 15L
    private val HOUR = DateTimeFormatter.ofPattern("HH'h'mm")
}
