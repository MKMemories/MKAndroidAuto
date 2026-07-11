package com.mkmemories.copilot.car

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

/** Point d'entrée Android Auto (Car App Library, catégorie POI). */
class CopilotCarAppService : CarAppService() {

    // TODO release : restreindre aux hosts officiels via allowlist
    // (androidx.car.app.R.array.hosts_allowlist_sample en debug uniquement).
    override fun createHostValidator(): HostValidator =
        HostValidator.ALLOW_ALL_HOSTS_VALIDATOR

    override fun onCreateSession(): Session = CopilotSession()
}

class CopilotSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen = RoadTripScreen(carContext)
}
