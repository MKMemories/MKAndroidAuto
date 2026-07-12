package com.mkmemories.copilot.ui.carnet

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mkmemories.copilot.feature.carnet.CarnetPhase
import com.mkmemories.copilot.feature.carnet.CarnetVoyage
import com.mkmemories.copilot.feature.carnet.EntryKind
import com.mkmemories.copilot.ui.theme.BrandAuroraTeal
import com.mkmemories.copilot.ui.theme.BrandGold
import com.mkmemories.copilot.ui.theme.BrandGoldLight
import com.mkmemories.copilot.ui.theme.BrandMist
import com.mkmemories.copilot.ui.theme.BrandNight
import com.mkmemories.copilot.ui.theme.BrandSurface
import com.mkmemories.copilot.ui.theme.BrandSurfaceHigh

private enum class NodeState { DONE, CURRENT, UPCOMING }

/**
 * Ruban d'itinéraire dynamique : une carte de route verticale générée depuis le
 * carnet. Le trait pointillé « s'écoule » (animation), chaque escale est un nœud
 * numéroté, et l'état (franchie / en cours / à venir) se met à jour selon la
 * progression. Un tap sur un nœud lance la navigation vers son premier trajet.
 */
@Composable
fun ItineraryRibbon(
    carnet: CarnetVoyage,
    isEntryDone: (String) -> Boolean,
    onNodeNavigate: (CarnetPhase) -> Unit,
) {
    val phases = carnet.phases
    val states = phases.map { phase ->
        val allDone = phase.entries.isNotEmpty() && phase.entryIds.all { isEntryDone(it) }
        if (allDone) NodeState.DONE else NodeState.UPCOMING
    }.toMutableList()
    // La première escale non franchie devient « en cours ».
    states.indexOfFirst { it == NodeState.UPCOMING }.takeIf { it >= 0 }?.let { states[it] = NodeState.CURRENT }

    // Animation : décalage du pointillé (trait qui s'écoule) + pulsation du nœud courant.
    val transition = rememberInfiniteTransition(label = "ribbon")
    val dashPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 34f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "dash",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(BrandSurface, BrandNight)))
            .border(1.dp, BrandGold.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
            .padding(vertical = 8.dp),
    ) {
        phases.forEachIndexed { i, phase ->
            RibbonRow(
                number = i + 1,
                phase = phase,
                state = states[i],
                isFirst = i == 0,
                isLast = i == phases.lastIndex,
                dashPhase = dashPhase,
                pulse = pulse,
                onNavigate = { onNodeNavigate(phase) },
            )
        }
    }
}

@Composable
private fun RibbonRow(
    number: Int,
    phase: CarnetPhase,
    state: NodeState,
    isFirst: Boolean,
    isLast: Boolean,
    dashPhase: Float,
    pulse: Float,
    onNavigate: () -> Unit,
) {
    val accent = when (state) {
        NodeState.DONE -> BrandAuroraTeal
        NodeState.CURRENT -> BrandGold
        NodeState.UPCOMING -> BrandMist.copy(alpha = 0.5f)
    }

    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        // Colonne timeline : trait pointillé animé + nœud
        Box(modifier = Modifier.width(58.dp).fillMaxHeight()) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxHeight().width(58.dp)) {
                drawConnectors(
                    dashPhase = dashPhase,
                    isFirst = isFirst,
                    isLast = isLast,
                    doneAbove = state == NodeState.DONE,
                    doneBelow = state == NodeState.DONE,
                )
            }
            // Nœud numéroté
            val nodeSize = if (state == NodeState.CURRENT) 40.dp else 34.dp
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 20.dp)
                    .size(nodeSize)
                    .clip(RoundedCornerShape(nodeSize / 2))
                    .background(if (state == NodeState.UPCOMING) BrandSurfaceHigh else accent.copy(alpha = if (state == NodeState.CURRENT) pulse else 1f))
                    .border(
                        width = if (state == NodeState.CURRENT) 2.dp else 0.dp,
                        color = BrandGoldLight.copy(alpha = if (state == NodeState.CURRENT) pulse else 0f),
                        shape = RoundedCornerShape(nodeSize / 2),
                    )
                    .clickable(onClick = onNavigate),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    when (state) {
                        NodeState.DONE -> "✓"
                        else -> number.toString()
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (state == NodeState.UPCOMING) BrandMist else BrandNight,
                )
            }
        }

        // Carte de l'escale
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp, top = 18.dp, bottom = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(phase.leadTransport.glyph(), fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    phase.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (state == NodeState.UPCOMING) BrandMist else Color.White,
                )
            }
            Text(phase.place, style = MaterialTheme.typography.bodyMedium, color = accent)
            Text(phase.dateLabel, style = MaterialTheme.typography.labelMedium, color = BrandMist)
            if (state == NodeState.CURRENT && phase.firstDrive != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "● Étape en cours — tapez le numéro pour naviguer",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandGoldLight,
                )
            }
        }
    }
}

/** Trait pointillé animé au-dessus et au-dessous du nœud (offset par densité géré par l'appelant). */
private fun DrawScope.drawConnectors(
    dashPhase: Float,
    isFirst: Boolean,
    isLast: Boolean,
    doneAbove: Boolean,
    doneBelow: Boolean,
) {
    val cx = size.width / 2f
    // le nœud est à ~20dp du haut ; on approxime son centre à 37px + rayon selon état.
    val nodeCenterY = 37f + 17f
    val effect = PathEffect.dashPathEffect(floatArrayOf(10f, 12f), dashPhase)
    val done = Color(0xFF2EE6A8)
    val pending = Color(0xFF6F8296)

    if (!isFirst) {
        drawLine(
            color = if (doneAbove) done else pending,
            start = Offset(cx, 0f),
            end = Offset(cx, nodeCenterY),
            strokeWidth = 4f,
            cap = StrokeCap.Round,
            pathEffect = effect,
        )
    }
    if (!isLast) {
        drawLine(
            color = if (doneBelow) done else pending,
            start = Offset(cx, nodeCenterY),
            end = Offset(cx, size.height),
            strokeWidth = 4f,
            cap = StrokeCap.Round,
            pathEffect = effect,
        )
    }
}

private fun EntryKind?.glyph(): String = when (this) {
    EntryKind.DRIVE -> "🧭"
    EntryKind.FLIGHT -> "✈️"
    EntryKind.FERRY -> "⛴️"
    EntryKind.CAR -> "🚗"
    EntryKind.LODGING -> "🏡"
    null -> "•"
}
