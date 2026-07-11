package com.mkmemories.copilot.feature.guardian

import android.telephony.SmsManager
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Pack Ange gardien — "J'arrive bien" automatique.
 *
 * À l'arrivée à destination (détectée par le géofencing du road trip),
 * envoie un SMS aux proches choisis : "Bien arrivé à Lyon, 18h42".
 * Zéro serveur, zéro coût.
 */
object ArrivalNotifier {

    fun notifyArrival(
        destinationName: String,
        recipients: List<String>,
        arrivalTime: LocalTime = LocalTime.now(),
        smsSender: (phoneNumber: String, message: String) -> Unit = ::sendSmsViaAndroid,
    ) {
        val message = arrivalMessage(destinationName, arrivalTime)
        recipients.forEach { number -> smsSender(number, message) }
    }

    /** Message envoyé aux proches, horodaté à la française ("18h42"). */
    internal fun arrivalMessage(destinationName: String, arrivalTime: LocalTime): String {
        val time = arrivalTime.format(DateTimeFormatter.ofPattern("HH'h'mm"))
        return "MK Copilot : bien arrivé à $destinationName, $time. 🚗"
    }

    @Suppress("DEPRECATION")
    private fun sendSmsViaAndroid(phoneNumber: String, message: String) {
        SmsManager.getDefault().sendTextMessage(phoneNumber, null, message, null, null)
    }
}
