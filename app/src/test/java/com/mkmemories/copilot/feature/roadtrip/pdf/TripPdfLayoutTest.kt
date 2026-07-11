package com.mkmemories.copilot.feature.roadtrip.pdf

import com.mkmemories.copilot.feature.roadtrip.Trip
import com.mkmemories.copilot.feature.roadtrip.TripDay
import com.mkmemories.copilot.feature.roadtrip.TripStop
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Mise en page du carnet PDF : sections, dates françaises, enrichissement. */
class TripPdfLayoutTest {

    private val trip = Trip(
        name = "Grèce",
        days = listOf(
            TripDay(
                LocalDate.of(2026, 9, 2),
                listOf(
                    TripStop("Hôtel Katikies", 36.46, 25.37, locality = "Oia", time = LocalTime.of(15, 30)),
                    TripStop("Plage Rouge", 36.34, 25.39, locality = "Santorin"),
                ),
            ),
            TripDay(LocalDate.of(2026, 9, 1), listOf(TripStop("Athènes", 37.98, 23.72, locality = "Athènes"))),
        ),
    )

    @Test
    fun `les sections sont triees par date et datees en francais`() {
        val sections = TripPdfLayout.buildSections(trip, emptyMap())
        assertEquals(listOf("Mardi 1 septembre 2026", "Mercredi 2 septembre 2026"), sections.map { it.dateLabel })
    }

    @Test
    fun `numerotation par journee, heure de rendez-vous dans le sous-titre`() {
        val sections = TripPdfLayout.buildSections(trip, emptyMap())
        val day2 = sections[1]
        assertEquals(listOf(1, 2), day2.stops.map { it.number })
        assertEquals("Rendez-vous à 15h30 — Oia", day2.stops[0].subtitle)
        assertEquals("Santorin", day2.stops[1].subtitle)
    }

    @Test
    fun `la description est associee par localite, sans doublon dans le titre`() {
        val sections = TripPdfLayout.buildSections(trip, mapOf("Oia" to "Village perché d'Oia."))
        assertEquals("Village perché d'Oia.", sections[1].stops[0].description)
        assertNull(sections[1].stops[1].description)
    }

    @Test
    fun `une etape nommee comme sa localite n'affiche pas de sous-titre redondant`() {
        val sections = TripPdfLayout.buildSections(trip, emptyMap())
        assertNull("« Athènes » à Athènes : pas de sous-titre", sections[0].stops[0].subtitle)
    }

    @Test
    fun `les cles d'enrichissement sont uniques - une requete par localite`() {
        assertEquals(listOf("Oia", "Santorin", "Athènes"), TripPdfLayout.enrichmentKeys(trip))
    }
}
