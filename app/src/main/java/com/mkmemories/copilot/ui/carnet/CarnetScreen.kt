package com.mkmemories.copilot.ui.carnet

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mkmemories.copilot.feature.carnet.CarnetEntry
import com.mkmemories.copilot.feature.carnet.CarnetPhase
import com.mkmemories.copilot.feature.carnet.CarnetProgress
import com.mkmemories.copilot.feature.carnet.CarnetVoyage
import com.mkmemories.copilot.feature.carnet.EntryKind
import com.mkmemories.copilot.feature.carnet.GreeceOdyssey
import com.mkmemories.copilot.feature.carnet.nextDriveDay
import com.mkmemories.copilot.feature.carnet.toTrip
import com.mkmemories.copilot.feature.roadtrip.NavigationLauncher
import com.mkmemories.copilot.feature.roadtrip.TripStop
import com.mkmemories.copilot.feature.roadtrip.TripStore
import com.mkmemories.copilot.feature.roadtrip.frenchLabel
import com.mkmemories.copilot.feature.roadtrip.timeLabel
import com.mkmemories.copilot.ui.theme.BrandAuroraTeal
import com.mkmemories.copilot.ui.theme.BrandGold
import com.mkmemories.copilot.ui.theme.BrandGoldLight
import com.mkmemories.copilot.ui.theme.BrandIce
import com.mkmemories.copilot.ui.theme.BrandMist
import com.mkmemories.copilot.ui.theme.BrandNight
import com.mkmemories.copilot.ui.theme.BrandSurface
import com.mkmemories.copilot.ui.theme.BrandSurfaceHigh
import java.time.LocalDate

/**
 * « Carnet de voyage » — le compagnon de voyage magazine : chaque phase, chaque
 * trajet, ferry, location et logement. Les trajets voiture se lancent d'un tap
 * dans Google Maps, et l'ensemble alimente automatiquement la navigation
 * (accueil + Android Auto) via [TripStore].
 */
@Composable
fun CarnetScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val carnet = remember { GreeceOdyssey.itinerary() }
    val progress = remember { CarnetProgress(context) }
    val store = remember { TripStore(context) }

    // Première ouverture : on nourrit la navigation si aucun voyage n'est encore choisi.
    remember {
        if (store.load() == null) store.save(carnet.toTrip())
        true
    }

    var version by remember { mutableIntStateOf(0) }
    val allIds = remember(carnet) { carnet.entries.map { it.id } }
    val done = allIds.count { progress.isDone(it) }.also { version } // relit à chaque version

    fun navigateTo(entry: CarnetEntry) {
        val place = entry.destination ?: return
        try {
            NavigationLauncher.navigateFromPhone(
                context,
                TripStop(place.name, place.latitude, place.longitude, locality = place.locality),
            )
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Aucune app de navigation trouvée", Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler(onBack = onBack)

    Box(modifier = Modifier.fillMaxSize().background(BrandNight)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // Barre de navigation
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Retour", tint = Color.White)
                }
                Text("Carnet de voyage", style = MaterialTheme.typography.titleMedium, color = BrandGold)
            }

            HeaderHero(carnet)

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(16.dp))
                ProgressBanner(done = done, total = allIds.size)

                Spacer(Modifier.height(12.dp))
                NextDriveCard(carnet, onNavigate = ::navigateTo)

                Spacer(Modifier.height(20.dp))
                Text("L'ITINÉRAIRE", style = MaterialTheme.typography.labelSmall, color = BrandGold, letterSpacing = 2.sp)
                Spacer(Modifier.height(10.dp))
                version // dépendance de recomposition : le ruban se rafraîchit à chaque coche
                ItineraryRibbon(
                    carnet = carnet,
                    isEntryDone = { progress.isDone(it) },
                    onNodeNavigate = { phase -> phase.firstDrive?.let(::navigateTo) },
                )

                Spacer(Modifier.height(24.dp))
                Text("LE CARNET, JOUR PAR JOUR", style = MaterialTheme.typography.labelSmall, color = BrandGold, letterSpacing = 2.sp)

                carnet.phases.forEach { phase ->
                    Spacer(Modifier.height(22.dp))
                    PhaseBlock(
                        phase = phase,
                        isDone = { progress.isDone(it) },
                        onToggle = { id -> progress.toggle(id); version++ },
                        onNavigate = ::navigateTo,
                    )
                }

                Spacer(Modifier.height(24.dp))
                Text(
                    "Kalo Taxidi ! ✨",
                    style = MaterialTheme.typography.titleLarge,
                    color = BrandGoldLight,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(36.dp))
            }
        }
    }
}

@Composable
private fun HeaderHero(carnet: CarnetVoyage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(BrandSurface, BrandNight)),
            )
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Text(
            "CARNET DE VOYAGE PREMIUM",
            style = MaterialTheme.typography.labelSmall,
            color = BrandGold.copy(alpha = 0.85f),
            letterSpacing = 3.sp,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            carnet.title,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            lineHeight = 44.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(carnet.subtitle, style = MaterialTheme.typography.titleMedium, color = BrandIce)
        Spacer(Modifier.height(14.dp))
        Text(carnet.travelers, style = MaterialTheme.typography.bodyLarge, color = BrandGoldLight)
        Spacer(Modifier.height(2.dp))
        Text(
            "${carnet.start.frenchLabel()} — ${carnet.end.frenchLabel()}",
            style = MaterialTheme.typography.bodyMedium,
            color = BrandMist,
        )
    }
}

@Composable
private fun ProgressBanner(done: Int, total: Int) {
    val ratio = if (total == 0) 0f else done.toFloat() / total
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Progression du voyage", style = MaterialTheme.typography.labelLarge, color = BrandMist)
            Text("$done / $total étapes", style = MaterialTheme.typography.labelLarge, color = BrandAuroraTeal)
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(BrandSurfaceHigh),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Brush.horizontalGradient(listOf(BrandAuroraTeal, BrandIce))),
            )
        }
    }
}

@Composable
private fun NextDriveCard(carnet: CarnetVoyage, onNavigate: (CarnetEntry) -> Unit) {
    val today = LocalDate.now()
    val nextDay = carnet.nextDriveDay(today) ?: return
    val nextEntry = carnet.drives.firstOrNull { it.date == nextDay.date } ?: return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(BrandSurfaceHigh, BrandSurface)))
            .border(1.dp, BrandGold.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Text("PROCHAINE ROUTE", style = MaterialTheme.typography.labelSmall, color = BrandGold, letterSpacing = 2.sp)
        Spacer(Modifier.height(6.dp))
        Text(nextEntry.destination?.name.orEmpty(), style = MaterialTheme.typography.titleLarge, color = Color.White)
        Text(
            nextEntry.date.frenchLabel() + (nextEntry.timeLabelOrNull()?.let { " · $it" } ?: ""),
            style = MaterialTheme.typography.bodyMedium,
            color = BrandMist,
        )
        Spacer(Modifier.height(12.dp))
        FilledPill("Naviguer avec Google Maps") { onNavigate(nextEntry) }
    }
}

@Composable
private fun PhaseBlock(
    phase: CarnetPhase,
    isDone: (String) -> Boolean,
    onToggle: (String) -> Unit,
    onNavigate: (CarnetEntry) -> Unit,
) {
    Column {
        Text(phase.dateLabel.uppercase(), style = MaterialTheme.typography.labelSmall, color = BrandIce, letterSpacing = 2.sp)
        Spacer(Modifier.height(2.dp))
        Text(phase.title, style = MaterialTheme.typography.headlineSmall, color = BrandGold)
        Text(phase.place, style = MaterialTheme.typography.bodyMedium, color = BrandMist)
        Spacer(Modifier.height(12.dp))

        phase.entries.forEach { entry ->
            EntryRow(entry, done = isDone(entry.id), onToggle = { onToggle(entry.id) }, onNavigate = onNavigate)
            Spacer(Modifier.height(10.dp))
        }

        if (phase.inspirations.isNotEmpty()) {
            InspirationCard(phase.inspirations)
        }
    }
}

@Composable
private fun EntryRow(
    entry: CarnetEntry,
    done: Boolean,
    onToggle: () -> Unit,
    onNavigate: (CarnetEntry) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BrandSurface.copy(alpha = 0.92f))
            .border(1.dp, entry.kind.accent().copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(14.dp)
            .animateContentSize(),
    ) {
        // Pastille « fait / à faire »
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (done) BrandAuroraTeal else BrandSurfaceHigh)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (done) "✓" else entry.kind.emoji(), fontSize = 14.sp, color = if (done) BrandNight else Color.White)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (done) BrandMist else Color.White,
                )
                entry.timeLabelOrNull()?.let {
                    Spacer(Modifier.width(8.dp))
                    Text(it, style = MaterialTheme.typography.labelMedium, color = entry.kind.accent())
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(entry.detail, style = MaterialTheme.typography.bodyMedium, color = BrandMist)

            if (entry.destination != null) {
                Spacer(Modifier.height(10.dp))
                OutlinedPill("Naviguer") { onNavigate(entry) }
            }
        }
    }
}

@Composable
private fun InspirationCard(items: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BrandGold.copy(alpha = 0.08f))
            .border(1.dp, BrandGold.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Text("✨ Carnet d'inspirations", style = MaterialTheme.typography.titleSmall, color = BrandGoldLight)
        Spacer(Modifier.height(8.dp))
        items.forEach { item ->
            Row {
                Text("•  ", color = BrandGold)
                Text(item, style = MaterialTheme.typography.bodyMedium, color = BrandMist)
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun FilledPill(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(BrandAuroraTeal)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🧭  $text", style = MaterialTheme.typography.labelLarge, color = BrandNight)
    }
}

@Composable
private fun OutlinedPill(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.5.dp, BrandAuroraTeal.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🧭  $text", style = MaterialTheme.typography.labelLarge, color = BrandAuroraTeal)
    }
}

private fun EntryKind.emoji(): String = when (this) {
    EntryKind.DRIVE -> "🧭"
    EntryKind.FLIGHT -> "✈️"
    EntryKind.FERRY -> "⛴️"
    EntryKind.CAR -> "🚗"
    EntryKind.LODGING -> "🏡"
}

private fun EntryKind.accent(): Color = when (this) {
    EntryKind.DRIVE -> BrandAuroraTeal
    EntryKind.FLIGHT -> BrandIce
    EntryKind.FERRY -> BrandIce
    EntryKind.CAR -> BrandGold
    EntryKind.LODGING -> BrandGoldLight
}

private fun CarnetEntry.timeLabelOrNull(): String? =
    TripStop("", 0.0, 0.0, time = time).timeLabel()
