package com.mkmemories.copilot.feature.carnet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GreeceOdysseyTest {

    private val carnet = GreeceOdyssey.itinerary()

    @Test
    fun `le carnet couvre les sept phases du voyage`() {
        assertEquals(7, carnet.phases.size)
        assertEquals("depart", carnet.phases.first().id)
        assertEquals("retour", carnet.phases.last().id)
    }

    @Test
    fun `chaque trajet voiture a une destination geolocalisee`() {
        assertEquals(11, carnet.drives.size)
        assertTrue(carnet.drives.all { it.destination != null })
    }

    @Test
    fun `le premier trajet mene a Orly et le dernier au domicile`() {
        assertEquals("Aéroport de Paris-Orly", carnet.drives.first().destination?.name)
        assertEquals("Domicile — Andrésy", carnet.drives.last().destination?.name)
    }

    @Test
    fun `toutes les entrees restent dans la periode du voyage`() {
        assertTrue(carnet.entries.all { !it.date.isBefore(carnet.start) && !it.date.isAfter(carnet.end) })
    }

    @Test
    fun `les identifiants d entrees sont uniques`() {
        val ids = carnet.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `les phases insulaires portent des inspirations`() {
        val milos = carnet.phases.first { it.id == "milos" }
        assertFalse(milos.inspirations.isEmpty())
    }
}
