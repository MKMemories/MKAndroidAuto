package com.mkmemories.copilot.car

import android.text.SpannableString
import android.text.Spanned
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarLocation
import androidx.car.app.model.Distance
import androidx.car.app.model.DistanceSpan
import androidx.car.app.model.ForegroundCarColorSpan
import androidx.car.app.model.ItemList
import androidx.car.app.model.Metadata
import androidx.car.app.model.Place
import androidx.car.app.model.PlaceListMapTemplate
import androidx.car.app.model.PlaceMarker
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.mkmemories.copilot.feature.ai.BriefingEnricher
import com.mkmemories.copilot.feature.briefing.BriefingPlayer
import com.mkmemories.copilot.feature.briefing.WeatherBriefingGenerator
import com.mkmemories.copilot.feature.dangerzones.ZoneAlertEngine
import com.mkmemories.copilot.feature.guardian.DriveGuardService
import com.mkmemories.copilot.feature.location.LocationProvider
import com.mkmemories.copilot.feature.roadtrip.NavigationLauncher
import com.mkmemories.copilot.feature.roadtrip.TripRepository
import com.mkmemories.copilot.feature.roadtrip.TripStop
import com.mkmemories.copilot.feature.roadtrip.timeLabel
import java.time.LocalDate
import kotlinx.coroutines.launch

// Or bruni et vert aurore de la charte, en couleurs voiture (thème clair / sombre)
private val CarGold = CarColor.createCustom(0xFFE8B84B.toInt(), 0xFFE8B84B.toInt())
private val CarAuroraTeal = CarColor.createCustom(0xFF1FA97D.toInt(), 0xFF2EE6A8.toInt())

/**
 * Écran principal sur Android Auto : carte + étapes du jour du road trip.
 *
 * Ergonomie conduite : repères numérotés sur la carte, distance réelle vers
 * chaque étape, « Étape suivante » en un tap dans la barre d'actions, étapes
 * visitées en vert en fin de liste, briefing vocal sans quitter la route.
 */
class RoadTripScreen(carContext: CarContext) : Screen(carContext) {

    private val briefingPlayer = BriefingPlayer(carContext)
    private var briefingLoading = false

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) = invalidate() // distances et ✓ à jour
            override fun onDestroy(owner: LifecycleOwner) = briefingPlayer.release()
        })
    }

    override fun onGetTemplate(): Template {
        val trip = TripRepository.currentTrip(carContext)
        val stops = trip.stopsFor(LocalDate.now())
        val here = DriveGuardService.lastLocation ?: LocationProvider.lastKnown(carContext)
        // Les étapes restantes d'abord : ce sont elles qu'on veut au premier regard
        val ordered = stops.withIndex().sortedBy { it.value.visited }

        val listBuilder = ItemList.Builder()
        if (stops.isEmpty()) {
            listBuilder.setNoItemsMessage(
                "Aucune étape aujourd'hui. Planifiez votre voyage sur le téléphone — profitez de la route !",
            )
        } else {
            ordered.forEach { (index, stop) ->
                listBuilder.addItem(stopRow(index + 1, stop, here?.latitude, here?.longitude))
            }
        }

        val nextStop = stops.firstOrNull { !it.visited }
        val anchorStop = nextStop ?: stops.firstOrNull()

        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("Briefing")
                    .setOnClickListener { playBriefing() }
                    .build(),
            )
            .apply {
                nextStop?.let { next ->
                    addAction(
                        Action.Builder()
                            .setTitle("▶ Étape suivante")
                            .setOnClickListener { NavigationLauncher.navigateFromCar(carContext, next) }
                            .build(),
                    )
                }
            }
            .build()

        return PlaceListMapTemplate.Builder()
            .setTitle(trip.name)
            .setHeaderAction(Action.APP_ICON)
            .setItemList(listBuilder.build())
            .apply {
                anchorStop?.let {
                    setAnchor(
                        Place.Builder(CarLocation.create(it.latitude, it.longitude))
                            .setMarker(PlaceMarker.Builder().setColor(CarGold).build())
                            .build(),
                    )
                }
            }
            .setActionStrip(actionStrip)
            .build()
    }

    /** Une étape : repère numéroté, distance réelle, heure, statut colorié. */
    private fun stopRow(number: Int, stop: TripStop, hereLat: Double?, hereLng: Double?): Row {
        val marker = PlaceMarker.Builder()
            .setLabel("$number")
            .setColor(if (stop.visited) CarAuroraTeal else CarGold)
            .build()
        val place = Place.Builder(CarLocation.create(stop.latitude, stop.longitude))
            .setMarker(marker)
            .build()

        val status: CharSequence = if (stop.visited) {
            SpannableString("Visité ✓").apply {
                setSpan(ForegroundCarColorSpan.create(CarAuroraTeal), 0, length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            }
        } else {
            val timePrefix = stop.timeLabel()?.let { "$it — " } ?: ""
            if (hereLat != null && hereLng != null) {
                // "~ 12 km — 09h30 — appuyer pour y aller", distance rendue par l'hôte
                val meters = ZoneAlertEngine.distanceMeters(hereLat, hereLng, stop.latitude, stop.longitude)
                val distance =
                    if (meters >= 1_000) Distance.create(meters / 1_000, Distance.UNIT_KILOMETERS)
                    else Distance.create(meters, Distance.UNIT_METERS)
                SpannableString("  — ${timePrefix}appuyer pour y aller").apply {
                    setSpan(DistanceSpan.create(distance), 0, 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                }
            } else {
                SpannableString("${timePrefix}Étape $number — appuyer pour y aller")
            }
        }

        return Row.Builder()
            .setTitle(stop.name)
            .addText(status)
            .setOnClickListener {
                if (stop.visited) {
                    CarToast.makeText(carContext, "Étape déjà visitée", CarToast.LENGTH_SHORT).show()
                } else {
                    NavigationLauncher.navigateFromCar(carContext, stop)
                }
            }
            .setMetadata(Metadata.Builder().setPlace(place).build())
            .build()
    }

    /** Briefing météo localisé + étapes du jour, enrichi par l'IA (locale, puis Mistral). */
    private fun playBriefing() {
        if (briefingLoading) return
        briefingLoading = true
        CarToast.makeText(carContext, "Briefing en préparation…", CarToast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val (latitude, longitude) = LocationProvider.coordinatesOrFallback(carContext)
            val weather = try {
                WeatherBriefingGenerator.generate(latitude, longitude)
            } catch (e: Exception) {
                "Météo indisponible pour l'instant."
            }
            val stops = TripRepository.currentTrip(carContext).stopsFor(LocalDate.now())
            briefingPlayer.speak(BriefingEnricher.enrich(carContext, weather, stops))
            briefingLoading = false
        }
    }
}
