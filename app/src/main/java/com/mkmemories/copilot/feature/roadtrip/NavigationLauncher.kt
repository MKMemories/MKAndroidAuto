package com.mkmemories.copilot.feature.roadtrip

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.car.app.CarContext

/**
 * Interaction avec Google Maps : l'app orchestre le voyage,
 * Maps fait le guidage (handoff officiel, aucune API payante).
 */
object NavigationLauncher {

    /** Depuis l'écran voiture : bascule Android Auto sur l'app de navigation par défaut. */
    fun navigateFromCar(carContext: CarContext, stop: TripStop) {
        val uri = Uri.parse("geo:${stop.latitude},${stop.longitude}?q=${Uri.encode(stop.name)}")
        carContext.startCarApp(Intent(CarContext.ACTION_NAVIGATE, uri))
    }

    /** Depuis le téléphone : guidage immédiat vers une étape. */
    fun navigateFromPhone(context: Context, stop: TripStop) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("google.navigation:q=${stop.latitude},${stop.longitude}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Depuis le téléphone : itinéraire complet d'une journée dans Maps
     * (l'URL officielle accepte jusqu'à 9 étapes intermédiaires).
     */
    fun dayItineraryUrl(stops: List<TripStop>): Uri = Uri.parse(dayItineraryUrlString(stops))

    /** Construction pure de l'URL Maps, le séparateur d'étapes `|` encodé en %7C. */
    internal fun dayItineraryUrlString(stops: List<TripStop>): String {
        require(stops.isNotEmpty()) { "Aucune étape pour ce jour" }
        val destination = stops.last()
        val waypoints = stops.dropLast(1).take(9)
            .joinToString("%7C") { "${it.latitude},${it.longitude}" }
        return buildString {
            append("https://www.google.com/maps/dir/?api=1")
            append("&destination=${destination.latitude},${destination.longitude}")
            if (waypoints.isNotEmpty()) append("&waypoints=$waypoints")
            append("&travelmode=driving")
        }
    }
}
