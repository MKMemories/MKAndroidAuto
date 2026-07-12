package com.mkmemories.copilot.ui.parking

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Place
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
import com.mkmemories.copilot.feature.dangerzones.ZoneAlertEngine
import com.mkmemories.copilot.feature.guardian.ParkingFormat
import com.mkmemories.copilot.feature.guardian.ParkingMemory
import com.mkmemories.copilot.feature.guardian.ParkingReminderReceiver
import com.mkmemories.copilot.feature.location.LocationProvider
import com.mkmemories.copilot.ui.theme.BrandAuroraTeal
import com.mkmemories.copilot.ui.theme.BrandEmber
import com.mkmemories.copilot.ui.theme.BrandGold
import com.mkmemories.copilot.ui.theme.BrandIce
import com.mkmemories.copilot.ui.theme.BrandMist
import com.mkmemories.copilot.ui.theme.BrandNight
import com.mkmemories.copilot.ui.theme.BrandSurface
import kotlinx.coroutines.launch

/**
 * « Ma voiture » : retrouver la place de stationnement — guidage à pied,
 * distance, note, minuteur zone bleue. Enregistrement manuel possible même
 * sans l'Ange gardien (utile si le service n'était pas actif).
 */
@Composable
fun ParkingScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val memory = remember { ParkingMemory(context) }

    var spot by remember { mutableStateOf(memory.lastParkingSpot()) }
    var note by remember { mutableStateOf(memory.note ?: "") }
    var saving by remember { mutableStateOf(false) }
    var showTimer by remember { mutableStateOf(false) }

    // Distance à vol d'oiseau depuis la position actuelle, si connue
    val distanceLabel = remember(spot) {
        val here = LocationProvider.lastKnown(context)
        if (here != null && spot != null) {
            ParkingFormat.distanceLabel(
                ZoneAlertEngine.distanceMeters(here.latitude, here.longitude, spot!!.latitude, spot!!.longitude),
            )
        } else {
            null
        }
    }

    fun saveHere() {
        saving = true
        scope.launch {
            val here = LocationProvider.current(context)
            if (here != null) {
                memory.saveParkingSpot(here.latitude, here.longitude, System.currentTimeMillis())
                spot = memory.lastParkingSpot()
                Toast.makeText(
                    context,
                    "Place enregistrée ✓  ${ParkingFormat.coordinates(here.latitude, here.longitude)}",
                    Toast.LENGTH_LONG,
                ).show()
            } else {
                Toast.makeText(context, "Position introuvable — activez la localisation", Toast.LENGTH_LONG).show()
            }
            saving = false
        }
    }

    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) saveHere() else Toast.makeText(context, "Permission de localisation refusée", Toast.LENGTH_LONG).show() }

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
                Text("Ma voiture", style = MaterialTheme.typography.headlineMedium, color = BrandGold)
            }
            Spacer(Modifier.height(12.dp))

            val currentSpot = spot
            if (currentSpot == null) {
                ParkingCard {
                    Text(
                        "Aucune place enregistrée pour l'instant.",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                    )
                    Text(
                        "Enregistrez votre place en un tap avant de vous éloigner — elle sera mémorisée ici.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BrandMist,
                    )
                }
            } else {
                ParkingCard {
                    Text(
                        "Garée ${ParkingFormat.parkedAgo(System.currentTimeMillis(), currentSpot.timestampMillis)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                    distanceLabel?.let {
                        Text("À environ $it à vol d'oiseau", style = MaterialTheme.typography.bodyLarge, color = BrandIce)
                    }

                    val mapsUrl = ParkingFormat.googleMapsUrl(currentSpot.latitude, currentSpot.longitude)
                    Spacer(Modifier.height(8.dp))
                    // Coordonnées enregistrées + lien Google Maps : info concrète et exploitable.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "📍 ${ParkingFormat.coordinates(currentSpot.latitude, currentSpot.longitude)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BrandMist,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val clip = context.getSystemService(android.content.ClipboardManager::class.java)
                                    clip?.setPrimaryClip(android.content.ClipData.newPlainText("Ma voiture", mapsUrl))
                                    Toast.makeText(context, "Lien Google Maps copié", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 4.dp),
                        )
                        Spacer(Modifier.size(10.dp))
                        Text(
                            "Copier",
                            style = MaterialTheme.typography.labelMedium,
                            color = BrandIce,
                        )
                    }
                    Spacer(Modifier.height(14.dp))

                    PrimaryButton(
                        icon = { Icon(Icons.Rounded.Place, contentDescription = null, tint = BrandNight) },
                        text = "Guidage à pied",
                    ) {
                        try {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("google.navigation:q=${currentSpot.latitude},${currentSpot.longitude}&mode=w"),
                                ),
                            )
                        } catch (e: ActivityNotFoundException) {
                            // Repli : ouvrir dans n'importe quelle app de cartes
                            try {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("geo:${currentSpot.latitude},${currentSpot.longitude}?q=${currentSpot.latitude},${currentSpot.longitude}(Ma voiture)")),
                                )
                            } catch (e2: ActivityNotFoundException) {
                                Toast.makeText(context, "Aucune app de cartes disponible", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SecondaryButton(
                            text = "Ouvrir dans Maps",
                            modifier = Modifier.weight(1f),
                        ) {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl)))
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(context, "Aucune app de cartes disponible", Toast.LENGTH_SHORT).show()
                            }
                        }
                        SecondaryButton(
                            text = "Partager",
                            modifier = Modifier.weight(1f),
                        ) {
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Où est ma voiture")
                                putExtra(Intent.EXTRA_TEXT, "Ma voiture est garée ici : $mapsUrl")
                            }
                            context.startActivity(Intent.createChooser(share, "Partager la position"))
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = note,
                        onValueChange = {
                            note = it
                            memory.note = it
                        },
                        label = { Text("Note (étage, place, repère…)") },
                        colors = parkingFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "⏱ Minuteur zone bleue",
                            style = MaterialTheme.typography.labelLarge,
                            color = BrandIce,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(BrandIce.copy(alpha = 0.12f))
                                .clickable { showTimer = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        )
                        Text(
                            "Oublier",
                            style = MaterialTheme.typography.labelLarge,
                            color = BrandEmber,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    memory.clear()
                                    spot = null
                                    note = ""
                                    Toast.makeText(context, "Position oubliée", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                icon = {
                    if (saving) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BrandNight, strokeWidth = 2.5.dp)
                    else Icon(Icons.Rounded.Place, contentDescription = null, tint = BrandNight)
                },
                text = if (saving) "Enregistrement…" else "Enregistrer ma place ici",
            ) {
                if (saving) return@PrimaryButton
                if (LocationProvider.hasPermission(context)) saveHere()
                else locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }

            Spacer(Modifier.height(28.dp))
        }
    }

    if (showTimer) {
        val minutes = listOf(30, 60, 90, 120)
        AlertDialog(
            onDismissRequest = { showTimer = false },
            confirmButton = {},
            title = { Text("Rappel de stationnement") },
            text = {
                Column {
                    Text("Une seule notification, quelques minutes avant l'échéance :")
                    Spacer(Modifier.height(4.dp))
                    minutes.forEach { m ->
                        Text(
                            if (m < 60) "$m minutes" else "${m / 60} h${if (m % 60 > 0) " ${m % 60}" else ""}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    ParkingReminderReceiver.schedule(context, (m - 10).coerceAtLeast(5))
                                    showTimer = false
                                    Toast.makeText(context, "Rappel programmé", Toast.LENGTH_SHORT).show()
                                }
                                .padding(10.dp),
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun ParkingCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(BrandSurface.copy(alpha = 0.9f))
            .border(1.dp, BrandGold.copy(alpha = 0.22f), RoundedCornerShape(20.dp))
            .padding(16.dp)
            .animateContentSize(),
        content = { content() },
    )
}

@Composable
private fun PrimaryButton(icon: @Composable () -> Unit, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(BrandAuroraTeal)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(Modifier.size(10.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = BrandNight)
    }
}

@Composable
private fun SecondaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(23.dp))
            .border(1.5.dp, BrandAuroraTeal.copy(alpha = 0.7f), RoundedCornerShape(23.dp))
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = BrandAuroraTeal)
    }
}

@Composable
private fun parkingFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = BrandIce,
    unfocusedBorderColor = BrandMist.copy(alpha = 0.4f),
    focusedLabelColor = BrandIce,
    unfocusedLabelColor = BrandMist,
    cursorColor = BrandIce,
)
