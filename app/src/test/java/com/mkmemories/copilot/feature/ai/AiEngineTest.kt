package com.mkmemories.copilot.feature.ai

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiEngineTest {

    private class FakeBackend(override val isAvailable: Boolean, private val reply: String) : AiEngine {
        override suspend fun complete(prompt: String): String = reply
    }

    @Test
    fun `le routeur choisit le premier backend disponible dans l'ordre de preference`() = runTest {
        val router = AiRouter(
            listOf(
                FakeBackend(isAvailable = false, reply = "on-device"),
                FakeBackend(isAvailable = true, reply = "mistral"),
            ),
        )
        assertEquals("mistral", router.best()!!.complete("test"))
    }

    @Test
    fun `aucun backend disponible = repli gabarits sans IA (null)`() {
        assertNull(AiRouter(listOf(FakeBackend(false, ""))).best())
        assertNull(AiRouter(emptyList()).best())
    }

    @Test
    fun `le backend Mistral n'est disponible qu'avec une cle non vide`() {
        assertFalse(MistralBackend { null }.isAvailable)
        assertFalse(MistralBackend { "" }.isAvailable)
        assertFalse(MistralBackend { "  " }.isAvailable)
        assertTrue(MistralBackend { "ma-clé-perso" }.isAvailable)
    }

    @Test
    fun `le backend on-device se declare indisponible en v1`() {
        assertFalse(OnDeviceAiBackend().isAvailable)
    }
}
