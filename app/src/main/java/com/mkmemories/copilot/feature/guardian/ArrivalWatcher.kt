package com.mkmemories.copilot.feature.guardian

import com.mkmemories.copilot.feature.dangerzones.ZoneAlertEngine
import com.mkmemories.copilot.feature.roadtrip.TripStop

/**
 * Géofencing d'arrivée — logique pure, alimentée par le flux de position du
 * service de conduite. Quand on entre dans le rayon d'une étape non visitée,
 * elle est déclarée atteinte UNE seule fois (anti-rebond : il faut ressortir
 * du rayon avant qu'une nouvelle arrivée à la même étape soit possible).
 */
class ArrivalWatcher(private val radiusMeters: Double = DEFAULT_RADIUS_METERS) {

    private var lastReachedKey: String? = null

    /** Retourne l'étape atteinte par cette position, ou null. */
    fun onLocation(latitude: Double, longitude: Double, stops: List<TripStop>): TripStop? {
        val reached = stops.firstOrNull { stop ->
            !stop.visited &&
                ZoneAlertEngine.distanceMeters(latitude, longitude, stop.latitude, stop.longitude) <= radiusMeters
        }
        val key = reached?.let { "${it.name}@${it.latitude},${it.longitude}" }
        if (key == lastReachedKey) return null // toujours dans le même rayon : pas de doublon
        lastReachedKey = key
        return reached
    }

    companion object {
        const val DEFAULT_RADIUS_METERS = 150.0
    }
}
