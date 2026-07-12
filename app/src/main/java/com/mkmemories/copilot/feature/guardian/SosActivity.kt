package com.mkmemories.copilot.feature.guardian

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.mkmemories.copilot.feature.location.LocationProvider
import com.mkmemories.copilot.feature.settings.SettingsStore
import com.mkmemories.copilot.ui.theme.BrandAuroraTeal
import com.mkmemories.copilot.ui.theme.BrandEmber
import com.mkmemories.copilot.ui.theme.BrandGold
import com.mkmemories.copilot.ui.theme.BrandNight
import com.mkmemories.copilot.ui.theme.MKCopilotTheme

/**
 * « Accident détecté — tout va bien ? » : plein écran, même téléphone
 * verrouillé. Un seul geste géant pour annuler ; sans réponse, SMS SOS aux
 * contacts d'urgence avec la position, puis 112 pré-composé.
 */
class SosActivity : ComponentActivity() {

    private lateinit var sos: SosManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        val settings = SettingsStore(this)
        var onTick: (Int) -> Unit = {}
        var onSent: () -> Unit = {}

        sos = SosManager(
            scope = lifecycleScope,
            emergencyContacts = { settings.emergencyContacts },
            lastKnownLocation = { DriveGuardService.lastLocation ?: LocationProvider.lastKnown(this) },
            onCountdownTick = { onTick(it) },
            onSosSent = { onSent() },
        )

        setContent {
            MKCopilotTheme {
                var secondsLeft by remember { mutableIntStateOf(SosManager.COUNTDOWN_SECONDS) }
                var sent by remember { mutableStateOf(false) }
                onTick = { left -> secondsLeft = left }
                onSent = { sent = true }

                SosScreen(
                    secondsLeft = secondsLeft,
                    sent = sent,
                    hasContacts = settings.emergencyContacts.isNotEmpty(),
                    onAllGood = {
                        sos.cancel()
                        finish()
                    },
                    onCall112 = {
                        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")))
                    },
                )
            }
        }
        sos.startCountdown()
    }

    override fun onDestroy() {
        sos.cancel()
        super.onDestroy()
    }
}

@Composable
private fun SosScreen(
    secondsLeft: Int,
    sent: Boolean,
    hasContacts: Boolean,
    onAllGood: () -> Unit,
    onCall112: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(BrandNight)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                if (sent) "SOS envoyé" else "Accident détecté",
                style = MaterialTheme.typography.displaySmall,
                color = BrandEmber,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                when {
                    sent -> "Vos contacts d'urgence ont reçu votre position."
                    hasContacts -> "Tout va bien ? Sans réponse, vos contacts d'urgence recevront votre position dans :"
                    else -> "Tout va bien ? Aucun contact d'urgence configuré — seul l'appel au 112 est proposé."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            if (!sent) {
                Text(
                    "$secondsLeft",
                    style = MaterialTheme.typography.displaySmall,
                    color = BrandGold,
                )
                Spacer(Modifier.height(32.dp))
            }

            // Geste géant, faisable sans regarder : tout l'écran bas = « tout va bien »
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(BrandAuroraTeal)
                    .clickable(onClick = onAllGood),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (sent) "Fermer" else "TOUT VA BIEN",
                    style = MaterialTheme.typography.headlineMedium,
                    color = BrandNight,
                )
            }
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(BrandEmber.copy(alpha = 0.25f))
                    .clickable(onClick = onCall112),
                contentAlignment = Alignment.Center,
            ) {
                Text("Appeler le 112", style = MaterialTheme.typography.titleLarge, color = BrandEmber)
            }
        }
    }
}
