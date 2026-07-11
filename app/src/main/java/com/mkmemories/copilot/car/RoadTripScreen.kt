package com.mkmemories.copilot.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import com.mkmemories.copilot.feature.roadtrip.NavigationLauncher
import com.mkmemories.copilot.feature.roadtrip.TripRepository
import java.time.LocalDate

/**
 * Écran principal sur Android Auto : les étapes du jour du road trip.
 * Un tap sur une étape → handoff vers Google Maps sur l'écran voiture.
 */
class RoadTripScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val trip = TripRepository.currentTrip()
        val stops = trip.stopsFor(LocalDate.now())

        val listBuilder = ItemList.Builder()
        if (stops.isEmpty()) {
            listBuilder.setNoItemsMessage("Aucune étape prévue aujourd'hui")
        } else {
            stops.forEach { stop ->
                listBuilder.addItem(
                    Row.Builder()
                        .setTitle(stop.name)
                        .addText(if (stop.visited) "Visité ✓" else "Appuyer pour lancer la navigation")
                        .setOnClickListener { NavigationLauncher.navigateFromCar(carContext, stop) }
                        .build(),
                )
            }
        }

        return ListTemplate.Builder()
            .setTitle("${trip.name} — aujourd'hui")
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(listBuilder.build())
            .build()
    }
}
