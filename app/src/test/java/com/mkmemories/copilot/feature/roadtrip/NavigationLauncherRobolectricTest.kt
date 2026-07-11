package com.mkmemories.copilot.feature.roadtrip

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Comportement réel côté Android : Uri valide et intent de navigation correct. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NavigationLauncherRobolectricTest {

    @Test
    fun `dayItineraryUrl produit un Uri https valide vers Google Maps`() {
        val uri = NavigationLauncher.dayItineraryUrl(
            listOf(TripStop("Chambord", 47.6161, 1.517), TripStop("Blois", 47.5861, 1.3359)),
        )
        assertEquals("https", uri.scheme)
        assertEquals("www.google.com", uri.host)
        assertEquals("1", uri.getQueryParameter("api"))
        assertEquals("47.5861,1.3359", uri.getQueryParameter("destination"))
        assertEquals("driving", uri.getQueryParameter("travelmode"))
        // Une fois décodé par Android, le waypoint retrouve son pipe
        assertEquals("47.6161,1.517", uri.getQueryParameter("waypoints"))
    }

    @Test
    fun `navigateFromPhone lance un intent de guidage Google Maps`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        NavigationLauncher.navigateFromPhone(app, TripStop("Chambord", 47.6161, 1.517))

        val intent = shadowOf(app).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("google.navigation:q=47.6161,1.517", intent.data.toString())
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }
}
