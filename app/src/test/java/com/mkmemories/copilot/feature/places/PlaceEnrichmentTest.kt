package com.mkmemories.copilot.feature.places

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** Enrichissement Wikipédia : meilleur effort, jamais bloquant pour l'export. */
class PlaceEnrichmentTest {

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

    @Test
    fun `l'extrait encyclopedique est retourne tel quel`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"type":"standard","title":"Santorin",
                    "extract":"Santorin est une île grecque des Cyclades, célèbre pour sa caldeira."}""",
            ),
        )
        assertEquals(
            "Santorin est une île grecque des Cyclades, célèbre pour sa caldeira.",
            PlaceEnrichment.describe("Santorin", baseUrl = base()),
        )
    }

    @Test
    fun `page d'homonymie = null (un extrait d'homonymie n'apprend rien)`() = runTest {
        server.enqueue(
            MockResponse().setBody("""{"type":"disambiguation","extract":"Paris peut désigner..."}"""),
        )
        assertNull(PlaceEnrichment.describe("Paris", baseUrl = base()))
    }

    @Test
    fun `page absente (404) = null, jamais d'exception`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))
        assertNull(PlaceEnrichment.describe("Hôtel Inconnu Xyz", baseUrl = base()))
    }

    @Test
    fun `reponse sans extrait = null`() = runTest {
        server.enqueue(MockResponse().setBody("""{"type":"standard","title":"X"}"""))
        assertNull(PlaceEnrichment.describe("X", baseUrl = base()))
    }
}
