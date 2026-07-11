package com.mkmemories.copilot.feature.guardian

import android.telephony.SmsManager
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Pack Ange gardien — "J'arrive bien" automatique.
 *
 * À l'arrivée à destination (détectée par le géofencing du road trip),
 * envoie un SMS aux proches choisis : "Bien arrivé à Lyon, 18 h 42".
 * Zéro serveur, zéro coût.
 */
object ArrivalNotifier {

    fun notifyArrival(destinationName: String, recipients: List<String>) {
        val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH'h'mm"))
        val message = "MK Copilot : bien arrivé à $destinationName, $time. 🚗"

        @Suppress("DEPRECATION")
        val smsManager = SmsManager.getDefault()
        recipients.forEach { number ->
            smsManager.sendTextMessage(number, null, message, null, null)
        }
    }
}
