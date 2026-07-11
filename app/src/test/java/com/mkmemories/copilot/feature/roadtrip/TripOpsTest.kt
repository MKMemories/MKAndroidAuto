package com.mkmemories.copilot.feature.roadtrip

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripOpsTest {

    private val date: LocalDate = LocalDate.of(2026, 7, 15)
    private val stopA = TripStop("A", 1.0, 2.0)
    private val stopB = TripStop("B", 3.0, 4.0)

    @Test
    fun `withStop cree la journee si elle n'existe pas et trie les jours`() {
        val trip = Trip("T", listOf(TripDay(date.plusDays(1), listOf(stopB))))
            .withStop(date, stopA)
        assertEquals(listOf(date, date.plusDays(1)), trip.days.map { it.date })
        assertEquals(listOf("A"), trip.days.first().stops.map { it.name })
    }

    @Test
    fun `withStop ajoute en fin de journee existante`() {
        val trip = Trip("T", listOf(TripDay(date, listOf(stopA)))).withStop(date, stopB)
        assertEquals(listOf("A", "B"), trip.stopsFor(date).map { it.name })
    }

    @Test
    fun `withoutStop supprime l'etape et la journee devenue vide`() {
        val trip = Trip("T", listOf(TripDay(date, listOf(stopA)))).withoutStop(date, stopA)
        assertTrue(trip.days.isEmpty())
    }

    @Test
    fun `withMovedStop deplace vers le haut et le bas, et ignore les bords`() {
        val trip = Trip("T", listOf(TripDay(date, listOf(stopA, stopB))))
        assertEquals(listOf("B", "A"), trip.withMovedStop(date, 1, -1).stopsFor(date).map { it.name })
        assertEquals(listOf("B", "A"), trip.withMovedStop(date, 0, +1).stopsFor(date).map { it.name })
        // Hors bornes : rien ne bouge, rien ne plante
        assertEquals(listOf("A", "B"), trip.withMovedStop(date, 0, -1).stopsFor(date).map { it.name })
        assertEquals(listOf("A", "B"), trip.withMovedStop(date, 5, +1).stopsFor(date).map { it.name })
    }

    @Test
    fun `timeLabel formate l'heure a la francaise, null sans heure`() {
        assertEquals("09h05", stopA.copy(time = LocalTime.of(9, 5)).timeLabel())
        assertNull(stopA.timeLabel())
    }

    @Test
    fun `le briefing du jour enumere les etapes restantes avec leurs heures`() {
        val stops = listOf(
            TripStop("Musée", 0.0, 0.0, time = LocalTime.of(9, 30)),
            TripStop("Plage", 0.0, 0.0),
            TripStop("Déjà vu", 0.0, 0.0, visited = true),
        )
        assertEquals(
            "Au programme aujourd'hui, 2 étapes : Musée à 9 heures 30, puis Plage.",
            DayBriefing.forStops(stops),
        )
    }

    @Test
    fun `briefing - une heure pile ne mentionne pas les minutes`() {
        val stops = listOf(TripStop("Ferry", 0.0, 0.0, time = LocalTime.of(14, 0)))
        assertEquals(
            "Au programme aujourd'hui, une étape : Ferry à 14 heures.",
            DayBriefing.forStops(stops),
        )
    }

    @Test
    fun `briefing - null sans etape, message dedie si tout est visite`() {
        assertNull(DayBriefing.forStops(emptyList()))
        assertEquals(
            "Toutes les étapes du jour sont visitées. Bravo !",
            DayBriefing.forStops(listOf(stopA.copy(visited = true))),
        )
    }

    @Test
    fun `compose reunit meteo et etapes, et reste utilisable sans rien`() {
        val text = DayBriefing.compose("Il fait beau.", listOf(stopA))
        assertTrue(text.startsWith("Il fait beau. Au programme"))
        assertEquals("Aucune information pour aujourd'hui. Bonne route !", DayBriefing.compose(null, emptyList()))
    }
}
