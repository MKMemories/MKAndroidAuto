package com.mkmemories.copilot.feature.guardian

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ArrivalNotifierTest {

    @Test
    fun `le message d'arrivee est horodate a la francaise`() {
        assertEquals(
            "MK Copilot : bien arrivé à Lyon, 18h42. 🚗",
            ArrivalNotifier.arrivalMessage("Lyon", LocalTime.of(18, 42)),
        )
    }

    @Test
    fun `les minutes sont toujours sur deux chiffres`() {
        assertEquals(
            "MK Copilot : bien arrivé à Blois, 09h05. 🚗",
            ArrivalNotifier.arrivalMessage("Blois", LocalTime.of(9, 5)),
        )
    }

    @Test
    fun `chaque proche recoit le meme SMS`() {
        val sent = mutableListOf<Pair<String, String>>()
        ArrivalNotifier.notifyArrival(
            destinationName = "Amboise",
            recipients = listOf("+33611111111", "+33622222222"),
            arrivalTime = LocalTime.of(17, 30),
            smsSender = { number, message -> sent.add(number to message) },
        )
        assertEquals(2, sent.size)
        assertEquals(setOf("MK Copilot : bien arrivé à Amboise, 17h30. 🚗"), sent.map { it.second }.toSet())
    }
}
