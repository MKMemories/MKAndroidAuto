package com.mkmemories.copilot.feature.ai

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mkmemories.copilot.feature.roadtrip.TripStop
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** L'IA enrichit mais ne bloque jamais : sans clé, le moteur local répond. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BriefingEnricherTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `sans cle Mistral, le texte local part tel quel (moteur local prioritaire)`() = runTest {
        val text = BriefingEnricher.enrich(
            context,
            weather = "Il fait beau.",
            stops = listOf(TripStop("Chambord", 47.6, 1.5)),
        )
        assertEquals("Il fait beau. Au programme aujourd'hui, une étape : Chambord.", text)
    }

    @Test
    fun `le prompt d'enrichissement contient le briefing local et les consignes voix`() {
        val prompt = BriefingEnricher.enrichPrompt("Texte de base.")
        assertTrue(prompt.contains("Texte de base."))
        assertTrue(prompt.contains("voix haute"))
        assertTrue(prompt.contains("sans rien inventer"))
    }
}
