package com.mkmemories.copilot.car

import android.text.SpannableString
import android.text.Spanned
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.ForegroundCarColorSpan
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.mkmemories.copilot.feature.ai.BriefingEnricher
import com.mkmemories.copilot.feature.briefing.BriefingPlayer
import com.mkmemories.copilot.feature.briefing.WeatherBriefingGenerator
import com.mkmemories.copilot.feature.diag.AppLog
import com.mkmemories.copilot.feature.location.LocationProvider
import com.mkmemories.copilot.feature.roadtrip.NavigationLauncher
import com.mkmemories.copilot.feature.roadtrip.TripRepository
import com.mkmemories.copilot.feature.roadtrip.TripStop
import com.mkmemories.copilot.feature.roadtrip.timeLabel
import java.time.LocalDate
import kotlinx.coroutines.launch

// Vert aurore de la charte, en couleur voiture (thème clair / sombre)
private val CarAuroraTeal = CarColor.createCustom(0xFF1FA97D.toInt(), 0xFF2EE6A8.toInt())

/**
 * Écran principal sur Android Auto : les étapes du jour du road trip.
 *
 * Template : ListTemplate (liste standard). Volontairement PAS de template
 * carte (PlaceListMapTemplate), qui exige la permission androidx.car.app.
 * MAP_TEMPLATES et une distance sur chaque ligne — deux contraintes qui le
 * rendaient fragile. La liste est fiable, sans permission spéciale.
 *
 * Ergonomie conduite : étapes numérotées et datées, « Étape suivante » en un
 * tap dans la barre d'actions, visitées en vert en fin de liste, briefing
 * vocal sans quitter la route.
 */
class RoadTripScreen(carContext: CarContext) : Screen(carContext) {

    // Créé à la première demande de briefing seulement.
    private var briefingPlayer: BriefingPlayer? = null
    private var briefingLoading = false

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) = invalidate() // ✓ visités à jour
            override fun onDestroy(owner: LifecycleOwner) {
                runCatching { briefingPlayer?.release() }
            }
        })
    }

    override fun onGetTemplate(): Template =
        try {
            buildTemplate()
        } catch (e: Exception) {
            AppLog.error("car", "onGetTemplate a échoué — repli affiché", e)
            ListTemplate.Builder()
                .setTitle("MK Copilot")
                .setHeaderAction(Action.APP_ICON)
                .setSingleList(
                    ItemList.Builder()
                        .setNoItemsMessage("Préparez votre voyage sur le téléphone, puis reconnectez-vous.")
                        .build(),
                )
                .build()
        }

    private fun buildTemplate(): Template {
        AppLog.i("car", "buildTemplate")
        val trip = TripRepository.currentTrip(carContext)
        val stops = trip.stopsFor(LocalDate.now())
        // Les étapes restantes d'abord : ce sont elles qu'on veut au premier regard
        val ordered = stops.withIndex().sortedBy { it.value.visited }

        val listBuilder = ItemList.Builder()
        if (stops.isEmpty()) {
            listBuilder.setNoItemsMessage(
                "Aucune étape aujourd'hui. Planifiez votre voyage sur le téléphone — profitez de la route !",
            )
        } else {
            ordered.forEach { (index, stop) -> listBuilder.addItem(stopRow(index + 1, stop)) }
        }

        val nextStop = stops.firstOrNull { !it.visited }

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

        return ListTemplate.Builder()
            .setTitle(trip.name)
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(listBuilder.build())
            .setActionStrip(actionStrip)
            .build()
    }

    /** Une étape : numéro dans le titre, heure + statut, tap = navigation. */
    private fun stopRow(number: Int, stop: TripStop): Row {
        val status: CharSequence = if (stop.visited) {
            SpannableString("Visité ✓").apply {
                setSpan(ForegroundCarColorSpan.create(CarAuroraTeal), 0, length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            }
        } else {
            val timePrefix = stop.timeLabel()?.let { "$it — " } ?: ""
            "${timePrefix}Appuyer pour lancer la navigation"
        }

        return Row.Builder()
            .setTitle("$number.  ${stop.name}")
            .addText(status)
            .setOnClickListener {
                if (stop.visited) {
                    CarToast.makeText(carContext, "Étape déjà visitée", CarToast.LENGTH_SHORT).show()
                } else {
                    NavigationLauncher.navigateFromCar(carContext, stop)
                }
            }
            .build()
    }

    /** Briefing météo localisé + étapes du jour, enrichi par l'IA (locale, puis Mistral). */
    private fun playBriefing() {
        if (briefingLoading) return
        briefingLoading = true
        CarToast.makeText(carContext, "Briefing en préparation…", CarToast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            try {
                val (latitude, longitude) = LocationProvider.coordinatesOrFallback(carContext)
                val weather = try {
                    WeatherBriefingGenerator.generate(latitude, longitude)
                } catch (e: Exception) {
                    "Météo indisponible pour l'instant."
                }
                val stops = TripRepository.currentTrip(carContext).stopsFor(LocalDate.now())
                val player = briefingPlayer ?: BriefingPlayer(carContext).also { briefingPlayer = it }
                player.speak(BriefingEnricher.enrich(carContext, weather, stops))
            } catch (e: Exception) {
                AppLog.error("car", "playBriefing a échoué", e)
                CarToast.makeText(carContext, "Briefing indisponible pour l'instant", CarToast.LENGTH_LONG).show()
            } finally {
                briefingLoading = false
            }
        }
    }
}
