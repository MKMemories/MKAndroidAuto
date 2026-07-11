package com.mkmemories.copilot.feature.places

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Enrichissement des lieux pour l'export PDF : résumé encyclopédique de la
 * ville / île / site via l'API REST de Wikipédia (gratuite). Meilleur effort :
 * toute erreur ou page absente rend simplement `null`, jamais d'échec d'export.
 */
object PlaceEnrichment {

    suspend fun describe(
        placeName: String,
        baseUrl: String = "https://fr.wikipedia.org",
    ): String? = withContext(Dispatchers.IO) {
        try {
            val title = URLEncoder.encode(placeName.trim().replace(' ', '_'), "UTF-8")
            val connection =
                URL("$baseUrl/api/rest_v1/page/summary/$title?redirect=true").openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 8_000
                connection.readTimeout = 8_000
                connection.instanceFollowRedirects = true
                connection.setRequestProperty("User-Agent", "MKCopilot/0.1 (app gratuite road trip)")
                if (connection.responseCode != 200) return@withContext null
                parse(connection.inputStream.bufferedReader().readText())
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            null
        }
    }

    internal fun parse(body: String): String? {
        val json = JSONObject(body)
        // On écarte les pages d'homonymie : leur "extrait" n'apprend rien au voyageur
        if (json.optString("type") == "disambiguation") return null
        return json.optString("extract").takeIf { it.isNotBlank() }
    }
}
