package com.mkmemories.copilot.feature.carnet

import com.mkmemories.copilot.feature.roadtrip.Trip
import com.mkmemories.copilot.feature.roadtrip.TripDay
import com.mkmemories.copilot.feature.roadtrip.TripStop
import java.time.LocalDate

/**
 * Convertit le carnet en [Trip] navigable : chaque trajet voiture devient une
 * étape géolocalisée, regroupée par date. C'est ce voyage qui est poussé dans
 * [com.mkmemories.copilot.feature.roadtrip.TripStore], donc consommé tel quel
 * par l'accueil, l'écran Android Auto et le service de conduite — la navigation
 * s'alimente jour après jour, sans double saisie.
 */
fun CarnetVoyage.toTrip(): Trip {
    val days = drives
        .groupBy { it.date }
        .toSortedMap()
        .map { (date, list) ->
            TripDay(
                date = date,
                stops = list.map { entry ->
                    val place = entry.destination!!
                    TripStop(
                        name = place.name,
                        latitude = place.latitude,
                        longitude = place.longitude,
                        locality = place.locality,
                        time = entry.time,
                    )
                },
            )
        }
    return Trip(name = title, days = days)
}

/** Prochain jour de trajets à partir d'une date (utile pour l'accueil avant/pendant le voyage). */
fun CarnetVoyage.nextDriveDay(from: LocalDate): TripDay? =
    toTrip().days.firstOrNull { !it.date.isBefore(from) }
