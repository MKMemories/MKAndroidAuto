package com.mkmemories.copilot.feature.briefing

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * Intégration bout-en-bout du générateur : vraie requête HTTP contre un
 * serveur local aux réponses maîtrisées — fiabilité des données garantie,
 * zéro dépendance au réseau extérieur.
 */
class WeatherBriefingIntegrationTest {

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

    @Test
    fun `generate interroge Open-Meteo avec les bons parametres et lit la reponse`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "current": {"temperature_2m": 18.2, "weather_code": 61, "wind_speed_10m": 20.0},
                  "daily": {
                    "temperature_2m_min": [12.0],
                    "temperature_2m_max": [22.0],
                    "precipitation_probability_max": [80]
                  }
                }
                """.trimIndent(),
            ),
        )

        val text = WeatherBriefingGenerator.generate(48.85, 2.35, baseUrl = server.url("/").toString().trimEnd('/'))

        val request = server.takeRequest()
        assertEquals("/v1/forecast", request.requestUrl!!.encodedPath)
        assertEquals("48.85", request.requestUrl!!.queryParameter("latitude"))
        assertEquals("2.35", request.requestUrl!!.queryParameter("longitude"))
        assertEquals("auto", request.requestUrl!!.queryParameter("timezone"))

        assertTrue(text.contains("pluie"))
        assertTrue(text.contains("18 degrés"))
        assertTrue(text.contains("Risque de pluie : 80 pour cent"))
    }

    @Test
    fun `une erreur serveur remonte en exception - jamais de briefing mensonger`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("boom"))
        try {
            WeatherBriefingGenerator.generate(48.85, 2.35, baseUrl = server.url("/").toString().trimEnd('/'))
            fail("Une réponse 500 doit lever une exception")
        } catch (expected: Exception) {
            // L'appelant (UI / écran voiture) affiche son message d'indisponibilité
        }
    }

    @Test
    fun `une reponse tronquee ou invalide remonte en exception`() = runTest {
        server.enqueue(MockResponse().setBody("""{"current": {"temperature_2m": 18.2}}"""))
        try {
            WeatherBriefingGenerator.generate(48.85, 2.35, baseUrl = server.url("/").toString().trimEnd('/'))
            fail("Un JSON incomplet doit lever une exception")
        } catch (expected: Exception) {
        }
    }
}
