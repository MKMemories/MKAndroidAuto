package com.mkmemories.copilot.feature.guardian

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Escalade SOS en temps virtuel : le scénario le plus critique de l'app
 * (sécurité) est vérifié seconde par seconde, sans attendre 30 s réelles.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SosManagerTest {

    @Test
    fun `sans reponse du conducteur, SMS envoye a tous les contacts apres 30 secondes`() = runTest {
        val ticks = mutableListOf<Int>()
        val sent = mutableListOf<Pair<String, String>>()
        val sos = SosManager(
            scope = this,
            emergencyContacts = { listOf("+33600000001", "+33600000002") },
            lastKnownLocation = { null },
            onCountdownTick = { ticks.add(it) },
            smsSender = { number, message -> sent.add(number to message) },
        )

        sos.startCountdown()
        advanceUntilIdle()

        assertEquals((30 downTo 1).toList(), ticks)
        assertEquals(listOf("+33600000001", "+33600000002"), sent.map { it.first })
        sent.forEach { (_, message) ->
            assertTrue(message.contains("SOS MK Copilot"))
            assertTrue(message.contains("position indisponible"))
        }
    }

    @Test
    fun `le conducteur repond "tout va bien" - aucun SMS ne part`() = runTest {
        val sent = mutableListOf<String>()
        val sos = SosManager(
            scope = this,
            emergencyContacts = { listOf("+33600000001") },
            lastKnownLocation = { null },
            smsSender = { number, _ -> sent.add(number) },
        )

        sos.startCountdown()
        advanceTimeBy(15_000)
        sos.cancel()
        advanceUntilIdle()

        assertTrue("Aucun SMS ne doit partir après annulation", sent.isEmpty())
    }

    @Test
    fun `deux detections rapprochees ne lancent qu'un seul compte a rebours`() = runTest {
        val ticks = mutableListOf<Int>()
        val sos = SosManager(
            scope = this,
            emergencyContacts = { emptyList() },
            lastKnownLocation = { null },
            onCountdownTick = { ticks.add(it) },
        )

        sos.startCountdown()
        advanceTimeBy(2_000)
        sos.startCountdown() // choc secondaire pendant le compte à rebours
        advanceUntilIdle()

        assertEquals("Le compte à rebours ne doit pas être dupliqué", 30, ticks.size)
    }

    @Test
    fun `le message SOS contient un lien Google Maps quand la position est connue`() {
        val message = SosManager.sosMessage(45.7640, 4.8357)
        assertTrue(message.contains("https://maps.google.com/?q=45.764,4.8357"))
    }

    @Test
    fun `le message SOS reste utilisable sans position`() {
        val message = SosManager.sosMessage(null, null)
        assertTrue(message.contains("position indisponible"))
    }
}
