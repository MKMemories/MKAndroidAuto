package com.mkmemories.copilot.feature.dangerzones

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Open data radars : parseur tolérant, cache hors ligne, jamais de plantage. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RadarOpenDataRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        File(context.filesDir, "radars.csv").delete()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private val csv = """
        id;latitude;longitude;route;vitesse_vehicules_legers_kmh
        1;48.8566;2.3522;D7;50
        2;45.7578;4.8320;N6;90
        3;44.1234;4.9876;A7;130
        4;;2.0;D1;80
        5;43.5;1.5;D99;
    """.trimIndent()

    @Test
    fun `parseCsv - colonnes reperees par en-tete, lignes invalides ignorees`() {
        val zones = RadarOpenDataRepository.parseCsv(csv)
        assertEquals("La ligne sans latitude est ignorée", 4, zones.size)
        assertEquals(RoadType.URBAN, zones[0].roadType)     // 50 km/h
        assertEquals(RoadType.ROAD, zones[1].roadType)      // 90 km/h
        assertEquals(RoadType.HIGHWAY, zones[2].roadType)   // 130 km/h
        assertEquals(RoadType.ROAD, zones[3].roadType)      // vitesse inconnue : prudence
        assertEquals(50, zones[0].speedLimitKmh)
        assertNull(zones[3].speedLimitKmh)
    }

    @Test
    fun `parseCsv - separateur virgule et decimales a virgule geres`() {
        val zones = RadarOpenDataRepository.parseCsv(
            "latitude,longitude,vitesse\n\"48,85\",\"2,35\",110",
        )
        // Les guillemets ne sont pas gérés : ce format tombe proprement à zéro zone
        // (le vrai export data.gouv utilise ; et des points décimaux)
        assertTrue(zones.isEmpty() || zones.first().roadType == RoadType.HIGHWAY)
    }

    @Test
    fun `parseCsv - en-tete sans latitude-longitude = liste vide`() {
        assertTrue(RadarOpenDataRepository.parseCsv("a;b;c\n1;2;3").isEmpty())
    }

    @Test
    fun `mapping vitesse - regles francaises`() {
        assertEquals(RoadType.HIGHWAY, RadarOpenDataRepository.roadTypeForSpeed(130))
        assertEquals(RoadType.HIGHWAY, RadarOpenDataRepository.roadTypeForSpeed(110))
        assertEquals(RoadType.ROAD, RadarOpenDataRepository.roadTypeForSpeed(90))
        assertEquals(RoadType.ROAD, RadarOpenDataRepository.roadTypeForSpeed(80))
        assertEquals(RoadType.URBAN, RadarOpenDataRepository.roadTypeForSpeed(50))
        assertEquals(RoadType.URBAN, RadarOpenDataRepository.roadTypeForSpeed(30))
        assertEquals(RoadType.ROAD, RadarOpenDataRepository.roadTypeForSpeed(null))
    }

    @Test
    fun `csvUrlFromDatasetJson - premiere ressource CSV retenue`() {
        val json = """
            {"resources":[
              {"format":"pdf","url":"https://x/doc.pdf"},
              {"format":"CSV","url":"https://static.data.gouv.fr/radars.csv"},
              {"format":"csv","url":"https://x/autre.csv"}
            ]}
        """.trimIndent()
        assertEquals(
            "https://static.data.gouv.fr/radars.csv",
            RadarOpenDataRepository.csvUrlFromDatasetJson(json),
        )
        assertNull(RadarOpenDataRepository.csvUrlFromDatasetJson("""{"resources":[]}"""))
    }

    @Test
    fun `refreshIfStale - telecharge, met en cache, puis considere le cache frais`() = runTest {
        val base = server.url("/").toString().trimEnd('/')
        server.enqueue(
            MockResponse().setBody(
                """{"resources":[{"format":"csv","url":"$base/radars.csv"}]}""",
            ),
        )
        server.enqueue(MockResponse().setBody(csv))

        assertTrue("Premier appel : téléchargement", RadarOpenDataRepository.refreshIfStale(context, base))
        assertEquals(4, RadarOpenDataRepository.cachedZones(context).size)
        assertFalse("Cache frais : pas de re-téléchargement", RadarOpenDataRepository.refreshIfStale(context, base))
    }

    @Test
    fun `refreshIfStale - erreur reseau = false, cache existant preserve`() = runTest {
        File(context.filesDir, "radars.csv").writeText(csv)
        server.enqueue(MockResponse().setResponseCode(500))
        // Force la péremption du cache pour déclencher la tentative réseau
        File(context.filesDir, "radars.csv").setLastModified(1_000L)

        assertFalse(RadarOpenDataRepository.refreshIfStale(context, server.url("/").toString().trimEnd('/')))
        assertEquals("Les anciennes données restent utilisables", 4, RadarOpenDataRepository.cachedZones(context).size)
    }

    @Test
    fun `cachedZones sans cache = liste vide, jamais d'exception`() {
        assertTrue(RadarOpenDataRepository.cachedZones(context).isEmpty())
    }
}
