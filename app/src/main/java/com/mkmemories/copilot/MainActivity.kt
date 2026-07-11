package com.mkmemories.copilot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mkmemories.copilot.feature.briefing.BriefingPlayer
import com.mkmemories.copilot.feature.briefing.WeatherBriefingGenerator
import kotlinx.coroutines.launch

/**
 * Écran téléphone (v1 squelette) : présente les piliers et permet de tester
 * le briefing météo du jour (généré via Open-Meteo, lu par la synthèse vocale).
 */
class MainActivity : ComponentActivity() {

    private lateinit var briefingPlayer: BriefingPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        briefingPlayer = BriefingPlayer(this)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HomeScreen(onPlayBriefing = { text -> briefingPlayer.speak(text) })
                }
            }
        }
    }

    override fun onDestroy() {
        briefingPlayer.release()
        super.onDestroy()
    }
}

@Composable
private fun HomeScreen(onPlayBriefing: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var briefing by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("MK Copilot", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Le copilote qui veille sur vous : road trip planifié, briefing du jour, " +
                "zones de danger, pack Ange gardien — 100 % gratuit.",
            style = MaterialTheme.typography.bodyLarge,
        )

        Button(onClick = {
            scope.launch {
                briefing = try {
                    // TODO v1.1 : utiliser la vraie position (FusedLocationProvider).
                    WeatherBriefingGenerator.generate(latitude = 48.8566, longitude = 2.3522)
                } catch (e: Exception) {
                    "Impossible de récupérer la météo : ${e.message}"
                }
                briefing?.let(onPlayBriefing)
            }
        }) {
            Text("🌤️ Écouter le briefing du jour")
        }

        briefing?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
