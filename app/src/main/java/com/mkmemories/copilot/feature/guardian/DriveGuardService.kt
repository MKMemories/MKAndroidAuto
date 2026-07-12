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
import android.telephony.SmsManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.mkmemories.copilot.MainActivity
import com.mkmemories.copilot.R
import com.mkmemories.copilot.feature.blackbox.BlackBox
import com.mkmemories.copilot.feature.briefing.BriefingPlayer
import com.mkmemories.copilot.feature.dangerzones.RadarOpenDataRepository
import com.mkmemories.copilot.feature.dangerzones.ZoneAlertEngine
import com.mkmemories.copilot.feature.drive.FatigueMonitor
import com.mkmemories.copilot.feature.drive.IceRisk
import com.mkmemories.copilot.feature.fuel.FuelPrices
import com.mkmemories.copilot.feature.guide.TouristGuide
import com.mkmemories.copilot.feature.journal.TripJournal
import com.mkmemories.copilot.feature.journal.TripLogEntry
import com.mkmemories.copilot.feature.location.LocationProvider
import com.mkmemories.copilot.feature.messaging.DriveMessaging
import com.mkmemories.copilot.feature.places.NearbyWiki
import com.mkmemories.copilot.feature.places.PlaceEnrichment
import com.mkmemories.copilot.feature.roadtrip.TripRepository
import com.mkmemories.copilot.feature.roadtrip.TripStore
import com.mkmemories.copilot.feature.roadtrip.withUpdatedStop
import com.mkmemories.copilot.feature.settings.Feature
import com.mkmemories.copilot.feature.settings.SettingsStore
import com.mkmemories.copilot.feature.weather.RouteWeather
import java.time.LocalDate
import java.time.LocalTime
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Le cœur battant de l'Ange gardien : service au premier plan pendant la
 * conduite. Une seule notification (permanente, silencieuse) — tout le reste
 * passe par la voix, cadencé par les moteurs pour ne jamais harceler.
 * Chaque fonction est gouvernée par son interrupteur des Réglages.
 */
class DriveGuardService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var crashDetector: CrashDetector? = null
    private var tts: BriefingPlayer? = null
    private var zoneEngine: ZoneAlertEngine? = null

    private val arrivalWatcher = ArrivalWatcher()
    private val fatigue = FatigueMonitor()
    private val tracking = TripTracking()
    private val guide = TouristGuide()
    internal val messaging = DriveMessaging()
    private val blackBox = BlackBox()

    private var startedAt = 0L
    private var traveledMeters = 0.0
    private var previousLocation: Location? = null

    private val locationListener = LocationListener { onLocation(it) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        current = this
        tts = BriefingPlayer(this)
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val settings = SettingsStore(this)
        startInForeground()
        running = true
        startedAt = System.currentTimeMillis()
        traveledMeters = 0.0
        previousLocation = null

        if (settings.guardianEnabled) {
            crashDetector = CrashDetector(this) { magnitude -> onPossibleCrash(magnitude) }
                .also { it.start() }
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

        departureBriefing(settings)
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        current = null
        crashDetector?.stop()
        runCatching {
            (getSystemService(Context.LOCATION_SERVICE) as LocationManager).removeUpdates(locationListener)
        }
        // Journal de bord : le trajet se clôt ici
        if (SettingsStore(this).isEnabled(Feature.TRIP_JOURNAL) && startedAt > 0) {
            TripJournal(this).record(
                TripLogEntry(startedAt, System.currentTimeMillis(), traveledMeters / 1000.0),
            )
        }
        tts?.release()
        scope.cancel()
        super.onDestroy()
    }

    // -- Briefing de départ (une seule fois, tout est meilleur-effort) ---------

    private fun departureBriefing(settings: SettingsStore) {
        scope.launch {
            val (lat, lng) = LocationProvider.coordinatesOrFallback(this@DriveGuardService)

            if (settings.isEnabled(Feature.ICE_ALERT)) {
                launch(Dispatchers.IO) {
                    runCatching {
                        val json = org.json.JSONObject(
                            java.net.URL(
                                "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lng" +
                                    "&current=temperature_2m,weather_code&timezone=auto",
                            ).readText(),
                        ).getJSONObject("current")
                        IceRisk.warning(
                            json.getDouble("temperature_2m"),
                            json.getInt("weather_code"),
                            Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                        )?.let { speak(it) }
                    }
                }
            }

            if (settings.isEnabled(Feature.ROUTE_WEATHER)) {
                launch(Dispatchers.IO) {
                    val stops = TripRepository.currentTrip(this@DriveGuardService).stopsFor(LocalDate.now())
                    RouteWeather.alertForRoute(stops)?.let { speak(it) }
                }
            }

            if (settings.isEnabled(Feature.FUEL_PRICES)) {
                launch(Dispatchers.IO) {
                    FuelPrices.cheapestNearby(lat, lng)?.let { speak(FuelPrices.announcement(it)) }
                }
            }
        }
    }

    // -- Flux de position --------------------------------------------------------

    private fun onLocation(location: Location) {
        lastLocation = location
        val settings = SettingsStore(this)

        // Distance parcourue (journal) + boîte noire
        previousLocation?.let {
            traveledMeters += ZoneAlertEngine.distanceMeters(
                it.latitude, it.longitude, location.latitude, location.longitude,
            )
        }
        previousLocation = location
        if (settings.isEnabled(Feature.BLACK_BOX)) {
            blackBox.record(
                BlackBox.Sample(
                    System.currentTimeMillis(), location.latitude, location.longitude,
                    if (location.hasSpeed()) location.speed else null, null,
                ),
            )
        }

        // Fatigue : cadencée par le moniteur lui-même
        if (settings.isEnabled(Feature.FATIGUE_ALERT)) {
            fatigue.check(
                System.currentTimeMillis() - startedAt,
                Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            )?.let { speak(it) }
        }

        // Zones de danger
        zoneEngine?.onLocation(location.latitude, location.longitude)?.let { speak(it) }

        // Voyage suivi par SMS
        if (settings.isEnabled(Feature.TRIP_TRACKING) &&
            settings.arrivalRecipients.isNotEmpty() && hasSmsPermission()
        ) {
            val next = TripRepository.currentTrip(this).stopsFor(LocalDate.now())
                .firstOrNull { !it.visited }?.name
            tracking.onLocation(
                System.currentTimeMillis(), location.latitude, location.longitude, next, LocalTime.now(),
            )?.let { message ->
                settings.arrivalRecipients.forEach { number ->
                    runCatching { sendSms(number, message) }
                }
            }
        }

        // Guide du territoire : au plus toutes les 10 min et 5 km
        if (settings.isEnabled(Feature.TOURIST_GUIDE) &&
            guide.shouldNarrate(System.currentTimeMillis(), location.latitude, location.longitude)
        ) {
            scope.launch(Dispatchers.IO) {
                val sites = NearbyWiki.around(location.latitude, location.longitude, radiusMeters = 8_000, limit = 3)
                val title = guide.pick(
                    System.currentTimeMillis(), location.latitude, location.longitude,
                    sites.map { it.title },
                ) ?: return@launch
                val summary = PlaceEnrichment.describe(title) ?: return@launch
                speak("Près d'ici : $title. ${summary.take(280)}")
            }
        }

        // Géofencing d'arrivée
        handleArrival(location, settings)
    }

    private fun handleArrival(location: Location, settings: SettingsStore) {
        val store = TripStore(this)
        val trip = TripRepository.currentTrip(this)
        val today = LocalDate.now()
        val stops = trip.stopsFor(today)
        val reached = arrivalWatcher.onLocation(location.latitude, location.longitude, stops) ?: return

        val index = stops.indexOf(reached)
        store.save(trip.withUpdatedStop(today, index) { it.copy(visited = true) })

        if (settings.arrivalSmsEnabled && settings.arrivalRecipients.isNotEmpty() && hasSmsPermission()) {
            runCatching { ArrivalNotifier.notifyArrival(reached.name, settings.arrivalRecipients, LocalTime.now()) }
        }

        val next = trip.stopsFor(today).filter { !it.visited && it != reached }.firstOrNull()
        val nextText = next?.let { " Prochaine étape : ${it.name}." } ?: " C'était la dernière étape du jour, bravo !"
        speak("Étape atteinte : ${reached.name}.$nextText")
    }

    // -- Choc --------------------------------------------------------------------

    private fun onPossibleCrash(magnitudeMs2: Float) {
        val settings = SettingsStore(this)
        if (settings.isEnabled(Feature.BLACK_BOX)) {
            blackBox.record(
                BlackBox.Sample(System.currentTimeMillis(), lastLocation?.latitude, lastLocation?.longitude, null, magnitudeMs2),
            )
            runCatching { blackBox.dump(this, System.currentTimeMillis(), magnitudeMs2) }
        }
        startActivity(
            Intent(this, SosActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
    }

    // -- SMS entrants (messagerie apaisée, appelée par SmsReceiver) ---------------

    fun handleIncomingSms(sender: String, body: String) {
        val settings = SettingsStore(this)
        if (settings.isEnabled(Feature.MESSAGE_READER)) {
            speak(messaging.spokenAnnouncement(sender, body))
        }
        if (settings.isEnabled(Feature.AUTO_REPLY) && hasSmsPermission()) {
            val next = TripRepository.currentTrip(this).stopsFor(LocalDate.now()).firstOrNull { !it.visited }
            messaging.autoReply(sender, System.currentTimeMillis(), next?.time, next?.name)
                ?.let { reply -> runCatching { sendSms(sender, reply) } }
        }
    }

    // -- Aides --------------------------------------------------------------------

    private fun speak(text: String) = tts?.speak(text) ?: Unit

    @Suppress("DEPRECATION")
    private fun sendSms(number: String, message: String) {
        SmsManager.getDefault().sendTextMessage(number, null, message, null, null)
    }

    private fun hasSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) ==
            PackageManager.PERMISSION_GRANTED

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
                    this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
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

        /** Instance vivante, pour le récepteur SMS. */
        @Volatile
        internal var current: DriveGuardService? = null
            private set

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, DriveGuardService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, DriveGuardService::class.java))
        }
    }
}
