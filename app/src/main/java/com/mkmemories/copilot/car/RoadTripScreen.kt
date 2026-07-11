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
import com.mkmemories.copilot.feature.briefing.BriefingPlayer
import com.mkmemories.copilot.feature.briefing.WeatherBriefingGenerator
import com.mkmemories.copilot.feature.roadtrip.DayBriefing
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
 * Ergonomie conduite : repères numérotés dans l'ordre du voyage directement
 * sur la carte, un tap = handoff navigation, étapes visitées reléguées en
 * vert sous les étapes restantes, et le briefing météo à un tap dans la
 * barre d'actions — tout est faisable d'un coup d'œil.
 */
class RoadTripScreen(carContext: CarContext) : Screen(carContext) {

    private val briefingPlayer = BriefingPlayer(carContext)
    private var briefingLoading = false

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) = briefingPlayer.release()
        })
    }

    override fun onGetTemplate(): Template {
        val trip = TripRepository.currentTrip(carContext)
        val stops = trip.stopsFor(LocalDate.now())
        // Les étapes restantes d'abord : ce sont elles qu'on veut au premier regard
        val ordered = stops.withIndex().sortedBy { it.value.visited }

        val listBuilder = ItemList.Builder()
        if (stops.isEmpty()) {
            listBuilder.setNoItemsMessage("Aucune étape prévue aujourd'hui — profitez de la route !")
        } else {
            ordered.forEach { (index, stop) -> listBuilder.addItem(stopRow(index + 1, stop)) }
        }

        val nextStop = stops.firstOrNull { !it.visited } ?: stops.firstOrNull()

        return PlaceListMapTemplate.Builder()
            .setTitle(trip.name)
            .setHeaderAction(Action.APP_ICON)
            .setItemList(listBuilder.build())
            .apply {
                // Carte centrée sur la prochaine étape, marquée à l'or de l'emblème
                nextStop?.let {
                    setAnchor(
                        Place.Builder(CarLocation.create(it.latitude, it.longitude))
                            .setMarker(PlaceMarker.Builder().setColor(CarGold).build())
                            .build(),
                    )
                }
            }
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(
                        Action.Builder()
                            .setTitle("Briefing")
                            .setOnClickListener { playBriefing() }
                            .build(),
                    )
                    .build(),
            )
            .build()
    }

    /** Une étape : repère numéroté sur la carte, statut colorié, tap = navigation. */
    private fun stopRow(number: Int, stop: TripStop): Row {
        val marker = PlaceMarker.Builder()
            .setLabel("$number")
            .setColor(if (stop.visited) CarAuroraTeal else CarGold)
            .build()
        val place = Place.Builder(CarLocation.create(stop.latitude, stop.longitude))
            .setMarker(marker)
            .build()

        val status = if (stop.visited) {
            SpannableString("Visité ✓").apply {
                setSpan(
                    ForegroundCarColorSpan.create(CarAuroraTeal),
                    0, length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE,
                )
            }
        } else {
            val prefix = stop.timeLabel()?.let { "$it — " } ?: ""
            SpannableString("${prefix}Étape $number — appuyer pour y aller")
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

    /** Briefing météo + étapes du jour, lu dans les haut-parleurs sans quitter la route des yeux. */
    private fun playBriefing() {
        if (briefingLoading) return
        briefingLoading = true
        CarToast.makeText(carContext, "Briefing en préparation…", CarToast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val weather = try {
                // TODO v1.1 : utiliser la vraie position (FusedLocationProvider).
                WeatherBriefingGenerator.generate(latitude = 48.8566, longitude = 2.3522)
            } catch (e: Exception) {
                "Météo indisponible pour l'instant."
            }
            val stops = TripRepository.currentTrip(carContext).stopsFor(LocalDate.now())
            briefingPlayer.speak(DayBriefing.compose(weather, stops))
            briefingLoading = false
        }
    }
}
