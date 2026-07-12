package com.mkmemories.copilot.car

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.text.SpannableString
import android.text.Spanned
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ForegroundCarColorSpan
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.SectionedItemList
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
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
import com.mkmemories.copilot.feature.roadtrip.frenchLabel
import com.mkmemories.copilot.feature.roadtrip.timeLabel
import java.time.LocalDate
import kotlinx.coroutines.launch

// Couleurs de la charte
private val CarAuroraTeal = CarColor.createCustom(0xFF1FA97D.toInt(), 0xFF2EE6A8.toInt())
private const val GOLD = 0xFFE8B84B.toInt()
private const val TEAL = 0xFF2EE6A8.toInt()

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

        // Contrainte ListTemplate : au plus UNE action avec titre dans la barre.
        // On garde « Briefing » ; la navigation vers la prochaine étape se fait
        // en touchant la première ligne (les étapes restantes sont en tête).
        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("Briefing")
                    .setOnClickListener { playBriefing() }
                    .build(),
            )
            .build()

        val builder = ListTemplate.Builder()
            .setTitle(trip.name)
            .setHeaderAction(Action.APP_ICON)
            .setActionStrip(actionStrip)

        if (stops.isEmpty()) {
            builder.setSingleList(
                ItemList.Builder()
                    .setNoItemsMessage(
                        "Aucune étape aujourd'hui. Planifiez votre voyage sur le téléphone — profitez de la route !",
                    )
                    .build(),
            )
        } else {
            val listBuilder = ItemList.Builder()
            ordered.forEach { (index, stop) -> listBuilder.addItem(stopRow(index + 1, stop)) }
            // En-tête de section = la date du jour, en toutes lettres
            builder.addSectionedList(
                SectionedItemList.create(listBuilder.build(), LocalDate.now().frenchLabel()),
            )
        }

        return builder.build()
    }

    /** Une étape : repère numéroté (ou photo), heure · ville, statut, tap = navigation. */
    private fun stopRow(number: Int, stop: TripStop): Row {
        val builder = Row.Builder().setTitle(stop.name)

        // Ligne 1 : heure de rendez-vous · ville (si présentes et non redondantes)
        val locality = stop.locality?.takeIf { !stop.name.contains(it, ignoreCase = true) }
        val subtitle = listOfNotNull(stop.timeLabel(), locality).joinToString(" · ")
        if (subtitle.isNotBlank()) builder.addText(subtitle)

        // Ligne 2 : statut
        val status: CharSequence = if (stop.visited) {
            SpannableString("Visité ✓").apply {
                setSpan(ForegroundCarColorSpan.create(CarAuroraTeal), 0, length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            }
        } else {
            "Appuyer pour lancer la navigation"
        }
        builder.addText(status)

        // Image : photo de l'étape si attachée, sinon repère numéroté doré / vert
        (stop.photoPath?.let { photoIcon(it) } ?: numberedIcon(number, stop.visited))
            ?.let { builder.setImage(it, Row.IMAGE_TYPE_LARGE) }

        builder.setOnClickListener {
            if (stop.visited) {
                CarToast.makeText(carContext, "Étape déjà visitée", CarToast.LENGTH_SHORT).show()
            } else {
                NavigationLauncher.navigateFromCar(carContext, stop)
            }
        }
        return builder.build()
    }

    /** Pastille ronde numérotée (or si à venir, vert si visitée), dessinée en Bitmap. */
    private fun numberedIcon(number: Int, visited: Boolean): CarIcon? = try {
        val size = 128
        val color = if (visited) TEAL else GOLD
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val r = size / 2f
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; alpha = 38 }
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color; style = Paint.Style.STROKE; strokeWidth = 9f
        }
        canvas.drawCircle(r, r, r - 7, fill)
        canvas.drawCircle(r, r, r - 7, ring)
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color; textSize = 62f; textAlign = Paint.Align.CENTER; isFakeBoldText = true
        }
        canvas.drawText("$number", r, r - (text.descent() + text.ascent()) / 2, text)
        CarIcon.Builder(IconCompat.createWithBitmap(bmp)).build()
    } catch (e: Exception) {
        null
    }

    /** Vignette carrée de la photo d'étape, ou null si le fichier a disparu. */
    private fun photoIcon(path: String): CarIcon? = try {
        val options = BitmapFactory.Options().apply { inSampleSize = 4 }
        val src = BitmapFactory.decodeFile(path, options) ?: return null
        val side = minOf(src.width, src.height)
        val square = Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
        val scaled = Bitmap.createScaledBitmap(square, 128, 128, true)
        CarIcon.Builder(IconCompat.createWithBitmap(scaled)).build()
    } catch (e: Exception) {
        null
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
