package com.mkmemories.copilot.feature.guardian

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.mkmemories.copilot.MainActivity
import com.mkmemories.copilot.R
import com.mkmemories.copilot.feature.briefing.BriefingPlayer
import com.mkmemories.copilot.feature.dangerzones.RadarOpenDataRepository
import com.mkmemories.copilot.feature.dangerzones.ZoneAlertEngine
import com.mkmemories.copilot.feature.location.LocationProvider
import com.mkmemories.copilot.feature.roadtrip.TripRepository
import com.mkmemories.copilot.feature.roadtrip.TripStore
import com.mkmemories.copilot.feature.roadtrip.withUpdatedStop
import com.mkmemories.copilot.feature.settings.SettingsStore
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Le cœur battant de l'Ange gardien : service au premier plan pendant la
 * conduite. Démarre avec Android Auto (ou manuellement depuis le téléphone) :
 *  - capteur de choc branché → écran « Tout va bien ? » → escalade SOS ;
 *  - géofencing d'arrivée → étape visitée ✓ + « J'arrive bien » + suivante ;
 *  - zones de danger → alertes vocales pendant que Maps navigue ;
 *  - position mémorisée en continu (SOS et mémoire de stationnement).
 */
class DriveGuardService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var crashDetector: CrashDetector? = null
    private var tts: BriefingPlayer? = null
    private val arrivalWatcher = ArrivalWatcher()
    private var zoneEngine: ZoneAlertEngine? = null

    private val locationListener = LocationListener { onLocation(it) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        tts = BriefingPlayer(this)
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val settings = SettingsStore(this)
        startInForeground()
        running = true

        if (settings.guardianEnabled) {
            crashDetector = CrashDetector(this) { onPossibleCrash() }.also { it.start() }
        }

        if (settings.dangerZonesEnabled) {
            scope.launch(Dispatchers.IO) {
                RadarOpenDataRepository.refreshIfStale(this@DriveGuardService)
                val zones = RadarOpenDataRepository.cachedZones(this@DriveGuardService)
                zoneEngine = ZoneAlertEngine { zones }
            }
        }

        if (LocationProvider.hasPermission(this)) {
            val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            runCatching {
                val provider =
                    if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) LocationManager.GPS_PROVIDER
                    else LocationManager.NETWORK_PROVIDER
                manager.requestLocationUpdates(provider, UPDATE_INTERVAL_MS, UPDATE_DISTANCE_M, locationListener)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        crashDetector?.stop()
        runCatching {
            (getSystemService(Context.LOCATION_SERVICE) as LocationManager)
                .removeUpdates(locationListener)
        }
        tts?.release()
        scope.cancel()
        super.onDestroy()
    }

    // -- Événements de conduite ---------------------------------------------

    private fun onLocation(location: Location) {
        lastLocation = location

        // Zones de danger : alerte vocale légale, compatible avec toute navigation
        zoneEngine?.onLocation(location.latitude, location.longitude)?.let { alert ->
            tts?.speak(alert)
        }

        // Géofencing d'arrivée
        val store = TripStore(this)
        val trip = TripRepository.currentTrip(this)
        val today = LocalDate.now()
        val stops = trip.stopsFor(today)
        val reached = arrivalWatcher.onLocation(location.latitude, location.longitude, stops) ?: return

        // Étape visitée ✓, persistée pour le téléphone ET l'écran voiture
        val index = stops.indexOf(reached)
        store.save(trip.withUpdatedStop(today, index) { it.copy(visited = true) })

        val settings = SettingsStore(this)
        if (settings.arrivalSmsEnabled && settings.arrivalRecipients.isNotEmpty() && hasSmsPermission()) {
            runCatching {
                ArrivalNotifier.notifyArrival(reached.name, settings.arrivalRecipients, LocalTime.now())
            }
        }

        val next = trip.stopsFor(today).filter { !it.visited && it != reached }.firstOrNull()
        val nextText = next?.let { " Prochaine étape : ${it.name}." } ?: " C'était la dernière étape du jour, bravo !"
        tts?.speak("Étape atteinte : ${reached.name}.$nextText")
    }

    private fun onPossibleCrash() {
        // L'écran « Tout va bien ? » prend le relais (compte à rebours + SOS)
        startActivity(
            Intent(this, SosActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
    }

    private fun hasSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) ==
            PackageManager.PERMISSION_GRANTED

    // -- Notification de premier plan -----------------------------------------

    private fun startInForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Mode conduite", NotificationManager.IMPORTANCE_LOW),
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("MK Copilot veille sur votre route")
            .setContentText("Ange gardien actif : accident, arrivées, zones de danger")
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val CHANNEL_ID = "drive_guard"
        private const val NOTIFICATION_ID = 71
        private const val UPDATE_INTERVAL_MS = 15_000L
        private const val UPDATE_DISTANCE_M = 40f

        /** Dernière position vue en conduite — utilisée par le SOS et le parking. */
        @Volatile
        var lastLocation: Location? = null
            private set

        @Volatile
        var running: Boolean = false
            private set

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, DriveGuardService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, DriveGuardService::class.java))
        }
    }
}
