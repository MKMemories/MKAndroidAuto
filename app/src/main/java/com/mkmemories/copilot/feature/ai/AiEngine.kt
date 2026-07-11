package com.mkmemories.copilot.feature.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Couche IA 100 % gratuite, trois backends par ordre de préférence :
 *  1. [OnDeviceAiBackend] — Gemini Nano via AICore ou petit modèle local
 *     (privé, hors ligne, illimité). TODO v1.1.
 *  2. [MistralBackend] — clé API personnelle gratuite de l'utilisateur
 *     (tier "Experiment" de La Plateforme, ~2 req/min : suffisant pour un
 *     briefing par trajet). Optionnelle, saisie dans les réglages.
 *  3. Repli sans IA : les briefings restent générés par gabarits
 *     (voir WeatherBriefingGenerator, qui n'a besoin d'aucune IA).
 */
interface AiEngine {
    val isAvailable: Boolean
    suspend fun complete(prompt: String): String
}

class OnDeviceAiBackend : AiEngine {
    // TODO v1.1 : intégrer AICore (Gemini Nano) quand disponible sur l'appareil,
    // sinon un petit modèle quantisé (llama.cpp / MediaPipe LLM Inference).
    override val isAvailable: Boolean = false
    override suspend fun complete(prompt: String): String =
        error("Backend on-device pas encore disponible")
}

/** Backend Mistral avec la clé gratuite personnelle de l'utilisateur. */
class MistralBackend(private val apiKey: () -> String?) : AiEngine {

    override val isAvailable: Boolean get() = !apiKey().isNullOrBlank()

    override suspend fun complete(prompt: String): String = withContext(Dispatchers.IO) {
        val key = requireNotNull(apiKey()) { "Clé API Mistral absente" }
        val connection = URL("https://api.mistral.ai/v1/chat/completions")
            .openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 15_000
            connection.readTimeout = 60_000
            connection.setRequestProperty("Authorization", "Bearer $key")
            connection.setRequestProperty("Content-Type", "application/json")

            val body = JSONObject()
                .put("model", "mistral-small-latest")
                .put(
                    "messages",
                    JSONArray().put(
                        JSONObject().put("role", "user").put("content", prompt),
                    ),
                )
            connection.outputStream.use { it.write(body.toString().toByteArray()) }

            val response = JSONObject(connection.inputStream.bufferedReader().readText())
            response.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        } finally {
            connection.disconnect()
        }
    }
}

/** Choisit le meilleur backend disponible, ou null (repli gabarits sans IA). */
class AiRouter(private val backends: List<AiEngine>) {
    fun best(): AiEngine? = backends.firstOrNull { it.isAvailable }
}
