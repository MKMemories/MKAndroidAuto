package com.mkmemories.copilot.feature.guardian

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Seuil de détection d'accident : jamais en conduite normale, toujours en choc réel. */
class CrashDetectorTest {

    @Test
    fun `la conduite normale ne declenche jamais l'alerte`() {
        // Accélération franche ~0,4 g, freinage appuyé ~0,8 g, nid de poule ~2 g
        assertFalse(CrashDetector.isCrash(CrashDetector.magnitudeMs2(4f, 0f, 0f)))
        assertFalse(CrashDetector.isCrash(CrashDetector.magnitudeMs2(0f, 8f, 0f)))
        assertFalse(CrashDetector.isCrash(CrashDetector.magnitudeMs2(10f, 10f, 15f)))
    }

    @Test
    fun `un freinage d'urgence (environ 1 g) reste sous le seuil`() {
        assertFalse(CrashDetector.isCrash(9.81f))
    }

    @Test
    fun `un choc a 6 g et plus declenche l'alerte`() {
        assertTrue(CrashDetector.isCrash(60f))
        assertTrue(CrashDetector.isCrash(CrashDetector.magnitudeMs2(60f, 30f, 10f)))
    }

    @Test
    fun `le seuil vaut environ 6 g comme documente`() {
        assertEquals(60f / 9.81f, 6.12f, 0.05f)
    }

    @Test
    fun `la norme du vecteur est correcte`() {
        assertEquals(5f, CrashDetector.magnitudeMs2(3f, 4f, 0f), 0.0001f)
        assertEquals(13f, CrashDetector.magnitudeMs2(3f, 4f, 12f), 0.0001f)
    }
}
