package com.mkmemories.copilot.feature.roadtrip

import java.time.LocalDate

/**
 * Source des voyages. V1 : voyage de démonstration en mémoire.
 * TODO v1.1 : persistance locale (Room) + import KML/KMZ (Google My Maps), GPX et CSV.
 */
object TripRepository {

    fun currentTrip(): Trip = demoTrip()

    private fun demoTrip(): Trip {
        val today = LocalDate.now()
        return Trip(
            name = "Châteaux de la Loire",
            days = listOf(
                TripDay(
                    date = today,
                    stops = listOf(
                        TripStop("Château de Chambord", 47.6161, 1.5170),
                        TripStop("Château de Cheverny", 47.5006, 1.4579),
                        TripStop("Hôtel — Blois", 47.5861, 1.3359),
                    ),
                ),
                TripDay(
                    date = today.plusDays(1),
                    stops = listOf(
                        TripStop("Château de Chenonceau", 47.3249, 1.0704),
                        TripStop("Amboise", 47.4131, 0.9846),
                    ),
                ),
            ),
        )
    }
}
