package com.mkmemories.copilot.feature.roadtrip

import android.content.Context
import java.time.LocalDate

/**
 * Source des voyages : le voyage planifié par l'utilisateur ([TripStore])
 * s'il existe, sinon un voyage de démonstration.
 * TODO v1.1 : multi-voyages + import KML/KMZ (Google My Maps), GPX et CSV.
 */
object TripRepository {

    fun currentTrip(context: Context): Trip = TripStore(context).load() ?: demoTrip()

    internal fun demoTrip(): Trip {
        val today = LocalDate.now()
        return Trip(
            name = "Châteaux de la Loire",
            days = listOf(
                TripDay(
                    date = today,
                    stops = listOf(
                        TripStop("Château de Chambord", 47.6161, 1.5170, locality = "Chambord"),
                        TripStop("Château de Cheverny", 47.5006, 1.4579, locality = "Cheverny"),
                        TripStop("Hôtel — Blois", 47.5861, 1.3359, locality = "Blois"),
                    ),
                ),
                TripDay(
                    date = today.plusDays(1),
                    stops = listOf(
                        TripStop("Château de Chenonceau", 47.3249, 1.0704, locality = "Chenonceaux"),
                        TripStop("Amboise", 47.4131, 0.9846, locality = "Amboise"),
                    ),
                ),
            ),
        )
    }
}
