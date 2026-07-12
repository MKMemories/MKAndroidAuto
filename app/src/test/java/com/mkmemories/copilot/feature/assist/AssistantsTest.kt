package com.mkmemories.copilot.feature.assist

import com.mkmemories.copilot.feature.guide.TouristGuide
import com.mkmemories.copilot.feature.messaging.DriveMessaging
import com.mkmemories.copilot.feature.voice.VoiceCommand
import com.mkmemories.copilot.feature.voice.VoiceCommands
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantsTest {

    // --- Messagerie apaisée ------------------------------------------------------

    @Test
    fun `lecture vocale - messages longs tronques proprement`() {
        val messaging = DriveMessaging()
        assertEquals("Message de Marie : On mange où ?", messaging.spokenAnnouncement("Marie", "On mange où ?"))
        val long = messaging.spokenAnnouncement("Paul", "x".repeat(500))
        assertTrue(long.endsWith("… message tronqué"))
        assertTrue(long.length < 300)
    }

    @Test
    fun `reponse automatique avec ETA et prochaine etape`() {
        val reply = DriveMessaging().autoReply("Marie", 0L, LocalTime.of(18, 40), "Blois")!!
        assertTrue(reply.contains("Je conduis"))
        assertTrue(reply.contains("J'arrive à Blois vers 18h40"))
        assertTrue(reply.contains("Réponse automatique"))
    }

    @Test
    fun `jamais deux reponses au meme contact en moins de 30 minutes`() {
        val messaging = DriveMessaging()
        assertNotNull(messaging.autoReply("Marie", 0L, null, null))
        assertNull("Marie vient d'être prévenue", messaging.autoReply("Marie", 10 * 60_000L, null, null))
        assertNotNull("Autre contact : réponse normale", messaging.autoReply("Paul", 10 * 60_000L, null, null))
        assertNotNull("Après 30 min : de nouveau possible", messaging.autoReply("Marie", 31 * 60_000L, null, null))
    }

    @Test
    fun `sans ETA, la reponse reste correcte`() {
        val reply = DriveMessaging().autoReply("Marie", 0L, null, null)!!
        assertTrue(!reply.contains("J'arrive"))
    }

    // --- Guide du territoire --------------------------------------------------------

    @Test
    fun `cadence - jamais deux histoires en moins de 10 minutes ni sans avoir roule`() {
        val guide = TouristGuide()
        assertTrue(guide.shouldNarrate(0L, 47.0, 1.0))
        guide.pick(0L, 47.0, 1.0, listOf("Château A"))
        assertTrue("Trop tôt", !guide.shouldNarrate(5 * 60_000L, 47.5, 1.5))
        assertTrue("Assez de temps mais pas assez de route", !guide.shouldNarrate(11 * 60_000L, 47.001, 1.0))
        assertTrue(guide.shouldNarrate(11 * 60_000L, 47.2, 1.3))
    }

    @Test
    fun `jamais deux fois le meme site`() {
        val guide = TouristGuide()
        assertEquals("Château A", guide.pick(0L, 1.0, 1.0, listOf("Château A", "Halle B")))
        assertEquals("Halle B", guide.pick(20 * 60_000L, 2.0, 2.0, listOf("Château A", "Halle B")))
        assertNull(guide.pick(40 * 60_000L, 3.0, 3.0, listOf("Château A", "Halle B")))
    }

    // --- Commandes vocales ------------------------------------------------------------

    @Test
    fun `formulations naturelles reconnues, accents ignores`() {
        assertEquals(VoiceCommand.NEXT_STOP, VoiceCommands.match("Prochaine étape s'il te plaît"))
        assertEquals(VoiceCommand.NEXT_STOP, VoiceCommands.match("étape suivante"))
        assertEquals(VoiceCommand.WHERE_IS_CAR, VoiceCommands.match("Où est ma voiture ?"))
        assertEquals(VoiceCommand.NOTIFY_ARRIVAL, VoiceCommands.match("préviens Marie que j'arrive"))
        assertEquals(VoiceCommand.PLAY_BRIEFING, VoiceCommands.match("quel temps fait-il"))
    }

    @Test
    fun `phrase hors sujet = aucune action (jamais d'action accidentelle)`() {
        assertNull(VoiceCommands.match("mets de la musique"))
        assertNull(VoiceCommands.match(""))
    }

    @Test
    fun `normalisation - accents et ponctuation aplatis`() {
        assertEquals("ou est ma voiture", VoiceCommands.normalize("Où est ma voiture ?!"))
    }
}
