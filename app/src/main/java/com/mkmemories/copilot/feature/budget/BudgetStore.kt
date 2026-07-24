package com.mkmemories.copilot.feature.budget

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistance locale du budget : statut de paiement modifié par l'utilisateur
 * (surcharge du pré-rempli) et prestations ajoutées. Appliqué par-dessus le
 * budget de base pour donner la liste effective.
 */
class BudgetStore(context: Context) {

    private val prefs = context.getSharedPreferences("budget_store", Context.MODE_PRIVATE)

    fun setPaid(id: String, paid: Boolean) {
        val map = readPaid()
        map[id] = paid
        prefs.edit { putString(KEY_PAID, JSONObject(map as Map<*, *>).toString()) }
    }

    fun addCustom(item: BudgetItem) {
        val list = readCustom().apply { add(item) }
        writeCustom(list)
    }

    fun removeCustom(id: String) {
        writeCustom(readCustom().filterNot { it.id == id }.toMutableList())
        val map = readPaid().apply { remove(id) }
        prefs.edit { putString(KEY_PAID, JSONObject(map as Map<*, *>).toString()) }
    }

    fun clear() = prefs.edit { clear() }

    /** Liste effective = base (statut surchargé) + prestations ajoutées. */
    fun effectiveItems(base: List<BudgetItem>): List<BudgetItem> {
        val paid = readPaid()
        val decoratedBase = base.map { item -> paid[item.id]?.let { item.copy(paid = it) } ?: item }
        val custom = readCustom().map { item -> paid[item.id]?.let { item.copy(paid = it) } ?: item }
        return decoratedBase + custom
    }

    // — Sérialisation —

    private fun readPaid(): MutableMap<String, Boolean> {
        val out = mutableMapOf<String, Boolean>()
        val root = JSONObject(prefs.getString(KEY_PAID, "{}") ?: "{}")
        root.keys().forEach { out[it] = root.getBoolean(it) }
        return out
    }

    private fun readCustom(): MutableList<BudgetItem> {
        val out = mutableListOf<BudgetItem>()
        val arr = JSONArray(prefs.getString(KEY_CUSTOM, "[]") ?: "[]")
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                BudgetItem(
                    id = o.getString("id"),
                    category = o.optString("category"),
                    label = o.optString("label"),
                    dates = o.optString("dates"),
                    schedule = o.optString("schedule"),
                    amountCents = o.getLong("amountCents"),
                    paid = o.optBoolean("paid", false),
                    note = o.optString("note"),
                    custom = true,
                ),
            )
        }
        return out
    }

    private fun writeCustom(list: List<BudgetItem>) {
        val arr = JSONArray()
        list.forEach { e ->
            arr.put(
                JSONObject()
                    .put("id", e.id)
                    .put("category", e.category)
                    .put("label", e.label)
                    .put("dates", e.dates)
                    .put("schedule", e.schedule)
                    .put("amountCents", e.amountCents)
                    .put("paid", e.paid)
                    .put("note", e.note),
            )
        }
        prefs.edit { putString(KEY_CUSTOM, arr.toString()) }
    }

    private companion object {
        const val KEY_PAID = "paid"
        const val KEY_CUSTOM = "custom"
    }
}
