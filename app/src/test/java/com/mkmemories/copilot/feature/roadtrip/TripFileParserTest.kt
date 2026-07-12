package com.mkmemories.copilot.feature.roadtrip

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Import KML / GPX / JSON : tolérant, jamais de plantage sur un fichier tiers. */
class TripFileParserTest {

    @Test
    fun `KML Google My Maps - noms, CDATA et coordonnees lng-lat`() {
        val kml = """
            <?xml version="1.0"?>
            <kml xmlns="http://www.opengis.net/kml/2.2"><Document>
              <Placemark><name><![CDATA[Château de Chambord]]></name>
                <Point><coordinates>1.5170,47.6161,0</coordinates></Point></Placemark>
              <Placemark><name>Hôtel — Blois</name>
                <Point><coordinates>
                  1.3359,47.5861
                </coordinates></Point></Placemark>
              <Placemark><name>Sans coordonnées</name></Placemark>
            </Document></kml>
        """.trimIndent()
        val places = TripFileParser.parse(kml)
        assertEquals(2, places.size)
        assertEquals("Château de Chambord", places[0].name)
        // KML donne lng,lat : vérifie l'inversion
        assertEquals(47.6161, places[0].latitude, 0.0001)
        assertEquals(1.5170, places[0].longitude, 0.0001)
        assertEquals("Hôtel — Blois", places[1].name)
    }

    @Test
    fun `GPX - waypoints avec lat-lon en attributs`() {
        val gpx = """
            <?xml version="1.0"?>
            <gpx version="1.1"><wpt lat="47.6161" lon="1.5170"><name>Chambord</name></wpt>
            <wpt lat="47.5861" lon="1.3359"/></gpx>
        """.trimIndent()
        val places = TripFileParser.parse(gpx)
        assertEquals(2, places.size)
        assertEquals("Chambord", places[0].name)
        assertEquals("Étape importée", places[1].name)
        assertEquals(47.5861, places[1].latitude, 0.0001)
    }

    @Test
    fun `JSON MK Copilot - voyage complet restaure avec dates et heures`() {
        val trip = Trip(
            "Grèce",
            listOf(TripDay(LocalDate.of(2026, 9, 2), listOf(TripStop("Oia", 36.46, 25.37)))),
        )
        val restored = TripFileParser.parseFullTrip(TripStore.toJson(trip))
        assertEquals(trip, restored)
    }

    @Test
    fun `fichier quelconque = liste vide et pas de voyage, jamais d'exception`() {
        assertTrue(TripFileParser.parse("bonjour, ceci n'est pas un fichier de voyage").isEmpty())
        assertTrue(TripFileParser.parse("").isEmpty())
        assertNull(TripFileParser.parseFullTrip("<kml></kml>"))
        assertNull(TripFileParser.parseFullTrip("{pas du json"))
    }
}
