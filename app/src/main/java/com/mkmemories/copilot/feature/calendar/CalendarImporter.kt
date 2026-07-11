package com.mkmemories.copilot.feature.calendar

import android.content.Context
import android.provider.CalendarContract
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Import des rendez-vous depuis l'agenda du téléphone — y compris Google
 * Agenda, dont les événements sont synchronisés sur l'appareil. Aucune clé
 * API, aucun OAuth : lecture locale via le CalendarProvider Android
 * (permission READ_CALENDAR demandée à l'utilisateur au premier import).
 */
object CalendarImporter {

    data class CalendarEvent(
        val title: String,
        /** Lieu tel que saisi dans l'agenda — géocodé ensuite via la recherche de lieux. */
        val location: String?,
        /** Heure de début, null pour les événements "journée entière". */
        val time: LocalTime?,
    )

    fun eventsOn(context: Context, date: LocalDate): List<CalendarEvent> {
        val zone = ZoneId.systemDefault()
        val begin = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(begin.toString())
            .appendPath(end.toString())
            .build()

        val events = mutableListOf<CalendarEvent>()
        context.contentResolver.query(
            uri,
            arrayOf(
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.EVENT_LOCATION,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.ALL_DAY,
            ),
            null,
            null,
            "${CalendarContract.Instances.BEGIN} ASC",
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val title = cursor.getString(0)?.takeIf { it.isNotBlank() } ?: continue
                val allDay = cursor.getInt(3) == 1
                events.add(
                    CalendarEvent(
                        title = title,
                        location = cursor.getString(1)?.takeIf { it.isNotBlank() },
                        time = if (allDay) {
                            null
                        } else {
                            Instant.ofEpochMilli(cursor.getLong(2)).atZone(zone).toLocalTime()
                        },
                    ),
                )
            }
        }
        return events
    }
}
