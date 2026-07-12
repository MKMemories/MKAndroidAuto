package com.mkmemories.copilot.feature.guardian

import java.util.Locale
import kotlin.math.roundToInt

/** Formatage lisible pour l'écran « Ma voiture ». Logique pure, testable. */
object ParkingFormat {

    /** « à l'instant », « il y a 25 min », « il y a 2 h 05 ». */
    fun parkedAgo(nowMillis: Long, savedMillis: Long): String {
        val minutes = ((nowMillis - savedMillis) / 60_000L).coerceAtLeast(0)
        return when {
            minutes < 1 -> "à l'instant"
            minutes < 60 -> "il y a $minutes min"
            else -> {
                val hours = minutes / 60
                val rest = minutes % 60
                "il y a $hours h" + if (rest > 0) " ${rest.toString().padStart(2, '0')}" else ""
            }
        }
    }

    /** « 320 m » ou « 1,2 km ». */
    fun distanceLabel(meters: Double): String =
        if (meters < 1_000) "${meters.roundToInt()} m"
        else String.format(Locale.FRENCH, "%.1f km", meters / 1_000)

    /** Coordonnées lisibles et copiables, ex. « 48.856600, 2.352200 ». */
    fun coordinates(latitude: Double, longitude: Double): String =
        String.format(Locale.US, "%.6f, %.6f", latitude, longitude)

    /**
     * Vrai lien Google Maps (place épinglée), ouvrable et partageable.
     * Ex. https://www.google.com/maps/search/?api=1&query=48.856600,2.352200
     */
    fun googleMapsUrl(latitude: Double, longitude: Double): String =
        String.format(Locale.US, "https://www.google.com/maps/search/?api=1&query=%.6f,%.6f", latitude, longitude)
}
