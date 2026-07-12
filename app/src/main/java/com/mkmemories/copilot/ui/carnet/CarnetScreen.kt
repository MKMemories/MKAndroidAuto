package com.mkmemories.copilot.ui.carnet

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.mkmemories.copilot.feature.carnet.CarnetEntry
import com.mkmemories.copilot.feature.carnet.CarnetLink
import com.mkmemories.copilot.feature.carnet.CarnetPhase
import com.mkmemories.copilot.feature.carnet.CarnetProgress
import com.mkmemories.copilot.feature.carnet.CarnetStore
import com.mkmemories.copilot.feature.carnet.CarnetVoyage
import com.mkmemories.copilot.feature.carnet.CustomEntry
import com.mkmemories.copilot.feature.carnet.EntryKind
import com.mkmemories.copilot.feature.carnet.GreeceOdyssey
import com.mkmemories.copilot.feature.carnet.nextDriveDay
import com.mkmemories.copilot.feature.carnet.pdf.CarnetPdfExporter
import com.mkmemories.copilot.feature.carnet.toTrip
import com.mkmemories.copilot.feature.places.PlaceSearch
import com.mkmemories.copilot.feature.places.PlaceSuggestion
import com.mkmemories.copilot.feature.roadtrip.NavigationLauncher
import com.mkmemories.copilot.feature.roadtrip.TripStop
import com.mkmemories.copilot.feature.roadtrip.TripStore
import com.mkmemories.copilot.feature.roadtrip.frenchLabel
import com.mkmemories.copilot.feature.roadtrip.timeLabel
import com.mkmemories.copilot.ui.theme.BrandAuroraTeal
import com.mkmemories.copilot.ui.theme.BrandEmber
import com.mkmemories.copilot.ui.theme.BrandGold
import com.mkmemories.copilot.ui.theme.BrandGoldLight
import com.mkmemories.copilot.ui.theme.BrandIce
import com.mkmemories.copilot.ui.theme.BrandMist
import com.mkmemories.copilot.ui.theme.BrandNight
import com.mkmemories.copilot.ui.theme.BrandSurface
import com.mkmemories.copilot.ui.theme.BrandSurfaceHigh
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * « Carnet de voyage » — compagnon magazine, désormais éditable : chaque étape
 * accepte des liens et des photos, on peut ajouter des lieux à visiter, et le
 * tout s'exporte en carnet de souvenirs PDF. L'ensemble alimente la navigation
 * (accueil + Android Auto) via [TripStore].
 */
@Composable
fun CarnetScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val base = remember { GreeceOdyssey.itinerary() }
    val edits = remember { CarnetStore(context) }
    val progress = remember { CarnetProgress(context) }
    val tripStore = remember { TripStore(context) }

    var version by remember { mutableIntStateOf(0) }
    val carnet = remember(version) { edits.overlayOnto(base) }

    // À chaque modification, on ré-alimente la navigation avec le carnet effectif.
    remember(version) {
        runCatching { tripStore.save(carnet.toTrip()) }
        true
    }

    var editingId by remember { mutableStateOf<String?>(null) }
    var addingToPhase by remember { mutableStateOf<String?>(null) }
    var photoTargetId by remember { mutableStateOf<String?>(null) }
    var exporting by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val target = photoTargetId
        photoTargetId = null
        if (uri == null || target == null) return@rememberLauncherForActivityResult
        val path = runCatching {
            val dir = File(context.filesDir, "photos").apply { mkdirs() }
            val file = File(dir, "carnet-${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } }
            file.absolutePath
        }.getOrNull()
        if (path != null) { edits.addPhoto(target, path); version++ }
        else Toast.makeText(context, "Photo inaccessible", Toast.LENGTH_SHORT).show()
    }

    fun pickPhoto(entryId: String) {
        photoTargetId = entryId
        photoPicker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun navigateTo(entry: CarnetEntry) {
        val place = entry.destination ?: return
        try {
            NavigationLauncher.navigateFromPhone(context, TripStop(place.name, place.latitude, place.longitude, locality = place.locality))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Aucune app de navigation trouvée", Toast.LENGTH_SHORT).show()
        }
    }

    fun openLink(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Lien invalide", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportPdf() {
        if (exporting) return
        exporting = true
        scope.launch {
            val result = runCatching {
                val file = withContext(Dispatchers.IO) { CarnetPdfExporter.export(context, carnet) }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "${carnet.title} — Carnet de souvenirs")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(share, "Partager le carnet de souvenirs"))
            }
            exporting = false
            if (result.isFailure) Toast.makeText(context, "Export impossible", Toast.LENGTH_SHORT).show()
        }
    }

    val done = carnet.entries.count { progress.isDone(it.id) }

    BackHandler(onBack = onBack)

    Box(modifier = Modifier.fillMaxSize().background(BrandNight)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 12.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Retour", tint = Color.White)
                }
                Text("Carnet de voyage", style = MaterialTheme.typography.titleMedium, color = BrandGold, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(BrandGold.copy(alpha = 0.16f))
                        .clickable(enabled = !exporting, onClick = ::exportPdf)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (exporting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BrandGold, strokeWidth = 2.dp)
                    } else {
                        Text("📕 PDF souvenir", style = MaterialTheme.typography.labelLarge, color = BrandGold)
                    }
                }
            }

            HeaderHero(carnet)

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(16.dp))
                ProgressBanner(done = done, total = carnet.entries.size)

                Spacer(Modifier.height(12.dp))
                NextDriveCard(carnet, onNavigate = ::navigateTo)

                Spacer(Modifier.height(20.dp))
                Text("L'ITINÉRAIRE", style = MaterialTheme.typography.labelSmall, color = BrandGold, letterSpacing = 2.sp)
                Spacer(Modifier.height(10.dp))
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
                        onEdit = { editingId = it },
                        onOpenLink = ::openLink,
                        onAddPlace = { addingToPhase = phase.id },
                    )
                }

                Spacer(Modifier.height(24.dp))
                Text("Kalo Taxidi ! ✨", style = MaterialTheme.typography.titleLarge, color = BrandGoldLight, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(36.dp))
            }
        }
    }

    // — Éditeur d'étape (liens + photos + suppression) —
    editingId?.let { id ->
        val entry = carnet.entries.firstOrNull { it.id == id }
        if (entry == null) editingId = null
        else EntryEditorDialog(
            entry = entry,
            onDismiss = { editingId = null },
            onAddLink = { label, url -> edits.addLink(id, CarnetLink(label, url)); version++ },
            onRemoveLink = { index -> edits.removeLink(id, index); version++ },
            onAddPhoto = { pickPhoto(id) },
            onRemovePhoto = { path -> edits.removePhoto(id, path); version++ },
            onDeleteEntry = if (entry.custom) {
                { edits.removeCustom(id); version++; editingId = null }
            } else {
                null
            },
        )
    }

    // — Ajout d'un lieu à visiter —
    addingToPhase?.let { phaseId ->
        val phase = carnet.phases.first { it.id == phaseId }
        AddPlaceDialog(
            phase = phase,
            defaultDate = phase.entries.firstOrNull()?.date ?: carnet.start,
            onDismiss = { addingToPhase = null },
            onConfirm = { entry -> edits.addCustom(entry); version++; addingToPhase = null },
        )
    }
}

// ---------------------------------------------------------------------------
// Contenu
// ---------------------------------------------------------------------------

@Composable
private fun HeaderHero(carnet: CarnetVoyage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(BrandSurface, BrandNight)))
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Text("CARNET DE VOYAGE PREMIUM", style = MaterialTheme.typography.labelSmall, color = BrandGold.copy(alpha = 0.85f), letterSpacing = 3.sp)
        Spacer(Modifier.height(10.dp))
        Text(carnet.title, fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.White, lineHeight = 44.sp)
        Spacer(Modifier.height(6.dp))
        Text(carnet.subtitle, style = MaterialTheme.typography.titleMedium, color = BrandIce)
        Spacer(Modifier.height(14.dp))
        Text(carnet.travelers, style = MaterialTheme.typography.bodyLarge, color = BrandGoldLight)
        Spacer(Modifier.height(2.dp))
        Text("${carnet.start.frenchLabel()} — ${carnet.end.frenchLabel()}", style = MaterialTheme.typography.bodyMedium, color = BrandMist)
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
        Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(BrandSurfaceHigh)) {
            Box(modifier = Modifier.fillMaxWidth(ratio).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Brush.horizontalGradient(listOf(BrandAuroraTeal, BrandIce))))
        }
    }
}

@Composable
private fun NextDriveCard(carnet: CarnetVoyage, onNavigate: (CarnetEntry) -> Unit) {
    val today = LocalDate.now()
    val nextDay = carnet.nextDriveDay(today) ?: return
    val nextEntry = carnet.navStops.firstOrNull { it.date == nextDay.date } ?: return

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
    onEdit: (String) -> Unit,
    onOpenLink: (String) -> Unit,
    onAddPlace: () -> Unit,
) {
    Column {
        Text(phase.dateLabel.uppercase(), style = MaterialTheme.typography.labelSmall, color = BrandIce, letterSpacing = 2.sp)
        Spacer(Modifier.height(2.dp))
        Text(phase.title, style = MaterialTheme.typography.headlineSmall, color = BrandGold)
        Text(phase.place, style = MaterialTheme.typography.bodyMedium, color = BrandMist)
        Spacer(Modifier.height(12.dp))

        phase.entries.forEach { entry ->
            EntryRow(entry, done = isDone(entry.id), onToggle = { onToggle(entry.id) }, onNavigate = onNavigate, onEdit = { onEdit(entry.id) }, onOpenLink = onOpenLink)
            Spacer(Modifier.height(10.dp))
        }

        AddPlaceRow(onAddPlace)

        if (phase.inspirations.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
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
    onEdit: () -> Unit,
    onOpenLink: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BrandSurface.copy(alpha = 0.92f))
            .border(1.dp, entry.kind.accent().copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(14.dp)
            .animateContentSize(),
    ) {
        Row {
            Box(
                modifier = Modifier.size(28.dp).clip(RoundedCornerShape(14.dp)).background(if (done) BrandAuroraTeal else BrandSurfaceHigh).clickable(onClick = onToggle),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (done) "✓" else entry.kind.emoji(), fontSize = 14.sp, color = if (done) BrandNight else Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.title, style = MaterialTheme.typography.titleMedium, color = if (done) BrandMist else Color.White)
                    entry.timeLabelOrNull()?.let {
                        Spacer(Modifier.width(8.dp))
                        Text(it, style = MaterialTheme.typography.labelMedium, color = entry.kind.accent())
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(entry.detail, style = MaterialTheme.typography.bodyMedium, color = BrandMist)
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Rounded.Edit, contentDescription = "Modifier l'étape", tint = BrandGold, modifier = Modifier.size(18.dp))
            }
        }

        // Liens
        if (entry.links.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            entry.links.forEach { link ->
                Text(
                    "🔗 ${link.label}",
                    style = MaterialTheme.typography.labelLarge,
                    color = BrandIce,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onOpenLink(link.url) }.padding(vertical = 3.dp),
                )
            }
        }

        // Photos
        if (entry.photos.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                entry.photos.take(3).forEach { path -> PhotoThumb(path) }
                if (entry.photos.size > 3) {
                    Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)).background(BrandSurfaceHigh), contentAlignment = Alignment.Center) {
                        Text("+${entry.photos.size - 3}", color = BrandMist)
                    }
                }
            }
        }

        if (entry.destination != null) {
            Spacer(Modifier.height(10.dp))
            OutlinedPill("Naviguer") { onNavigate(entry) }
        }
    }
}

@Composable
private fun PhotoThumb(path: String) {
    val bitmap = remember(path) {
        runCatching {
            BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = 4 })?.asImageBitmap()
        }.getOrNull()
    }
    Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)).background(BrandSurfaceHigh)) {
        bitmap?.let { Image(bitmap = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
    }
}

@Composable
private fun AddPlaceRow(onAddPlace: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, BrandAuroraTeal.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .clickable(onClick = onAddPlace)
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, tint = BrandAuroraTeal, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Ajouter un lieu à visiter", style = MaterialTheme.typography.labelLarge, color = BrandAuroraTeal)
    }
}

@Composable
private fun InspirationCard(items: List<String>) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(BrandGold.copy(alpha = 0.08f)).border(1.dp, BrandGold.copy(alpha = 0.25f), RoundedCornerShape(16.dp)).padding(14.dp),
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

// ---------------------------------------------------------------------------
// Dialogues
// ---------------------------------------------------------------------------

@Composable
private fun EntryEditorDialog(
    entry: CarnetEntry,
    onDismiss: () -> Unit,
    onAddLink: (String, String) -> Unit,
    onRemoveLink: (Int) -> Unit,
    onAddPhoto: () -> Unit,
    onRemovePhoto: (String) -> Unit,
    onDeleteEntry: (() -> Unit)?,
) {
    var label by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Terminé", color = BrandGold) } },
        dismissButton = onDeleteEntry?.let { { TextButton(onClick = it) { Text("Supprimer l'étape", color = BrandEmber) } } },
        title = { Text(entry.title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Liens", style = MaterialTheme.typography.labelLarge, color = BrandIce)
                entry.links.forEachIndexed { index, link ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔗 ${link.label}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { onRemoveLink(index) }) { Text("✕", color = BrandEmber) }
                    }
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Intitulé (Réservation, Billet…)") }, singleLine = true, colors = dialogFieldColors(), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Lien (https://…)") }, singleLine = true, colors = dialogFieldColors(), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                TextButton(
                    onClick = {
                        val l = label.trim().ifBlank { "Lien" }
                        val u = url.trim()
                        if (u.isNotBlank()) { onAddLink(l, if (u.startsWith("http")) u else "https://$u"); label = ""; url = "" }
                    },
                ) { Text("+ Ajouter le lien", color = BrandAuroraTeal) }

                Spacer(Modifier.height(14.dp))
                Text("Photos (${entry.photos.size})", style = MaterialTheme.typography.labelLarge, color = BrandIce)
                if (entry.photos.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                        entry.photos.take(3).forEach { path ->
                            Box(contentAlignment = Alignment.TopEnd) {
                                PhotoThumb(path)
                                Text("✕", color = BrandEmber, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(BrandNight.copy(alpha = 0.6f)).clickable { onRemovePhoto(path) }.padding(horizontal = 4.dp))
                            }
                        }
                    }
                }
                TextButton(onClick = onAddPhoto) { Text("+ Ajouter une photo", color = BrandAuroraTeal) }
            }
        },
    )
}

@Composable
private fun AddPlaceDialog(
    phase: CarnetPhase,
    defaultDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (CustomEntry) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    var timeText by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<PlaceSuggestion>>(emptyList()) }
    var chosen by remember { mutableStateOf<PlaceSuggestion?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    val time = runCatching { LocalTime.parse(timeText.trim().replace('h', ':').trim(':')) }.getOrNull()
                    onConfirm(
                        CustomEntry(
                            id = "custom-${System.currentTimeMillis()}",
                            phaseId = phase.id,
                            title = title.trim(),
                            detail = detail.trim(),
                            date = defaultDate,
                            time = time,
                            latitude = chosen?.latitude,
                            longitude = chosen?.longitude,
                            locality = chosen?.locality,
                        ),
                    )
                },
            ) { Text("Ajouter", color = BrandGold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler", color = BrandMist) } },
        title = { Text("Lieu à visiter — ${phase.title}") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Nom du lieu") }, singleLine = true, colors = dialogFieldColors(), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = detail, onValueChange = { detail = it }, label = { Text("Note (optionnel)") }, colors = dialogFieldColors(), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = timeText, onValueChange = { timeText = it }, label = { Text("Heure (ex. 10h30, optionnel)") }, singleLine = true, colors = dialogFieldColors(), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                Text("Localiser (pour la navigation)", style = MaterialTheme.typography.labelMedium, color = BrandIce)
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        if (it.length >= 3) scope.launch { suggestions = runCatching { PlaceSearch.search(it) }.getOrDefault(emptyList()) }
                    },
                    label = { Text("Rechercher une adresse / un lieu") },
                    singleLine = true,
                    colors = dialogFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                chosen?.let {
                    Text("📍 ${it.name} — ${it.detail}", style = MaterialTheme.typography.labelMedium, color = BrandAuroraTeal, modifier = Modifier.padding(top = 4.dp))
                }
                suggestions.take(4).forEach { s ->
                    Text(
                        "• ${s.name} — ${s.detail}",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandMist,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable {
                            chosen = s
                            if (title.isBlank()) title = s.name
                            suggestions = emptyList()
                            query = s.name
                        }.padding(6.dp),
                    )
                }
            }
        },
    )
}

// ---------------------------------------------------------------------------
// Aides UI
// ---------------------------------------------------------------------------

@Composable
private fun FilledPill(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(22.dp)).background(BrandAuroraTeal).clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { Text("🧭  $text", style = MaterialTheme.typography.labelLarge, color = BrandNight) }
}

@Composable
private fun OutlinedPill(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(20.dp)).border(1.5.dp, BrandAuroraTeal.copy(alpha = 0.7f), RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { Text("🧭  $text", style = MaterialTheme.typography.labelLarge, color = BrandAuroraTeal) }
}

@Composable
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = BrandIce,
    unfocusedBorderColor = BrandMist.copy(alpha = 0.4f),
    focusedLabelColor = BrandIce,
    unfocusedLabelColor = BrandMist,
    cursorColor = BrandIce,
)

private fun EntryKind.emoji(): String = when (this) {
    EntryKind.DRIVE -> "🧭"
    EntryKind.FLIGHT -> "✈️"
    EntryKind.FERRY -> "⛴️"
    EntryKind.CAR -> "🚗"
    EntryKind.LODGING -> "🏡"
    EntryKind.VISIT -> "📍"
}

private fun EntryKind.accent(): Color = when (this) {
    EntryKind.DRIVE -> BrandAuroraTeal
    EntryKind.FLIGHT -> BrandIce
    EntryKind.FERRY -> BrandIce
    EntryKind.CAR -> BrandGold
    EntryKind.LODGING -> BrandGoldLight
    EntryKind.VISIT -> BrandAuroraTeal
}

private fun CarnetEntry.timeLabelOrNull(): String? = TripStop("", 0.0, 0.0, time = time).timeLabel()
