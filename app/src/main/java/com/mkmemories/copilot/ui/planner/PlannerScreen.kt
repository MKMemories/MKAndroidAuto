package com.mkmemories.copilot.ui.planner

import android.Manifest
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.mkmemories.copilot.feature.calendar.CalendarImporter
import com.mkmemories.copilot.feature.places.PlaceSearch
import com.mkmemories.copilot.feature.places.PlaceSuggestion
import com.mkmemories.copilot.feature.roadtrip.Trip
import com.mkmemories.copilot.feature.roadtrip.TripDay
import com.mkmemories.copilot.feature.roadtrip.TripRepository
import com.mkmemories.copilot.feature.roadtrip.TripStop
import com.mkmemories.copilot.feature.roadtrip.TripStore
import com.mkmemories.copilot.feature.roadtrip.frenchLabel
import com.mkmemories.copilot.feature.roadtrip.pdf.TripPdfExporter
import com.mkmemories.copilot.feature.roadtrip.timeLabel
import com.mkmemories.copilot.feature.roadtrip.withMovedStop
import com.mkmemories.copilot.feature.roadtrip.withStop
import com.mkmemories.copilot.feature.roadtrip.withUpdatedStop
import com.mkmemories.copilot.feature.roadtrip.withoutStop
import com.mkmemories.copilot.ui.theme.BrandAuroraTeal
import com.mkmemories.copilot.ui.theme.BrandEmber
import com.mkmemories.copilot.ui.theme.BrandGold
import com.mkmemories.copilot.ui.theme.BrandIce
import com.mkmemories.copilot.ui.theme.BrandMist
import com.mkmemories.copilot.ui.theme.BrandNight
import com.mkmemories.copilot.ui.theme.BrandSurface
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val chipFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH)

/**
 * Planificateur d'étapes : une date, une heure de rendez-vous optionnelle,
 * un lieu tapé en toutes lettres — hôtel à Santorin, adresse en Crète ou en
 * France — choisi dans l'autocomplétion mondiale (OpenStreetMap). Import des
 * rendez-vous du jour depuis l'agenda du téléphone (Google Agenda inclus),
 * export en carnet PDF premium enrichi (Wikipédia).
 */
@Composable
fun PlannerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { TripStore(context) }
    var trip by remember { mutableStateOf(TripRepository.currentTrip(context)) }
    var exporting by remember { mutableStateOf(false) }

    fun update(newTrip: Trip) {
        trip = newTrip
        store.save(newTrip)
    }

    BackHandler(onBack = onBack)

    Box(modifier = Modifier.fillMaxSize().background(BrandNight)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Retour", tint = Color.White)
                }
                Text("Planificateur", style = MaterialTheme.typography.headlineMedium, color = BrandGold)
            }
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = trip.name,
                onValueChange = { update(trip.copy(name = it)) },
                label = { Text("Nom du voyage") },
                singleLine = true,
                colors = plannerFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            AddStopSection(
                onAdd = { date, stop ->
                    update(trip.withStop(date, stop))
                    Toast.makeText(context, "Étape ajoutée : ${stop.name}", Toast.LENGTH_SHORT).show()
                },
                onImported = { count, skipped ->
                    val skippedNote = if (skipped > 0) " ($skipped sans lieu, ignorés)" else ""
                    Toast.makeText(context, "$count rendez-vous importés$skippedNote", Toast.LENGTH_LONG).show()
                    trip = TripRepository.currentTrip(context)
                },
                trip = { trip },
                persist = ::update,
            )

            Spacer(Modifier.height(20.dp))

            if (trip.days.isEmpty()) {
                Text(
                    "Aucune étape pour l'instant. Choisissez une date, tapez un lieu — le voyage se construit ici.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandMist,
                )
            }

            trip.days.sortedBy { it.date }.forEach { day ->
                DayCard(
                    day = day,
                    onRemove = { stop -> update(trip.withoutStop(day.date, stop)) },
                    onMove = { index, delta -> update(trip.withMovedStop(day.date, index, delta)) },
                    onSetTime = { index, newTime ->
                        update(trip.withUpdatedStop(day.date, index) { it.copy(time = newTime) })
                    },
                )
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(8.dp))

            if (trip.days.isNotEmpty()) {
                ExportPdfButton(
                    exporting = exporting,
                    onClick = {
                        exporting = true
                        scope.launch {
                            try {
                                val descriptions = TripPdfExporter.collectDescriptions(trip)
                                val file = TripPdfExporter.export(context, trip, descriptions)
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                context.startActivity(
                                    Intent.createChooser(
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "application/pdf"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        },
                                        "Carnet de voyage PDF",
                                    ),
                                )
                            } catch (e: Exception) {
                                Toast.makeText(context, "Export impossible : ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                exporting = false
                            }
                        }
                    },
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Le carnet est enrichi automatiquement : descriptif des villes et îles, dates en toutes lettres, heures de rendez-vous.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandMist.copy(alpha = 0.8f),
                )
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "Recherche de lieux : données OpenStreetMap (Photon) — gratuit et sans clé.",
                style = MaterialTheme.typography.labelSmall,
                color = BrandMist.copy(alpha = 0.5f),
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Ajout d'une étape : date + heure optionnelle + autocomplétion + import agenda
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddStopSection(
    onAdd: (LocalDate, TripStop) -> Unit,
    onImported: (added: Int, skipped: Int) -> Unit,
    trip: () -> Trip,
    persist: (Trip) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var date by remember { mutableStateOf(LocalDate.now()) }
    var time by remember { mutableStateOf<LocalTime?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<PlaceSuggestion>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchFailed by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }

    fun importCalendarDay() {
        importing = true
        scope.launch {
            try {
                val events = CalendarImporter.eventsOn(context, date)
                var added = 0
                var skipped = 0
                var current = trip()
                events.forEach { event ->
                    val place = event.location?.let {
                        try {
                            PlaceSearch.search(it, limit = 1).firstOrNull()
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (place == null) {
                        skipped++
                    } else {
                        current = current.withStop(
                            date,
                            TripStop(
                                name = event.title,
                                latitude = place.latitude,
                                longitude = place.longitude,
                                locality = place.locality,
                                time = event.time,
                            ),
                        )
                        added++
                    }
                }
                persist(current)
                onImported(added, skipped)
            } finally {
                importing = false
            }
        }
    }

    val calendarPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) importCalendarDay()
        else Toast.makeText(context, "Permission agenda refusée", Toast.LENGTH_SHORT).show()
    }

    // Recherche au fil de la saisie, avec anti-rebond (le serveur reste léger)
    LaunchedEffect(query) {
        searchFailed = false
        if (query.trim().length < 3) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(350)
        searching = true
        try {
            suggestions = PlaceSearch.search(query.trim())
        } catch (e: Exception) {
            suggestions = emptyList()
            searchFailed = true
        }
        searching = false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(BrandSurface.copy(alpha = 0.9f))
            .border(1.dp, BrandGold.copy(alpha = 0.22f), RoundedCornerShape(20.dp))
            .padding(16.dp)
            .animateContentSize(),
    ) {
        Text("Ajouter une étape", style = MaterialTheme.typography.titleLarge, color = Color.White)
        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Date
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(BrandIce.copy(alpha = 0.12f))
                    .clickable { showDatePicker = true }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.DateRange, contentDescription = null, tint = BrandIce, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(date.format(chipFormatter), style = MaterialTheme.typography.labelLarge, color = BrandIce)
            }
            Spacer(Modifier.size(8.dp))
            // Heure optionnelle
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(BrandGold.copy(alpha = 0.12f))
                    .clickable { showTimePicker = true }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Notifications, contentDescription = null, tint = BrandGold, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(
                    time?.format(DateTimeFormatter.ofPattern("HH'h'mm")) ?: "Heure (option)",
                    style = MaterialTheme.typography.labelLarge,
                    color = BrandGold,
                )
            }
            if (time != null) {
                IconButton(onClick = { time = null }) {
                    Icon(Icons.Rounded.Clear, contentDescription = "Effacer l'heure", tint = BrandMist)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Hôtel, adresse, île, lieu… (monde entier)") },
            singleLine = true,
            colors = plannerFieldColors(),
            trailingIcon = {
                when {
                    searching -> CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BrandIce, strokeWidth = 2.dp)
                    query.isNotEmpty() -> IconButton(onClick = { query = "" }) {
                        Icon(Icons.Rounded.Clear, contentDescription = "Effacer", tint = BrandMist)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        if (searchFailed) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Recherche indisponible — vérifiez la connexion et réessayez.",
                style = MaterialTheme.typography.bodyMedium,
                color = BrandEmber,
            )
        }

        suggestions.forEach { suggestion ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        onAdd(
                            date,
                            TripStop(
                                name = suggestion.name,
                                latitude = suggestion.latitude,
                                longitude = suggestion.longitude,
                                locality = suggestion.locality,
                                time = time,
                            ),
                        )
                        query = ""
                        suggestions = emptyList()
                        time = null
                    }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Place, contentDescription = null, tint = BrandGold, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(suggestion.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
                    val detailLine = listOfNotNull(suggestion.category, suggestion.detail.takeIf { it.isNotBlank() })
                        .joinToString(" — ")
                    if (detailLine.isNotBlank()) {
                        Text(detailLine, style = MaterialTheme.typography.bodyMedium, color = BrandMist)
                    }
                }
                Icon(Icons.Rounded.Add, contentDescription = "Ajouter", tint = BrandAuroraTeal)
            }
        }

        Spacer(Modifier.height(10.dp))
        // Import agenda du jour sélectionné
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(enabled = !importing) {
                    calendarPermission.launch(Manifest.permission.READ_CALENDAR)
                }
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (importing) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BrandAuroraTeal, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Rounded.DateRange, contentDescription = null, tint = BrandAuroraTeal, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.size(8.dp))
            Text(
                if (importing) "Import des rendez-vous…"
                else "Importer les rendez-vous du ${date.format(chipFormatter)} depuis mon agenda",
                style = MaterialTheme.typography.labelLarge,
                color = BrandAuroraTeal,
            )
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Annuler") } },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = time?.hour ?: 9,
            initialMinute = time?.minute ?: 0,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    time = LocalTime.of(timeState.hour, timeState.minute)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Annuler") } },
            text = { TimePicker(state = timeState) },
        )
    }
}

// ---------------------------------------------------------------------------
// Journées, étapes, export
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayCard(
    day: TripDay,
    onRemove: (TripStop) -> Unit,
    onMove: (Int, Int) -> Unit,
    onSetTime: (Int, LocalTime?) -> Unit,
) {
    var editingTimeFor by remember { mutableStateOf<Int?>(null) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(BrandSurface.copy(alpha = 0.88f))
            .border(1.dp, BrandGold.copy(alpha = 0.22f), RoundedCornerShape(20.dp))
            .padding(16.dp)
            .animateContentSize(),
    ) {
        Text(day.date.frenchLabel(), style = MaterialTheme.typography.titleMedium, color = BrandGold)
        Spacer(Modifier.height(6.dp))
        day.stops.forEachIndexed { index, stop ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .border(1.5.dp, BrandGold.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${index + 1}", style = MaterialTheme.typography.labelLarge, color = BrandGold)
                }
                Spacer(Modifier.size(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(stop.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
                    val sub = listOfNotNull(stop.timeLabel(), stop.locality).joinToString(" · ")
                    if (sub.isNotBlank()) {
                        Text(sub, style = MaterialTheme.typography.bodyMedium, color = BrandMist)
                    }
                }
                IconButton(onClick = { editingTimeFor = index }) {
                    Icon(
                        Icons.Rounded.Notifications,
                        contentDescription = "Modifier l'heure",
                        tint = if (stop.time != null) BrandGold else BrandMist.copy(alpha = 0.5f),
                    )
                }
                IconButton(onClick = { onMove(index, -1) }, enabled = index > 0) {
                    Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Monter", tint = if (index > 0) BrandIce else BrandMist.copy(alpha = 0.3f))
                }
                IconButton(onClick = { onMove(index, +1) }, enabled = index < day.stops.lastIndex) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Descendre", tint = if (index < day.stops.lastIndex) BrandIce else BrandMist.copy(alpha = 0.3f))
                }
                IconButton(onClick = { onRemove(stop) }) {
                    Icon(Icons.Rounded.Clear, contentDescription = "Supprimer", tint = BrandEmber)
                }
            }
        }
    }

    editingTimeFor?.let { index ->
        val current = day.stops.getOrNull(index)?.time
        val timeState = rememberTimePickerState(
            initialHour = current?.hour ?: 9,
            initialMinute = current?.minute ?: 0,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { editingTimeFor = null },
            confirmButton = {
                TextButton(onClick = {
                    onSetTime(index, LocalTime.of(timeState.hour, timeState.minute))
                    editingTimeFor = null
                }) { Text("OK") }
            },
            dismissButton = {
                Row {
                    if (current != null) {
                        TextButton(onClick = {
                            onSetTime(index, null)
                            editingTimeFor = null
                        }) { Text("Sans heure") }
                    }
                    TextButton(onClick = { editingTimeFor = null }) { Text("Annuler") }
                }
            },
            text = { TimePicker(state = timeState) },
        )
    }
}

@Composable
private fun ExportPdfButton(exporting: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BrandGold.copy(alpha = 0.14f))
            .border(1.dp, BrandGold.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable(enabled = !exporting, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (exporting) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BrandGold, strokeWidth = 2.dp)
            Spacer(Modifier.size(10.dp))
            Text("Préparation du carnet…", style = MaterialTheme.typography.labelLarge, color = BrandGold)
        } else {
            Icon(Icons.Rounded.Share, contentDescription = null, tint = BrandGold, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(10.dp))
            Text("Exporter le carnet de voyage en PDF", style = MaterialTheme.typography.labelLarge, color = BrandGold)
        }
    }
}

@Composable
private fun plannerFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = BrandIce,
    unfocusedBorderColor = BrandMist.copy(alpha = 0.4f),
    focusedLabelColor = BrandIce,
    unfocusedLabelColor = BrandMist,
    cursorColor = BrandIce,
)
