package com.mkmemories.copilot.feature.update

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Mise à jour intégrée : interroge la dernière Release GitHub du dépôt
 * (API publique, sans authentification) et signale un APK plus récent que
 * le build installé. Meilleur effort : toute erreur rend `null`, l'app ne
 * dérange jamais l'utilisateur pour un problème de réseau.
 */
data class UpdateInfo(
    val buildNumber: Int,
    val title: String,
    val downloadUrl: String,
)

object UpdateChecker {

    private val TAG_PATTERN = Regex("""apk-build-(\d+)""")

    suspend fun check(
        currentBuild: Int,
        baseUrl: String = "https://api.github.com",
    ): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            // La liste (et non /releases/latest) : ce dernier ignore les préreleases
            val connection = URL("$baseUrl/repos/MKMemories/MKAndroidAuto/releases?per_page=10")
                .openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 8_000
                connection.readTimeout = 8_000
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                connection.setRequestProperty("User-Agent", "MKCopilot/0.1")
                if (connection.responseCode != 200) return@withContext null
                parse(connection.inputStream.bufferedReader().readText(), currentBuild)
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Le plus grand numéro de build strictement supérieur au build installé. */
    internal fun parse(body: String, currentBuild: Int): UpdateInfo? {
        val releases = JSONArray(body)
        var best: UpdateInfo? = null
        for (r in 0 until releases.length()) {
            val candidate = parseRelease(releases.getJSONObject(r)) ?: continue
            if (candidate.buildNumber > (best?.buildNumber ?: currentBuild)) {
                best = candidate
            }
        }
        return best
    }

    private fun parseRelease(release: JSONObject): UpdateInfo? {
        val buildNumber = TAG_PATTERN.find(release.optString("tag_name"))
            ?.groupValues?.get(1)?.toIntOrNull() ?: return null
        val assets = release.optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (asset.optString("name").endsWith(".apk")) {
                return UpdateInfo(
                    buildNumber = buildNumber,
                    title = release.optString("name").ifBlank { "Build $buildNumber" },
                    downloadUrl = asset.getString("browser_download_url"),
                )
            }
        }
        return null
    }
}
