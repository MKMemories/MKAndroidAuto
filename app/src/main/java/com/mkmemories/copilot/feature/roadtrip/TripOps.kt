package com.mkmemories.copilot.feature.roadtrip

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Opérations pures d'édition du voyage — utilisées par le planificateur. */

fun Trip.withStop(date: LocalDate, stop: TripStop): Trip {
    val existing = days.firstOrNull { it.date == date }
    val newDays =
        if (existing == null) days + TripDay(date, listOf(stop))
        else days.map { if (it.date == date) it.copy(stops = it.stops + stop) else it }
    return copy(days = newDays.sortedBy { it.date })
}

fun Trip.withoutStop(date: LocalDate, stop: TripStop): Trip =
    copy(
        days = days
            .map { if (it.date == date) it.copy(stops = it.stops - stop) else it }
            .filter { it.stops.isNotEmpty() },
    )

/** Modifie une étape en place (heure de rendez-vous, nom…) sans changer l'ordre. */
fun Trip.withUpdatedStop(date: LocalDate, index: Int, transform: (TripStop) -> TripStop): Trip =
    copy(
        days = days.map { day ->
            if (day.date != date || index !in day.stops.indices) return@map day
            day.copy(
                stops = day.stops.mapIndexed { i, stop -> if (i == index) transform(stop) else stop },
            )
        },
    )

fun Trip.withMovedStop(date: LocalDate, index: Int, delta: Int): Trip =
    copy(
        days = days.map { day ->
            if (day.date != date) return@map day
            val target = index + delta
            if (index !in day.stops.indices || target !in day.stops.indices) return@map day
            val stops = day.stops.toMutableList()
            val moved = stops.removeAt(index)
            stops.add(target, moved)
            day.copy(stops = stops)
        },
    )

/** "09h30" — format français court pour l'UI, le PDF et l'écran voiture. */
fun TripStop.timeLabel(): String? = time?.format(DateTimeFormatter.ofPattern("HH'h'mm"))

/**
 * Phrase des étapes du jour, prête pour la synthèse vocale — ajoutée au
 * briefing météo : "Au programme aujourd'hui, 3 étapes : X à 9 heures 30,
 * puis Y, puis Z. "
 */
object DayBriefing {

    fun forStops(stops: List<TripStop>): String? {
        if (stops.isEmpty()) return null
        val remaining = stops.filter { !it.visited }.ifEmpty { return "Toutes les étapes du jour sont visitées. Bravo !" }
        val parts = remaining.map { stop ->
            val spokenTime = stop.time?.let { time ->
                " à ${time.hour} heure${if (time.hour > 1) "s" else ""}" +
                    if (time.minute > 0) " ${time.minute}" else ""
            } ?: ""
            "${stop.name}$spokenTime"
        }
        val count = if (remaining.size == 1) "une étape" else "${remaining.size} étapes"
        return "Au programme aujourd'hui, $count : ${parts.joinToString(", puis ")}."
    }

    /** Briefing complet : météo (si disponible) + étapes du jour. */
    fun compose(weather: String?, stops: List<TripStop>): String =
        listOfNotNull(weather, forStops(stops)).joinToString(" ")
            .ifBlank { "Aucune information pour aujourd'hui. Bonne route !" }
}

/** Libellé français du jour, réutilisé partout ("Mardi 15 juillet 2026"). */
fun LocalDate.frenchLabel(): String =
    format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH))
        .replaceFirstChar { it.uppercase(Locale.FRENCH) }
