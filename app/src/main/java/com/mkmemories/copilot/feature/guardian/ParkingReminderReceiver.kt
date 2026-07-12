package com.mkmemories.copilot.feature.guardian

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.mkmemories.copilot.MainActivity
import com.mkmemories.copilot.R

/**
 * Rappel de stationnement (zone bleue / parcmètre) : UNE notification, à
 * l'heure choisie par l'utilisateur, jamais rien d'autre.
 */
class ParkingReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Rappel stationnement", NotificationManager.IMPORTANCE_HIGH),
        )
        manager.notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Stationnement : le temps file")
                .setContentText("Votre disque ou ticket arrive à échéance — pensez à la voiture.")
                .setAutoCancel(true)
                .setContentIntent(
                    PendingIntent.getActivity(
                        context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
                .build(),
        )
    }

    companion object {
        private const val CHANNEL_ID = "parking_reminder"
        private const val NOTIFICATION_ID = 72

        /** Programme le rappel unique dans [delayMinutes] minutes. */
        fun schedule(context: Context, delayMinutes: Int) {
            val alarm = context.getSystemService(AlarmManager::class.java)
            val pending = PendingIntent.getBroadcast(
                context, 0,
                Intent(context, ParkingReminderReceiver::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val at = System.currentTimeMillis() + delayMinutes * 60_000L
            // setWindow : pas besoin de permission alarme exacte, précision ± 10 min suffisante
            alarm.setWindow(AlarmManager.RTC_WAKEUP, at, 10 * 60_000L, pending)
        }
    }
}
