package com.mkmemories.copilot.feature.roadtrip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Construction pure de l'URL d'itinéraire — aucune dépendance Android. */
class NavigationLauncherUrlTest {

    @Test
    fun `une seule etape = destination sans waypoints`() {
        val url = NavigationLauncher.dayItineraryUrlString(listOf(TripStop("A", 47.5, 1.25)))
        assertEquals(
            "https://www.google.com/maps/dir/?api=1&destination=47.5,1.25&travelmode=driving",
            url,
        )
    }

    @Test
    fun `plusieurs etapes = la derniere en destination, les autres en waypoints encodes`() {
        val url = NavigationLauncher.dayItineraryUrlString(
            listOf(TripStop("A", 1.0, 2.0), TripStop("B", 3.0, 4.0), TripStop("C", 5.0, 6.0)),
        )
        assertTrue(url.contains("&destination=5.0,6.0"))
        assertTrue(url.contains("&waypoints=1.0,2.0%7C3.0,4.0"))
        // Le pipe brut casserait l'URL dans certains parseurs : il doit être encodé
        assertFalse(url.contains("|"))
    }

    @Test
    fun `l'URL respecte la limite officielle de 9 waypoints`() {
        val stops = (1..15).map { TripStop("S$it", it.toDouble(), it.toDouble()) }
        val url = NavigationLauncher.dayItineraryUrlString(stops)
        val waypointCount = url.substringAfter("&waypoints=").substringBefore("&")
            .split("%7C").size
        assertEquals(9, waypointCount)
        assertTrue("La destination doit rester la dernière étape", url.contains("&destination=15.0,15.0"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `liste vide = erreur explicite`() {
        NavigationLauncher.dayItineraryUrlString(emptyList())
    }
}
