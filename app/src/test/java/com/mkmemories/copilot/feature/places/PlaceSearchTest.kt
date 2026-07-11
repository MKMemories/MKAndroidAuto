package com.mkmemories.copilot.feature.places

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/** Autocomplétion de lieux : parsing GeoJSON Photon avec données maîtrisées. */
class PlaceSearchTest {

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

    // Extrait réaliste d'une réponse Photon pour "hôtel Santorin"
    private val photonBody = """
        {"features":[
          {"geometry":{"coordinates":[25.3753,36.4622],"type":"Point"},
           "properties":{"name":"Katikies Hotel","osm_key":"tourism","osm_value":"hotel",
                         "city":"Oia","country":"Grèce","state":"Égée-Méridionale"}},
          {"geometry":{"coordinates":[24.7,35.24],"type":"Point"},
           "properties":{"name":"","street":"Odos Eleftheriou","housenumber":"12",
                         "city":"Réthymnon","country":"Grèce"}}
        ]}
    """.trimIndent()

    @Test
    fun `parsing - nom, categorie francisee, localite et coordonnees GeoJSON (lng,lat)`() = runTest {
        server.enqueue(MockResponse().setBody(photonBody))
        val results = PlaceSearch.search("katikies", baseUrl = server.url("/").toString().trimEnd('/'))

        val hotel = results.first()
        assertEquals("Katikies Hotel", hotel.name)
        assertEquals("Hôtel", hotel.category)
        assertEquals("Oia", hotel.locality)
        // GeoJSON donne [longitude, latitude] : vérifie que l'ordre est bien inversé
        assertEquals(36.4622, hotel.latitude, 0.0001)
        assertEquals(25.3753, hotel.longitude, 0.0001)
        assertTrue(hotel.detail.contains("Grèce"))
    }

    @Test
    fun `une adresse sans nom utilise rue + numero comme intitule`() = runTest {
        server.enqueue(MockResponse().setBody(photonBody))
        val results = PlaceSearch.search("elefteriou", baseUrl = server.url("/").toString().trimEnd('/'))
        assertEquals("Odos Eleftheriou 12", results[1].name)
        assertEquals("Réthymnon", results[1].locality)
    }

    @Test
    fun `reponse sans resultat = liste vide`() = runTest {
        server.enqueue(MockResponse().setBody("""{"features":[]}"""))
        assertTrue(PlaceSearch.search("zzzz", baseUrl = server.url("/").toString().trimEnd('/')).isEmpty())
    }

    @Test
    fun `erreur serveur = exception (l'UI affiche son message de repli)`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))
        try {
            PlaceSearch.search("paris", baseUrl = server.url("/").toString().trimEnd('/'))
            fail("Une 503 doit lever une exception")
        } catch (expected: Exception) {
        }
    }

    @Test
    fun `la requete transmet bien le texte, la langue et la limite`() = runTest {
        server.enqueue(MockResponse().setBody("""{"features":[]}"""))
        PlaceSearch.search("hôtel à Santorin", limit = 4, baseUrl = server.url("/").toString().trimEnd('/'))
        val request = server.takeRequest()
        assertEquals("/api/", request.requestUrl!!.encodedPath)
        assertEquals("hôtel à Santorin", request.requestUrl!!.queryParameter("q"))
        assertEquals("fr", request.requestUrl!!.queryParameter("lang"))
        assertEquals("4", request.requestUrl!!.queryParameter("limit"))
    }
}
