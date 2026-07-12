package com.mkmemories.copilot.feature.roadtrip

/**
 * Import de voyages : KML (Google My Maps), GPX (waypoints) et JSON MK Copilot.
 * Parseurs textuels volontairement tolérants — un fichier exporté par un
 * autre outil ne doit jamais faire planter l'import ; les points sans
 * coordonnées sont ignorés.
 */
object TripFileParser {

    data class ImportedPlace(val name: String, val latitude: Double, val longitude: Double)

    /** Détecte le format et parse ; liste vide si rien d'exploitable. */
    fun parse(content: String): List<ImportedPlace> = when {
        content.contains("<kml", ignoreCase = true) -> parseKml(content)
        content.contains("<gpx", ignoreCase = true) -> parseGpx(content)
        content.trimStart().startsWith("{") -> parseTripJson(content)
        else -> emptyList()
    }

    /** KML : chaque <Placemark> avec <name> et <coordinates>lng,lat[,alt]</coordinates>. */
    internal fun parseKml(content: String): List<ImportedPlace> {
        val placemarks = Regex("<Placemark[\\s\\S]*?</Placemark>", RegexOption.IGNORE_CASE)
            .findAll(content)
        return placemarks.mapNotNull { match ->
            val block = match.value
            val name = Regex("<name>\\s*(?:<!\\[CDATA\\[)?(.*?)(?:]]>)?\\s*</name>", RegexOption.IGNORE_CASE)
                .find(block)?.groupValues?.get(1)?.trim().orEmpty()
            val coords = Regex("<coordinates>([\\s\\S]*?)</coordinates>", RegexOption.IGNORE_CASE)
                .find(block)?.groupValues?.get(1)?.trim() ?: return@mapNotNull null
            // Premier point seulement (un Placemark ligne/polygone donne son départ)
            val first = coords.split(Regex("\\s+")).firstOrNull() ?: return@mapNotNull null
            val parts = first.split(",")
            val lng = parts.getOrNull(0)?.toDoubleOrNull() ?: return@mapNotNull null
            val lat = parts.getOrNull(1)?.toDoubleOrNull() ?: return@mapNotNull null
            ImportedPlace(name.ifBlank { "Étape importée" }, lat, lng)
        }.toList()
    }

    /** GPX : waypoints <wpt lat=".." lon=".."><name>..</name></wpt>. */
    internal fun parseGpx(content: String): List<ImportedPlace> {
        val waypoints = Regex("<wpt\\s+[^>]*>[\\s\\S]*?</wpt>|<wpt\\s+[^>]*/>", RegexOption.IGNORE_CASE)
            .findAll(content)
        return waypoints.mapNotNull { match ->
            val block = match.value
            val lat = Regex("lat=\"([^\"]+)\"").find(block)?.groupValues?.get(1)?.toDoubleOrNull()
                ?: return@mapNotNull null
            val lng = Regex("lon=\"([^\"]+)\"").find(block)?.groupValues?.get(1)?.toDoubleOrNull()
                ?: return@mapNotNull null
            val name = Regex("<name>(.*?)</name>", RegexOption.IGNORE_CASE)
                .find(block)?.groupValues?.get(1)?.trim().orEmpty()
            ImportedPlace(name.ifBlank { "Étape importée" }, lat, lng)
        }.toList()
    }

    /** JSON MK Copilot (fichier de partage) : toutes les étapes de tous les jours. */
    internal fun parseTripJson(content: String): List<ImportedPlace> = try {
        TripStore.fromJson(content).days.flatMap { day ->
            day.stops.map { ImportedPlace(it.name, it.latitude, it.longitude) }
        }
    } catch (e: Exception) {
        emptyList()
    }

    /** Le voyage complet si le fichier est un partage MK Copilot, sinon null. */
    fun parseFullTrip(content: String): Trip? = try {
        if (content.trimStart().startsWith("{")) TripStore.fromJson(content) else null
    } catch (e: Exception) {
        null
    }
}
