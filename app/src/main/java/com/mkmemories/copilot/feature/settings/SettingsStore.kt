package com.mkmemories.copilot.feature.settings

import android.content.Context
import androidx.core.content.edit

/**
 * Réglages de l'app, stockés localement (zéro serveur). C'est le point
 * d'ancrage des trois piliers de sécurité : sans contacts d'urgence pas de
 * SOS, sans proches pas de « J'arrive bien », sans clé pas de Mistral.
 */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** Contacts d'urgence du SOS (numéros de téléphone). */
    var emergencyContacts: List<String>
        get() = readList(KEY_EMERGENCY)
        set(value) = writeList(KEY_EMERGENCY, value)

    /** Proches prévenus par « J'arrive bien ». */
    var arrivalRecipients: List<String>
        get() = readList(KEY_ARRIVAL)
        set(value) = writeList(KEY_ARRIVAL, value)

    /** Clé API Mistral personnelle (IA de repli, optionnelle). */
    var mistralKey: String?
        get() = prefs.getString(KEY_MISTRAL, null)?.takeIf { it.isNotBlank() }
        set(value) = prefs.edit { putString(KEY_MISTRAL, value?.trim()) }

    /** Ange gardien (détection d'accident + SOS) pendant la conduite. */
    var guardianEnabled: Boolean
        get() = prefs.getBoolean(KEY_GUARDIAN, true)
        set(value) = prefs.edit { putBoolean(KEY_GUARDIAN, value) }

    /** Alertes vocales de zones de danger pendant la conduite. */
    var dangerZonesEnabled: Boolean
        get() = prefs.getBoolean(KEY_DANGER, true)
        set(value) = prefs.edit { putBoolean(KEY_DANGER, value) }

    /** SMS « J'arrive bien » automatique à l'arrivée d'une étape. */
    var arrivalSmsEnabled: Boolean
        get() = prefs.getBoolean(KEY_ARRIVAL_SMS, false)
        set(value) = prefs.edit { putBoolean(KEY_ARRIVAL_SMS, value) }

    /** Interrupteur d'une fonctionnalité du registre [Feature]. */
    fun isEnabled(feature: Feature): Boolean =
        prefs.getBoolean("feature_${feature.name}", feature.defaultEnabled)

    fun setEnabled(feature: Feature, enabled: Boolean) =
        prefs.edit { putBoolean("feature_${feature.name}", enabled) }

    fun addEmergencyContact(number: String) {
        val cleaned = number.trim()
        if (cleaned.isNotBlank() && cleaned !in emergencyContacts) {
            emergencyContacts = emergencyContacts + cleaned
        }
    }

    fun removeEmergencyContact(number: String) {
        emergencyContacts = emergencyContacts - number
    }

    fun addArrivalRecipient(number: String) {
        val cleaned = number.trim()
        if (cleaned.isNotBlank() && cleaned !in arrivalRecipients) {
            arrivalRecipients = arrivalRecipients + cleaned
        }
    }

    fun removeArrivalRecipient(number: String) {
        arrivalRecipients = arrivalRecipients - number
    }

    private fun readList(key: String): List<String> =
        prefs.getString(key, "")?.split(SEPARATOR)?.filter { it.isNotBlank() } ?: emptyList()

    private fun writeList(key: String, value: List<String>) =
        prefs.edit { putString(key, value.joinToString(SEPARATOR)) }

    private companion object {
        const val SEPARATOR = "\u001F" // séparateur d'unité ASCII : impossible dans un numéro
        const val KEY_EMERGENCY = "emergency_contacts"
        const val KEY_ARRIVAL = "arrival_recipients"
        const val KEY_MISTRAL = "mistral_key"
        const val KEY_GUARDIAN = "guardian_enabled"
        const val KEY_DANGER = "danger_zones_enabled"
        const val KEY_ARRIVAL_SMS = "arrival_sms_enabled"
    }
}
