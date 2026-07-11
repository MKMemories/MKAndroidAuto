package com.mkmemories.copilot.feature.roadtrip

import java.time.LocalDate
import java.time.LocalTime

/**
 * Modèle du "mode Road Trip" : un voyage planifié sur plusieurs jours,
 * avec des étapes précises que l'app séquence automatiquement
 * (navigation Google Maps lancée étape par étape depuis l'écran voiture).
 */
data class Trip(
    val name: String,
    val days: List<TripDay>,
) {
    /** Les étapes du jour courant, celles que l'écran Android Auto affiche. */
    fun stopsFor(date: LocalDate): List<TripStop> =
        days.firstOrNull { it.date == date }?.stops.orEmpty()
}

data class TripDay(
    val date: LocalDate,
    val stops: List<TripStop>,
)

data class TripStop(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val visited: Boolean = false,
    /** Ville / île / région du lieu (issue de l'autocomplétion) — sert à enrichir le PDF. */
    val locality: String? = null,
    /** Heure de rendez-vous, optionnelle (visite guidée, ferry, restaurant…). */
    val time: LocalTime? = null,
)
