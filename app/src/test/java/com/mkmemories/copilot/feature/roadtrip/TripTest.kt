package com.mkmemories.copilot.feature.roadtrip

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripTest {

    private val today: LocalDate = LocalDate.of(2026, 7, 11)

    private val trip = Trip(
        name = "Test",
        days = listOf(
            TripDay(today, listOf(TripStop("A", 1.0, 2.0), TripStop("B", 3.0, 4.0))),
            TripDay(today.plusDays(1), listOf(TripStop("C", 5.0, 6.0))),
        ),
    )

    @Test
    fun `stopsFor renvoie les etapes du jour demande`() {
        assertEquals(listOf("A", "B"), trip.stopsFor(today).map { it.name })
        assertEquals(listOf("C"), trip.stopsFor(today.plusDays(1)).map { it.name })
    }

    @Test
    fun `stopsFor renvoie une liste vide pour un jour sans etape`() {
        assertTrue(trip.stopsFor(today.plusDays(30)).isEmpty())
    }

    @Test
    fun `le voyage de demonstration a des etapes pour aujourd'hui`() {
        // Garantit que l'écran Android Auto n'est jamais vide en démo
        val demo = TripRepository.currentTrip()
        assertTrue(demo.stopsFor(LocalDate.now()).isNotEmpty())
    }

    @Test
    fun `les etapes de demonstration ont des coordonnees plausibles (France)`() {
        val demo = TripRepository.currentTrip()
        demo.days.flatMap { it.stops }.forEach { stop ->
            assertTrue("${stop.name} : latitude hors de France", stop.latitude in 41.0..51.5)
            assertTrue("${stop.name} : longitude hors de France", stop.longitude in -5.5..9.9)
        }
    }
}
