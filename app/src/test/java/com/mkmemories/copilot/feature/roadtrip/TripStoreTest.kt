package com.mkmemories.copilot.feature.roadtrip

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Le voyage planifié doit survivre au redémarrage sans perdre une miette. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TripStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val trip = Trip(
        name = "Grèce 2026",
        days = listOf(
            TripDay(
                LocalDate.of(2026, 9, 2),
                listOf(
                    TripStop("Hôtel Katikies", 36.4622, 25.3753, locality = "Oia", time = LocalTime.of(15, 30)),
                    TripStop("Plage Rouge", 36.3486, 25.3944, locality = "Santorin"),
                ),
            ),
        ),
    )

    @Test
    fun `round-trip complet - nom, dates, heures, localites, visites`() {
        TripStore(context).save(trip)
        assertEquals(trip, TripStore(context).load())
    }

    @Test
    fun `aucun voyage stocke = null (repli demo geree par TripRepository)`() {
        assertNull(TripStore(context).load())
    }

    @Test
    fun `donnee corrompue = null, jamais de plantage`() {
        context.getSharedPreferences("trip_store", Context.MODE_PRIVATE)
            .edit().putString("trip", "{pas du json").apply()
        assertNull(TripStore(context).load())
    }

    @Test
    fun `serialisation stable - les jours sont tries au rechargement`() {
        val shuffled = trip.copy(
            days = listOf(
                TripDay(LocalDate.of(2026, 9, 5), listOf(TripStop("Z", 1.0, 1.0))),
                TripDay(LocalDate.of(2026, 9, 1), listOf(TripStop("A", 2.0, 2.0))),
            ),
        )
        TripStore(context).save(shuffled)
        assertEquals(
            listOf(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5)),
            TripStore(context).load()!!.days.map { it.date },
        )
    }
}
