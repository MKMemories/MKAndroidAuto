package com.mkmemories.copilot.feature.carnet

import android.content.Context
import androidx.core.content.edit
import java.time.LocalDate
import java.time.LocalTime
import org.json.JSONArray
import org.json.JSONObject

/**
 * Couche d'édition du carnet : liens et photos ajoutés à chaque étape, et
 * nouvelles étapes / lieux à visiter créés par l'utilisateur. 100 % local,
 * appliqué par-dessus le carnet de base pour produire le carnet effectif.
 */
class CarnetStore(context: Context) {

    private val prefs = context.getSharedPreferences("carnet_edits", Context.MODE_PRIVATE)

    // — Liens —
    fun addLink(entryId: String, link: CarnetLink) {
        val map = readLinks()
        map.getOrPut(entryId) { mutableListOf() }.add(link)
        writeLinks(map)
    }

    fun removeLink(entryId: String, index: Int) {
        val map = readLinks()
        map[entryId]?.let { if (index in it.indices) it.removeAt(index) }
        writeLinks(map)
    }

    // — Photos —
    fun addPhoto(entryId: String, path: String) {
        val map = readPhotos()
        map.getOrPut(entryId) { mutableListOf() }.add(path)
        writePhotos(map)
    }

    fun removePhoto(entryId: String, path: String) {
        val map = readPhotos()
        map[entryId]?.remove(path)
        writePhotos(map)
    }

    // — Étapes ajoutées —
    fun addCustom(entry: CustomEntry) {
        val list = readCustom().apply { add(entry) }
        writeCustom(list)
    }

    fun removeCustom(id: String) {
        val list = readCustom().filterNot { it.id == id }.toMutableList()
        writeCustom(list)
        // nettoyage des liens/photos orphelins
        readLinks().also { it.remove(id); writeLinks(it) }
        readPhotos().also { it.remove(id); writePhotos(it) }
    }

    fun clear() = prefs.edit { clear() }

    /**
     * Applique l'édition sur un carnet de base et renvoie le carnet effectif :
     * liens + photos greffés sur chaque étape, étapes ajoutées insérées dans
     * leur phase (triées par heure).
     */
    fun overlayOnto(base: CarnetVoyage): CarnetVoyage {
        val links = readLinks()
        val photos = readPhotos()
        val custom = readCustom().groupBy { it.phaseId }

        fun decorate(entry: CarnetEntry): CarnetEntry = entry.copy(
            links = entry.links + (links[entry.id]?.toList() ?: emptyList()),
            photos = entry.photos + (photos[entry.id]?.toList() ?: emptyList()),
        )

        val phases = base.phases.map { phase ->
            val added = custom[phase.id].orEmpty().map { decorate(it.toEntry()) }
            val merged = (phase.entries.map(::decorate) + added)
                .sortedWith(compareBy({ it.date }, { it.time ?: LocalTime.MAX }))
            phase.copy(entries = merged)
        }
        return base.copy(phases = phases)
    }

    // — Sérialisation —

    private fun readLinks(): MutableMap<String, MutableList<CarnetLink>> {
        val out = mutableMapOf<String, MutableList<CarnetLink>>()
        val root = JSONObject(prefs.getString(KEY_LINKS, "{}") ?: "{}")
        root.keys().forEach { id ->
            val arr = root.getJSONArray(id)
            val list = mutableListOf<CarnetLink>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(CarnetLink(o.getString("label"), o.getString("url")))
            }
            out[id] = list
        }
        return out
    }

    private fun writeLinks(map: Map<String, List<CarnetLink>>) {
        val root = JSONObject()
        map.filterValues { it.isNotEmpty() }.forEach { (id, list) ->
            val arr = JSONArray()
            list.forEach { arr.put(JSONObject().put("label", it.label).put("url", it.url)) }
            root.put(id, arr)
        }
        prefs.edit { putString(KEY_LINKS, root.toString()) }
    }

    private fun readPhotos(): MutableMap<String, MutableList<String>> {
        val out = mutableMapOf<String, MutableList<String>>()
        val root = JSONObject(prefs.getString(KEY_PHOTOS, "{}") ?: "{}")
        root.keys().forEach { id ->
            val arr = root.getJSONArray(id)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) list.add(arr.getString(i))
            out[id] = list
        }
        return out
    }

    private fun writePhotos(map: Map<String, List<String>>) {
        val root = JSONObject()
        map.filterValues { it.isNotEmpty() }.forEach { (id, list) ->
            root.put(id, JSONArray(list))
        }
        prefs.edit { putString(KEY_PHOTOS, root.toString()) }
    }

    private fun readCustom(): MutableList<CustomEntry> {
        val out = mutableListOf<CustomEntry>()
        val arr = JSONArray(prefs.getString(KEY_CUSTOM, "[]") ?: "[]")
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                CustomEntry(
                    id = o.getString("id"),
                    phaseId = o.getString("phaseId"),
                    title = o.getString("title"),
                    detail = o.optString("detail"),
                    date = LocalDate.parse(o.getString("date")),
                    time = o.optString("time").takeIf { it.isNotBlank() }?.let { LocalTime.parse(it) },
                    latitude = if (o.has("lat")) o.getDouble("lat") else null,
                    longitude = if (o.has("lng")) o.getDouble("lng") else null,
                    locality = o.optString("locality").takeIf { it.isNotBlank() },
                ),
            )
        }
        return out
    }

    private fun writeCustom(list: List<CustomEntry>) {
        val arr = JSONArray()
        list.forEach { e ->
            val o = JSONObject()
                .put("id", e.id)
                .put("phaseId", e.phaseId)
                .put("title", e.title)
                .put("detail", e.detail)
                .put("date", e.date.toString())
                .putOpt("time", e.time?.toString())
                .putOpt("locality", e.locality)
            if (e.latitude != null && e.longitude != null) {
                o.put("lat", e.latitude).put("lng", e.longitude)
            }
            arr.put(o)
        }
        prefs.edit { putString(KEY_CUSTOM, arr.toString()) }
    }

    private companion object {
        const val KEY_LINKS = "links"
        const val KEY_PHOTOS = "photos"
        const val KEY_CUSTOM = "custom"
    }
}

/** Étape / lieu à visiter ajouté par l'utilisateur. */
data class CustomEntry(
    val id: String,
    val phaseId: String,
    val title: String,
    val detail: String,
    val date: LocalDate,
    val time: LocalTime? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locality: String? = null,
) {
    fun toEntry(): CarnetEntry = CarnetEntry(
        id = id,
        kind = EntryKind.VISIT,
        title = title,
        detail = detail,
        date = date,
        time = time,
        destination = if (latitude != null && longitude != null) {
            CarnetPlace(title, latitude, longitude, locality)
        } else {
            null
        },
        custom = true,
    )
}
