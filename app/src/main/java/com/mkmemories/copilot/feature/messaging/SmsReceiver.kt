package com.mkmemories.copilot.feature.messaging

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.mkmemories.copilot.feature.guardian.DriveGuardService

/**
 * SMS entrants pendant la conduite : transmis au service de conduite qui
 * décide (lecture vocale, réponse automatique) selon les interrupteurs.
 * Hors conduite, ce récepteur ne fait strictement rien.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val service = DriveGuardService.current ?: return // pas en conduite : silence

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        // Un SMS long arrive en plusieurs parties : on les regroupe par expéditeur
        messages.groupBy { it.displayOriginatingAddress ?: "" }
            .forEach { (sender, parts) ->
                if (sender.isBlank()) return@forEach
                val body = parts.joinToString("") { it.displayMessageBody ?: "" }
                if (body.isNotBlank()) service.handleIncomingSms(sender, body)
            }
    }
}
