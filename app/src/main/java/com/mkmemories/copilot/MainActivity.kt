package com.mkmemories.copilot

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.mkmemories.copilot.feature.ai.BriefingEnricher
import com.mkmemories.copilot.feature.briefing.BriefingPlayer
import com.mkmemories.copilot.feature.briefing.WeatherBriefingGenerator
import com.mkmemories.copilot.feature.guardian.DriveGuardService
import com.mkmemories.copilot.feature.location.LocationProvider
import com.mkmemories.copilot.feature.roadtrip.DayBriefing
import com.mkmemories.copilot.ui.settings.SettingsScreen
import com.mkmemories.copilot.feature.roadtrip.NavigationLauncher
import com.mkmemories.copilot.feature.roadtrip.Trip
import com.mkmemories.copilot.feature.roadtrip.TripRepository
import com.mkmemories.copilot.feature.roadtrip.TripStop
import com.mkmemories.copilot.feature.roadtrip.timeLabel
import com.mkmemories.copilot.feature.update.UpdateChecker
import com.mkmemories.copilot.feature.update.UpdateInfo
import com.mkmemories.copilot.ui.planner.PlannerScreen
import com.mkmemories.copilot.ui.theme.BrandAuroraTeal
import com.mkmemories.copilot.ui.theme.BrandAuroraViolet
import com.mkmemories.copilot.ui.theme.BrandEmber
import com.mkmemories.copilot.ui.theme.BrandGold
import com.mkmemories.copilot.ui.theme.BrandGoldLight
import com.mkmemories.copilot.ui.theme.BrandIce
import com.mkmemories.copilot.ui.theme.BrandMist
import com.mkmemories.copilot.ui.theme.BrandNight
import com.mkmemories.copilot.ui.theme.BrandSurface
import com.mkmemories.copilot.ui.theme.MKCopilotTheme
import java.time.LocalDate
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var briefingPlayer: BriefingPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        briefingPlayer = BriefingPlayer(this)
        setContent {
            MKCopilotTheme {
                var screen by rememberSaveable { mutableStateOf(AppScreen.HOME) }
                when (screen) {
                    AppScreen.HOME -> {
                        val trip = remember(screen) { TripRepository.currentTrip(this) }
                        HomeScreen(
                            trip = trip,
                            onPlayBriefing = { text -> briefingPlayer.speak(text) },
                            onOpenPlanner = { screen = AppScreen.PLANNER },
                            onOpenSettings = { screen = AppScreen.SETTINGS },
                        )
                    }
                    AppScreen.PLANNER -> PlannerScreen(onBack = { screen = AppScreen.HOME })
                    AppScreen.SETTINGS -> SettingsScreen(onBack = { screen = AppScreen.HOME })
                }
            }
        }
    }

    override fun onDestroy() {
        briefingPlayer.release()
        super.onDestroy()
    }
}

private enum class AppScreen { HOME, PLANNER, SETTINGS }

// ---------------------------------------------------------------------------
// Écran d'accueil
// ---------------------------------------------------------------------------

private data class Feature(val image: Int, val title: String, val subtitle: String)

private val Features = listOf(
    Feature(R.drawable.feat_roadtrip, "Road trip", "Vos étapes du jour, guidées par Maps"),
    Feature(R.drawable.feat_briefing, "Briefing du jour", "Météo et conseils, lus en voiture"),
    Feature(R.drawable.feat_guardian, "Ange gardien", "Détection d'accident, SOS 30 s"),
    Feature(R.drawable.feat_sos, "SOS", "SMS d'urgence avec position GPS"),
    Feature(R.drawable.feat_arrival, "J'arrive bien", "Vos proches prévenus à l'arrivée"),
    Feature(R.drawable.feat_parking, "Parking", "Votre voiture, toujours retrouvée"),
    Feature(R.drawable.feat_danger, "Zones de danger", "Alertes légales, audio, mains libres"),
    Feature(R.drawable.feat_ai, "IA copilote", "Briefings malins, 100 % gratuits"),
)

@Composable
private fun HomeScreen(
    trip: Trip,
    onPlayBriefing: (String) -> Unit,
    onOpenPlanner: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val scroll = rememberScrollState()
    var appeared by rememberSaveable { mutableStateOf(false) }
    var update by remember { mutableStateOf<UpdateInfo?>(null) }
    LaunchedEffect(Unit) {
        appeared = true
        update = UpdateChecker.check(currentBuild = BuildConfig.BUILD_NUMBER)
    }

    Box(modifier = Modifier.fillMaxSize().background(BrandNight)) {
        // Hero fjord/aurore en parallaxe : il défile deux fois moins vite que le contenu
        Image(
            painter = painterResource(R.drawable.hero_fjord),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = -scroll.value * 0.5f
                    val fade = (scroll.value / 900f).coerceIn(0f, 0.45f)
                    alpha = 1f - fade
                },
            contentScale = ContentScale.Crop,
        )
        // Voile : lisible en bas, spectaculaire en haut
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to BrandNight.copy(alpha = 0.35f),
                        0.35f to Color.Transparent,
                        0.72f to BrandNight.copy(alpha = 0.92f),
                        1f to BrandNight,
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(200.dp))

            Reveal(appeared, index = 0) { HeroTitle() }

            update?.let { info ->
                Spacer(Modifier.height(20.dp))
                UpdateCard(info)
            }

            Spacer(Modifier.height(28.dp))
            Reveal(appeared, index = 1) { BriefingCard(trip, onPlayBriefing) }

            Spacer(Modifier.height(16.dp))
            Reveal(appeared, index = 2) { RoadTripCard(trip, onOpenPlanner) }

            Spacer(Modifier.height(28.dp))
            Reveal(appeared, index = 3) {
                Text(
                    "LES PILIERS",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandGold.copy(alpha = 0.85f),
                )
            }
            Spacer(Modifier.height(12.dp))

            Features.chunked(2).forEachIndexed { rowIndex, pair ->
                Reveal(appeared, index = 4 + rowIndex) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        pair.forEach { feature ->
                            FeatureCard(feature, modifier = Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(20.dp))
            Reveal(appeared, index = 9) {
                Text(
                    "Forgé dans le Nord — 100 % gratuit, zéro serveur, vos données restent à bord.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandMist.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                )
            }
            Spacer(Modifier.height(32.dp))
        }

        // Réglages, toujours accessible en haut à droite
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 4.dp, end = 8.dp),
        ) {
            Icon(
                Icons.Rounded.Settings,
                contentDescription = "Réglages",
                tint = Color.White.copy(alpha = 0.92f),
            )
        }
    }
}

/** Entrée en cascade : fondu + glissement, décalés selon la position dans la page. */
@Composable
private fun Reveal(visible: Boolean, index: Int, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(durationMillis = 500, delayMillis = 90 * index)) +
            slideInVertically(
                animationSpec = tween(durationMillis = 500, delayMillis = 90 * index),
                initialOffsetY = { it / 3 },
            ),
    ) {
        content()
    }
}

@Composable
private fun HeroTitle() {
    Column {
        Text(
            "MK Copilot",
            style = MaterialTheme.typography.displaySmall.copy(
                brush = Brush.linearGradient(listOf(BrandGoldLight, BrandGold, BrandEmber)),
            ),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Le copilote qui veille sur vous.",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )
        Text(
            "Road trip, briefing, ange gardien — 100 % gratuit.",
            style = MaterialTheme.typography.bodyMedium,
            color = BrandMist,
        )
    }
}

// ---------------------------------------------------------------------------
// Mise à jour intégrée
// ---------------------------------------------------------------------------

@Composable
private fun UpdateCard(info: UpdateInfo) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BrandEmber.copy(alpha = 0.16f))
            .border(1.dp, BrandEmber.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
            .clickable {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl)))
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, "Aucun navigateur disponible", Toast.LENGTH_SHORT).show()
                }
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Refresh, contentDescription = null, tint = BrandEmber, modifier = Modifier.size(22.dp))
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Mise à jour disponible", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(
                "${info.title} — touchez pour télécharger et installer",
                style = MaterialTheme.typography.bodyMedium,
                color = BrandMist,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Briefing du jour
// ---------------------------------------------------------------------------

@Composable
private fun BriefingCard(trip: Trip, onPlayBriefing: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var loading by remember { mutableStateOf(false) }
    var briefing by remember { mutableStateOf<String?>(null) }
    val todayStops = remember(trip) { trip.stopsFor(LocalDate.now()) }

    fun doFetch() {
        loading = true
        scope.launch {
            val location = LocationProvider.current(context)
            val latitude = location?.latitude ?: LocationProvider.FALLBACK_LATITUDE
            val longitude = location?.longitude ?: LocationProvider.FALLBACK_LONGITUDE
            val weather = try {
                WeatherBriefingGenerator.generate(latitude, longitude)
            } catch (e: Exception) {
                "Météo indisponible pour l'instant."
            }
            briefing = BriefingEnricher.enrich(context, weather, todayStops)
            loading = false
            briefing?.let(onPlayBriefing)
        }
    }

    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { doFetch() } // même refusée, le briefing part avec le repli

    fun fetch() {
        if (loading) return
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        if (LocationProvider.hasPermission(context)) doFetch()
        else locationPermission.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }

    NordicCard {
        Column(modifier = Modifier.padding(20.dp).animateContentSize()) {
            Text("Briefing du jour", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(
                "Météo, pluie, vent et vos étapes du jour — lus à voix haute dans les haut-parleurs de la voiture.",
                style = MaterialTheme.typography.bodyMedium,
                color = BrandMist,
            )
            Spacer(Modifier.height(16.dp))

            GradientButton(
                text = if (loading) "Préparation…" else "Écouter le briefing",
                loading = loading,
                onClick = ::fetch,
            )

            AnimatedVisibility(
                visible = briefing != null,
                enter = fadeIn(tween(350)) + expandVertically(tween(350)),
            ) {
                Row(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        briefing.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.92f),
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { briefing?.let(onPlayBriefing) }) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = "Relire le briefing",
                            tint = BrandIce,
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Road trip du jour
// ---------------------------------------------------------------------------

@Composable
private fun RoadTripCard(trip: Trip, onOpenPlanner: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val stops = remember(trip) { trip.stopsFor(LocalDate.now()) }

    NordicCard {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    trip.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onOpenPlanner) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Planifier les étapes", tint = BrandGold)
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                if (stops.isEmpty()) "Aucune étape prévue aujourd'hui — touchez le crayon pour planifier."
                else "Aujourd'hui — ${stops.size} étapes. Touchez-en une pour lancer Maps.",
                style = MaterialTheme.typography.bodyMedium,
                color = BrandMist,
            )
            Spacer(Modifier.height(8.dp))

            stops.forEachIndexed { index, stop ->
                StopRow(
                    index = index + 1,
                    stop = stop,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        try {
                            NavigationLauncher.navigateFromPhone(context, stop)
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(context, "Aucune app de navigation trouvée", Toast.LENGTH_SHORT).show()
                        }
                    },
                )
            }

            if (stops.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            try {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, NavigationLauncher.dayItineraryUrl(stops)),
                                )
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(context, "Impossible d'ouvrir Google Maps", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Send,
                        contentDescription = null,
                        tint = BrandGold,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(10.dp))
                    Text(
                        "Envoyer la journée complète dans Google Maps",
                        style = MaterialTheme.typography.labelLarge,
                        color = BrandGold,
                    )
                }
            }

            DriveModeRow()
        }
    }
}

/**
 * « Prendre la route » : démarre l'Ange gardien à la main (il démarre déjà
 * tout seul quand Android Auto se connecte).
 */
@Composable
private fun DriveModeRow() {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var driving by remember { mutableStateOf(DriveGuardService.running) }

    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        DriveGuardService.start(context) // même refusée : choc et SOS restent actifs
        driving = true
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                if (driving) {
                    DriveGuardService.stop(context)
                    driving = false
                } else if (LocationProvider.hasPermission(context)) {
                    DriveGuardService.start(context)
                    driving = true
                } else {
                    locationPermission.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (driving) Icons.Rounded.CheckCircle else Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = if (driving) BrandAuroraTeal else BrandIce,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.size(10.dp))
        Text(
            if (driving) "Ange gardien actif — toucher pour arrêter"
            else "Prendre la route — activer l'Ange gardien",
            style = MaterialTheme.typography.labelLarge,
            color = if (driving) BrandAuroraTeal else BrandIce,
        )
    }
}

@Composable
private fun StopRow(index: Int, stop: TripStop, onClick: () -> Unit) {
    val contentAlpha = if (stop.visited) 0.55f else 1f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = !stop.visited, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .border(1.5.dp, BrandGold.copy(alpha = 0.8f * contentAlpha), CircleShape)
                .background(BrandGold.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$index",
                style = MaterialTheme.typography.titleMedium,
                color = BrandGold.copy(alpha = contentAlpha),
            )
        }
        Spacer(Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stop.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = contentAlpha),
            )
            Text(
                listOfNotNull(
                    stop.timeLabel(),
                    if (stop.visited) "Visité" else "Lancer la navigation",
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = (if (stop.visited) BrandAuroraTeal else BrandIce).copy(alpha = contentAlpha),
            )
        }
        Icon(
            if (stop.visited) Icons.Rounded.CheckCircle else Icons.Rounded.Place,
            contentDescription = null,
            tint = (if (stop.visited) BrandAuroraTeal else BrandIce).copy(alpha = contentAlpha),
            modifier = Modifier.size(22.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// Cartes fonctionnalités
// ---------------------------------------------------------------------------

@Composable
private fun FeatureCard(feature: Feature, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "featureScale",
    )

    Box(
        modifier = modifier
            .height(168.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(MaterialTheme.shapes.medium)
            .border(1.dp, BrandGold.copy(alpha = 0.22f), MaterialTheme.shapes.medium)
            .clickable(interactionSource = interaction, indication = null) { /* v1.1 : écrans dédiés */ },
    ) {
        Image(
            painter = painterResource(feature.image),
            contentDescription = feature.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.45f to Color.Transparent,
                        1f to BrandNight.copy(alpha = 0.94f),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text(feature.title, style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(
                feature.subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 17.sp),
                color = BrandMist,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Briques communes
// ---------------------------------------------------------------------------

/** Carte sombre translucide, liseré or discret — la brique visuelle de l'accueil. */
@Composable
private fun NordicCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(BrandSurface.copy(alpha = 0.88f))
            .border(1.dp, BrandGold.copy(alpha = 0.22f), MaterialTheme.shapes.medium),
    ) {
        content()
    }
}

@Composable
private fun GradientButton(text: String, loading: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "buttonScale",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(27.dp))
            .background(Brush.horizontalGradient(listOf(BrandIce, BrandAuroraTeal, BrandAuroraViolet)))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = BrandNight,
                    strokeWidth = 2.5.dp,
                )
            } else {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = BrandNight,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.size(10.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, color = BrandNight)
        }
    }
}
