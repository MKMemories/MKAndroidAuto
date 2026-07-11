package com.mkmemories.copilot.feature.guardian

import android.location.Location
import android.telephony.SmsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pack Ange gardien — escalade SOS après détection d'accident.
 *
 * Séquence : alerte sonore "Accident détecté — tout va bien ?" + compte à
 * rebours de [COUNTDOWN_SECONDS]. Sans réponse du conducteur, SMS automatique
 * aux contacts d'urgence avec la position GPS. Zéro serveur : fonctionne même
 * sans données mobiles (zones blanches).
 */
class SosManager(
    private val scope: CoroutineScope,
    private val emergencyContacts: () -> List<String>,
    private val lastKnownLocation: () -> Location?,
    private val onCountdownTick: (secondsLeft: Int) -> Unit = {},
    private val smsSender: (phoneNumber: String, message: String) -> Unit = ::sendSmsViaAndroid,
) {

    private var countdownJob: Job? = null

    /** Démarre le compte à rebours ; le conducteur peut l'annuler via [cancel]. */
    fun startCountdown() {
        if (countdownJob?.isActive == true) return
        countdownJob = scope.launch {
            for (secondsLeft in COUNTDOWN_SECONDS downTo 1) {
                onCountdownTick(secondsLeft)
                delay(1_000)
            }
            sendSos()
        }
    }

    /** "Tout va bien" — le conducteur a répondu, on annule tout. */
    fun cancel() {
        countdownJob?.cancel()
        countdownJob = null
    }

    private fun sendSos() {
        val location = lastKnownLocation()
        val message = sosMessage(location?.latitude, location?.longitude)
        emergencyContacts().forEach { number -> smsSender(number, message) }
        // TODO v1.1 : afficher l'appel 112 pré-composé sur l'écran du téléphone.
    }

    companion object {
        const val COUNTDOWN_SECONDS = 30

        /** Message SOS envoyé aux contacts d'urgence, avec lien position si connue. */
        internal fun sosMessage(latitude: Double?, longitude: Double?): String {
            val position =
                if (latitude != null && longitude != null) {
                    "https://maps.google.com/?q=$latitude,$longitude"
                } else {
                    "position indisponible"
                }
            return "⚠️ SOS MK Copilot : un accident a peut-être eu lieu. " +
                "Dernière position : $position"
        }

        @Suppress("DEPRECATION")
        private fun sendSmsViaAndroid(phoneNumber: String, message: String) {
            SmsManager.getDefault().sendTextMessage(phoneNumber, null, message, null, null)
        }
    }
}
