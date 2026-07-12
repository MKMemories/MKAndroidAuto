package com.mkmemories.copilot.car

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mkmemories.copilot.feature.guardian.DriveGuardService
import com.mkmemories.copilot.feature.guardian.ParkingMemory
import com.mkmemories.copilot.feature.location.LocationProvider

/** Point d'entrée Android Auto (Car App Library, catégorie POI). */
class CopilotCarAppService : CarAppService() {

    // TODO release : restreindre aux hosts officiels via allowlist
    // (androidx.car.app.R.array.hosts_allowlist_sample en debug uniquement).
    override fun createHostValidator(): HostValidator =
        HostValidator.ALLOW_ALL_HOSTS_VALIDATOR

    override fun onCreateSession(): Session = CopilotSession()
}

class CopilotSession : Session() {

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                // Déconnexion de la voiture = on est garé : position mémorisée.
                // Meilleur effort — jamais rien qui puisse faire planter la session.
                runCatching {
                    val location = DriveGuardService.lastLocation ?: LocationProvider.lastKnown(carContext)
                    location?.let {
                        ParkingMemory(carContext).saveParkingSpot(it.latitude, it.longitude, it.time)
                    }
                }
            }
        })
        // NOTE : on ne démarre PAS le service de conduite ici. Démarrer un
        // foreground service depuis la session Android Auto viole les
        // restrictions Android 12+ (démarrage en arrière-plan) et le type
        // « location » exige une permission qu'Android Auto ne peut pas
        // demander → crash/ANR. L'Ange gardien est démarré depuis le
        // téléphone (bouton « Prendre la route »), contexte où c'est permis.
    }

    override fun onCreateScreen(intent: Intent): Screen = RoadTripScreen(carContext)
}
