package com.mkmemories.copilot.feature.ai

import android.content.Context
import com.mkmemories.copilot.feature.roadtrip.DayBriefing
import com.mkmemories.copilot.feature.roadtrip.TripStop
import com.mkmemories.copilot.feature.settings.SettingsStore
import kotlinx.coroutines.withTimeout

/**
 * Briefing enrichi, dans l'ordre de préférence du projet :
 *  1. Moteur LOCAL ([DayBriefing]) — il tourne dans le téléphone, hors ligne,
 *     illimité : c'est lui qui produit toujours le texte de base. (Gemini
 *     Nano en v1.1 pour la génération libre on-device.)
 *  2. Mistral en REPLI d'enrichissement, uniquement si l'utilisateur a
 *     fourni sa clé : reformulation plus naturelle du texte local.
 *  3. Toute erreur / lenteur → le texte local part tel quel. Jamais de
 *     briefing bloqué par l'IA.
 */
object BriefingEnricher {

    private const val ENRICH_TIMEOUT_MS = 8_000L

    suspend fun enrich(context: Context, weather: String?, stops: List<TripStop>): String {
        val localText = DayBriefing.compose(weather, stops)
        val engine = AiRouter(
            listOf(
                OnDeviceAiBackend(),
                MistralBackend { SettingsStore(context).mistralKey },
            ),
        ).best() ?: return localText

        return try {
            withTimeout(ENRICH_TIMEOUT_MS) {
                engine.complete(enrichPrompt(localText))
                    .trim()
                    .takeIf { it.length in 20..1200 } // garde-fou : réponse plausible
                    ?: localText
            }
        } catch (e: Exception) {
            localText
        }
    }

    internal fun enrichPrompt(briefing: String): String =
        "Tu es le copilote vocal d'une application de conduite. Réécris ce briefing " +
            "en français naturel et chaleureux, prêt à être lu à voix haute, en 60 à 90 mots, " +
            "sans emoji, sans liste, sans rien inventer : $briefing"
}
