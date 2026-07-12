package com.mkmemories.copilot.feature.data

import com.mkmemories.copilot.feature.fuel.FuelPrices
import com.mkmemories.copilot.feature.places.NearbyWiki
import com.mkmemories.copilot.feature.roadtrip.TripStop
import com.mkmemories.copilot.feature.weather.RouteWeather
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Données externes maîtrisées : météo de route, carburants, geosearch Wikipédia. */
class ExternalDataTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun base() = server.url("/").toString().trimEnd('/')

    // --- Météo d'itinéraire ---------------------------------------------------

    private fun weatherBody(code: Int) = """{"current":{"weather_code":$code}}"""

    @Test
    fun `annonce la premiere etape sous mauvais temps, en francais`() = runTest {
        server.enqueue(MockResponse().setBody(weatherBody(0)))   // Chambord : beau
        server.enqueue(MockResponse().setBody(weatherBody(63)))  // Amboise : pluie
        val alert = RouteWeather.alertForRoute(
            listOf(TripStop("Chambord", 47.6, 1.5), TripStop("Amboise", 47.4, 0.98)),
            baseUrl = base(),
        )
        assertEquals(
            "Plus loin sur votre route, de la pluie vous attend vers Amboise. Adaptez votre allure.",
            alert,
        )
    }

    @Test
    fun `beau temps partout = silence, etapes visitees ignorees`() = runTest {
        server.enqueue(MockResponse().setBody(weatherBody(1)))
        val alert = RouteWeather.alertForRoute(
            listOf(TripStop("Visitée", 1.0, 1.0, visited = true), TripStop("Chambord", 47.6, 1.5)),
            baseUrl = base(),
        )
        assertNull(alert)
        assertEquals("Une seule requête : l'étape visitée est ignorée", 1, server.requestCount)
    }

    @Test
    fun `panne reseau = silence, jamais d'erreur`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        assertNull(RouteWeather.alertForRoute(listOf(TripStop("A", 1.0, 1.0)), baseUrl = base()))
    }

    @Test
    fun `classification des codes degrades`() {
        assertTrue(RouteWeather.isBadWeather(61))
        assertTrue(RouteWeather.isBadWeather(95))
        assertTrue(RouteWeather.isBadWeather(45))
        assertTrue(!RouteWeather.isBadWeather(0))
        assertTrue(!RouteWeather.isBadWeather(3))
    }

    // --- Carburants -------------------------------------------------------------

    private val fuelBody = """
        {"records":[
          {"fields":{"nom":"Super U","ville":"Blois","e10_prix":1.72,"geom":[47.58,1.33]}},
          {"fields":{"nom":"Total","ville":"Vineuil","e10_prix":1.68,"geom":[47.59,1.37]}},
          {"fields":{"nom":"SansPrix","ville":"X","geom":[47.0,1.0]}}
        ]}
    """.trimIndent()

    @Test
    fun `la station la moins chere est retenue, celles sans prix ignorees`() = runTest {
        server.enqueue(MockResponse().setBody(fuelBody))
        val cheapest = FuelPrices.cheapestNearby(47.58, 1.33, "E10", baseUrl = base())!!
        assertEquals("Total", cheapest.name)
        assertEquals(1.68, cheapest.priceEuro, 0.001)
    }

    @Test
    fun `annonce vocale au format francais`() {
        val station = FuelPrices.parse(fuelBody, "E10")!!
        assertEquals("E10 à 1,68 euros le litre chez Total, à Vineuil.", FuelPrices.announcement(station))
    }

    @Test
    fun `reponse vide ou erreur = null`() = runTest {
        server.enqueue(MockResponse().setBody("""{"records":[]}"""))
        assertNull(FuelPrices.cheapestNearby(1.0, 1.0, baseUrl = base()))
        server.enqueue(MockResponse().setResponseCode(503))
        assertNull(FuelPrices.cheapestNearby(1.0, 1.0, baseUrl = base()))
    }

    // --- Wikipédia à proximité ----------------------------------------------------

    @Test
    fun `geosearch parse titres, coordonnees et distances`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"query":{"geosearch":[
                    {"title":"Château de Valençay","lat":47.16,"lon":1.56,"dist":850.3},
                    {"title":"","lat":1,"lon":1,"dist":10},
                    {"title":"Halle au blé","lat":47.15,"lon":1.57,"dist":1200.0}
                ]}}""",
            ),
        )
        val sites = NearbyWiki.around(47.16, 1.56, baseUrl = base())
        assertEquals(listOf("Château de Valençay", "Halle au blé"), sites.map { it.title })
        assertEquals(850.3, sites[0].distanceMeters, 0.01)
    }

    @Test
    fun `reponse sans resultat ou erreur = liste vide`() = runTest {
        server.enqueue(MockResponse().setBody("""{"query":{}}"""))
        assertTrue(NearbyWiki.around(1.0, 1.0, baseUrl = base()).isEmpty())
        server.enqueue(MockResponse().setResponseCode(500))
        assertTrue(NearbyWiki.around(1.0, 1.0, baseUrl = base()).isEmpty())
    }
}
