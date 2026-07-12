package com.mkmemories.copilot.ui.settings

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.mkmemories.copilot.feature.settings.Feature
import com.mkmemories.copilot.feature.settings.SettingsStore
import com.mkmemories.copilot.ui.theme.BrandAuroraTeal
import com.mkmemories.copilot.ui.theme.BrandEmber
import com.mkmemories.copilot.ui.theme.BrandGold
import com.mkmemories.copilot.ui.theme.BrandIce
import com.mkmemories.copilot.ui.theme.BrandMist
import com.mkmemories.copilot.ui.theme.BrandNight
import com.mkmemories.copilot.ui.theme.BrandSurface

// Fonctions dont l'activation déclenche la demande de permission d'envoi SMS
private val smsFeatures = setOf(Feature.AUTO_REPLY, Feature.TRIP_TRACKING)

/**
 * Réglages : le poste de commandement de l'Ange gardien (contacts SOS,
 * proches, interrupteurs) et de l'IA (clé Mistral en repli du moteur local).
 */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { SettingsStore(context) }

    var emergency by remember { mutableStateOf(store.emergencyContacts) }
    var recipients by remember { mutableStateOf(store.arrivalRecipients) }
    var mistralKey by remember { mutableStateOf(store.mistralKey ?: "") }
    var guardian by remember { mutableStateOf(store.guardianEnabled) }
    var danger by remember { mutableStateOf(store.dangerZonesEnabled) }
    var arrivalSms by remember { mutableStateOf(store.arrivalSmsEnabled) }

    val smsPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) {
            Toast.makeText(
                context,
                "Sans la permission SMS, le SOS et « J'arrive bien » ne pourront pas envoyer de messages",
                Toast.LENGTH_LONG,
            ).show()
        }
    }
    val receiveSmsPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "Permission SMS refusée : lecture/réponse automatique inactives", Toast.LENGTH_LONG).show()
        }
    }
    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "Permission micro refusée : commandes vocales inactives", Toast.LENGTH_LONG).show()
        }
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
                Text("Réglages", style = MaterialTheme.typography.headlineMedium, color = BrandGold)
            }
            Spacer(Modifier.height(12.dp))

            // --- Ange gardien -------------------------------------------------
            SettingsCard(title = "Ange gardien") {
                SwitchRow(
                    title = "Détection d'accident + SOS",
                    subtitle = "Choc violent → « Tout va bien ? » → SMS aux contacts avec position",
                    checked = guardian,
                    onChange = {
                        guardian = it
                        store.guardianEnabled = it
                        if (it) smsPermission.launch(Manifest.permission.SEND_SMS)
                    },
                )
                Spacer(Modifier.height(10.dp))
                Text("Contacts d'urgence", style = MaterialTheme.typography.titleMedium, color = Color.White)
                if (emergency.isEmpty()) {
                    Text(
                        "Aucun contact — le SOS ne peut prévenir personne.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BrandEmber,
                    )
                }
                PhoneList(numbers = emergency, onRemove = {
                    store.removeEmergencyContact(it)
                    emergency = store.emergencyContacts
                })
                PhoneAdder(label = "Ajouter un contact d'urgence") {
                    store.addEmergencyContact(it)
                    emergency = store.emergencyContacts
                    smsPermission.launch(Manifest.permission.SEND_SMS)
                }
            }

            Spacer(Modifier.height(14.dp))

            // --- J'arrive bien ------------------------------------------------
            SettingsCard(title = "« J'arrive bien »") {
                SwitchRow(
                    title = "SMS automatique à l'arrivée",
                    subtitle = "À chaque étape atteinte, vos proches sont rassurés sans toucher au téléphone",
                    checked = arrivalSms,
                    onChange = {
                        arrivalSms = it
                        store.arrivalSmsEnabled = it
                        if (it) smsPermission.launch(Manifest.permission.SEND_SMS)
                    },
                )
                Spacer(Modifier.height(10.dp))
                Text("Proches à prévenir", style = MaterialTheme.typography.titleMedium, color = Color.White)
                PhoneList(numbers = recipients, onRemove = {
                    store.removeArrivalRecipient(it)
                    recipients = store.arrivalRecipients
                })
                PhoneAdder(label = "Ajouter un proche") {
                    store.addArrivalRecipient(it)
                    recipients = store.arrivalRecipients
                }
            }

            Spacer(Modifier.height(14.dp))

            // --- Zones de danger ----------------------------------------------
            SettingsCard(title = "Zones de danger") {
                SwitchRow(
                    title = "Alertes vocales en conduite",
                    subtitle = "Segments légaux (300 m / 2 km / 4 km) issus de l'open data officiel, en cache hors ligne",
                    checked = danger,
                    onChange = {
                        danger = it
                        store.dangerZonesEnabled = it
                    },
                )
            }

            Spacer(Modifier.height(14.dp))

            // --- Toutes les fonctionnalités -----------------------------------
            SettingsCard(title = "Fonctionnalités") {
                Text(
                    "Chaque fonction s'active ou se coupe ici. Celles qui envoient des SMS, " +
                        "lisent vos messages ou parlent spontanément sont désactivées par défaut.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandMist,
                )
                Spacer(Modifier.height(6.dp))
                Feature.entries.forEach { feature ->
                    var enabled by remember { mutableStateOf(store.isEnabled(feature)) }
                    Spacer(Modifier.height(8.dp))
                    SwitchRow(
                        title = feature.title,
                        subtitle = feature.description,
                        checked = enabled,
                        onChange = {
                            enabled = it
                            store.setEnabled(feature, it)
                            when {
                                it && feature in smsFeatures ->
                                    smsPermission.launch(Manifest.permission.SEND_SMS)
                                it && feature == Feature.MESSAGE_READER ->
                                    receiveSmsPermission.launch(Manifest.permission.RECEIVE_SMS)
                                it && feature == Feature.AUTO_REPLY ->
                                    receiveSmsPermission.launch(Manifest.permission.RECEIVE_SMS)
                                it && feature == Feature.VOICE_COMMANDS ->
                                    micPermission.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // --- IA -----------------------------------------------------------
            SettingsCard(title = "IA copilote") {
                Text(
                    "Priorité au moteur local (il tourne dans le téléphone, hors ligne). " +
                        "Une clé Mistral gratuite (tier Experiment) sert de repli pour des briefings plus riches.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandMist,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = mistralKey,
                    onValueChange = {
                        mistralKey = it
                        store.mistralKey = it
                    },
                    label = { Text("Clé API Mistral (optionnelle)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = settingsFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(14.dp))

            // --- Données locales (journal + boîte noire) -----------------------
            SettingsCard(title = "Données locales") {
                val journal = remember { com.mkmemories.copilot.feature.journal.TripJournal(context) }
                val trips = remember { journal.all() }
                Text(
                    if (trips.isEmpty()) "Journal de bord : aucun trajet enregistré pour l'instant."
                    else "Journal de bord : ${trips.size} trajet${if (trips.size > 1) "s" else ""}, " +
                        "${"%.0f".format(journal.totalKm())} km au total.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                )
                val incidents = remember { com.mkmemories.copilot.feature.blackbox.BlackBox.incidents(context) }
                if (incidents.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Partager le dernier incident de la boîte noire (${incidents.size} enregistré${if (incidents.size > 1) "s" else ""})",
                        style = MaterialTheme.typography.labelLarge,
                        color = BrandGold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                try {
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        context, "${context.packageName}.fileprovider", incidents.first(),
                                    )
                                    context.startActivity(
                                        android.content.Intent.createChooser(
                                            android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                type = "application/json"
                                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            },
                                            "Incident boîte noire",
                                        ),
                                    )
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Partage impossible", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(vertical = 6.dp),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tout reste sur votre téléphone : rien n'est envoyé à un serveur.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandMist.copy(alpha = 0.7f),
                )
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Briques
// ---------------------------------------------------------------------------

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(BrandSurface.copy(alpha = 0.9f))
            .border(1.dp, BrandGold.copy(alpha = 0.22f), RoundedCornerShape(20.dp))
            .padding(16.dp)
            .animateContentSize(),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = BrandGold)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = BrandMist)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = BrandAuroraTeal,
                checkedThumbColor = BrandNight,
            ),
        )
    }
}

@Composable
private fun PhoneList(numbers: List<String>, onRemove: (String) -> Unit) {
    numbers.forEach { number ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Phone, contentDescription = null, tint = BrandIce, modifier = Modifier.size(16.dp))
            Spacer(Modifier.size(10.dp))
            Text(number, style = MaterialTheme.typography.bodyLarge, color = Color.White, modifier = Modifier.weight(1f))
            IconButton(onClick = { onRemove(number) }) {
                Icon(Icons.Rounded.Clear, contentDescription = "Supprimer $number", tint = BrandEmber)
            }
        }
    }
}

@Composable
private fun PhoneAdder(label: String, onAdd: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            label = { Text(label) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            colors = settingsFieldColors(),
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = {
                if (value.isNotBlank()) {
                    onAdd(value)
                    value = ""
                }
            },
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Ajouter", tint = BrandAuroraTeal)
        }
    }
}

@Composable
private fun settingsFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = BrandIce,
    unfocusedBorderColor = BrandMist.copy(alpha = 0.4f),
    focusedLabelColor = BrandIce,
    unfocusedLabelColor = BrandMist,
    cursorColor = BrandIce,
)
