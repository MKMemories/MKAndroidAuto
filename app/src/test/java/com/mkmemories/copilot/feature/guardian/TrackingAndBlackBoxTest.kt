package com.mkmemories.copilot.feature.guardian

import com.mkmemories.copilot.feature.blackbox.BlackBox
import java.time.LocalTime
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackingAndBlackBoxTest {

    // --- Voyage suivi par SMS -------------------------------------------------

    @Test
    fun `premier point = SMS immediat avec position, heure et prochaine etape`() {
        val tracking = TripTracking()
        val message = tracking.onLocation(1_000_000L, 46.5, 4.2, "Mâcon", LocalTime.of(21, 15))!!
        assertTrue(message.contains("21h15"))
        assertTrue(message.contains("https://maps.google.com/?q=46.5,4.2"))
        assertTrue(message.contains("En route vers Mâcon"))
    }

    @Test
    fun `pas de nouveau SMS avant l'intervalle, puis reprise`() {
        val tracking = TripTracking(intervalMillis = 30 * 60_000L)
        tracking.onLocation(0L, 1.0, 1.0, null, LocalTime.NOON)
        assertNull(tracking.onLocation(29 * 60_000L, 1.1, 1.1, null, LocalTime.NOON))
        assertNotNull(tracking.onLocation(30 * 60_000L, 1.2, 1.2, null, LocalTime.NOON))
    }

    @Test
    fun `sans prochaine etape, le message reste propre`() {
        val message = TripTracking().onLocation(0L, 1.0, 1.0, null, LocalTime.of(9, 5))!!
        assertTrue(message.endsWith("https://maps.google.com/?q=1.0,1.0."))
    }

    // --- Boîte noire -----------------------------------------------------------

    @Test
    fun `la fenetre glissante ne garde que les 60 dernieres secondes`() {
        val box = BlackBox(windowMillis = 60_000L)
        box.record(BlackBox.Sample(0L, 1.0, 1.0, 10f, 1f))
        box.record(BlackBox.Sample(30_000L, 1.1, 1.1, 12f, 1f))
        box.record(BlackBox.Sample(90_000L, 1.2, 1.2, 14f, 1f))
        assertEquals(listOf(30_000L, 90_000L), box.snapshot().map { it.timeMillis })
    }

    @Test
    fun `le JSON d'incident contient le choc et tous les echantillons`() {
        val box = BlackBox()
        box.record(BlackBox.Sample(1_000L, 45.0, 4.0, 25f, 2f))
        box.record(BlackBox.Sample(2_000L, null, null, null, 62f))
        val json = JSONObject(box.toJson(triggerMillis = 2_000L, magnitudeMs2 = 62f))
        assertEquals(2_000L, json.getLong("choc_t_ms"))
        assertEquals(62.0, json.getDouble("choc_acceleration_ms2"), 0.01)
        assertEquals(2, json.getJSONArray("echantillons").length())
        // L'échantillon sans GPS n'a pas de champs lat/lng mais reste présent
        val second = json.getJSONArray("echantillons").getJSONObject(1)
        assertTrue(!second.has("lat"))
        assertEquals(62.0, second.getDouble("acceleration_ms2"), 0.01)
    }
}
