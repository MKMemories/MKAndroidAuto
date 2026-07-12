package com.mkmemories.copilot.feature.carnet

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CarnetToTripTest {

    private val carnet = GreeceOdyssey.itinerary()

    @Test
    fun `toTrip ne garde que les trajets voiture geolocalises`() {
        val trip = carnet.toTrip()
        val stops = trip.days.flatMap { it.stops }
        assertEquals(11, stops.size)
        assertTrue(stops.all { it.latitude != 0.0 && it.longitude != 0.0 })
    }

    @Test
    fun `toTrip regroupe par date et trie les jours`() {
        val trip = carnet.toTrip()
        val dates = trip.days.map { it.date }
        assertEquals(dates.sorted(), dates)
        assertEquals(6, trip.days.size)
        // 24 juillet : route vers Orly puis route vers Kamari
        val firstDay = trip.days.first()
        assertEquals(LocalDate.of(2026, 7, 24), firstDay.date)
        assertEquals(2, firstDay.stops.size)
    }

    @Test
    fun `toTrip conserve les heures et localites`() {
        val trip = carnet.toTrip()
        val orly = trip.days.first().stops.first()
        assertEquals("Aéroport de Paris-Orly", orly.name)
        assertEquals("Orly", orly.locality)
        assertEquals(11, orly.time?.hour)
    }

    @Test
    fun `nextDriveDay renvoie le premier jour a partir de la date`() {
        assertEquals(LocalDate.of(2026, 7, 24), carnet.nextDriveDay(LocalDate.of(2026, 7, 1))?.date)
        assertEquals(LocalDate.of(2026, 7, 30), carnet.nextDriveDay(LocalDate.of(2026, 7, 28))?.date)
        assertEquals(LocalDate.of(2026, 8, 14), carnet.nextDriveDay(LocalDate.of(2026, 8, 14))?.date)
        assertNull(carnet.nextDriveDay(LocalDate.of(2026, 8, 20)))
    }
}
