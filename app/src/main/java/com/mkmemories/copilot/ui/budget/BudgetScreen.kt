package com.mkmemories.copilot.ui.budget

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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mkmemories.copilot.feature.budget.BudgetItem
import com.mkmemories.copilot.feature.budget.BudgetMath
import com.mkmemories.copilot.feature.budget.BudgetStats
import com.mkmemories.copilot.feature.budget.BudgetStore
import com.mkmemories.copilot.feature.budget.GreeceBudget
import com.mkmemories.copilot.ui.theme.BrandAuroraViolet
import com.mkmemories.copilot.ui.theme.BrandAuroraTeal
import com.mkmemories.copilot.ui.theme.BrandEmber
import com.mkmemories.copilot.ui.theme.BrandGold
import com.mkmemories.copilot.ui.theme.BrandGoldLight
import com.mkmemories.copilot.ui.theme.BrandIce
import com.mkmemories.copilot.ui.theme.BrandMist
import com.mkmemories.copilot.ui.theme.BrandNight
import com.mkmemories.copilot.ui.theme.BrandSurface
import com.mkmemories.copilot.ui.theme.BrandSurfaceHigh

/**
 * Suivi de budget : total, payé, reste à payer, la liste précise des dépenses
 * avec leur statut (basculable d'un tap) et l'ajout de nouvelles prestations.
 */
@Composable
fun BudgetScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { BudgetStore(context) }
    val base = remember { GreeceBudget.items() }

    var version by remember { mutableIntStateOf(0) }
    val items = remember(version) { store.effectiveItems(base) }
    var adding by remember { mutableStateOf(false) }

    val total = BudgetMath.totalCents(items)
    val paid = BudgetMath.paidCents(items)
    val remaining = BudgetMath.remainingCents(items)

    BackHandler(onBack = onBack)

    Box(modifier = Modifier.fillMaxSize().background(BrandNight)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Retour", tint = Color.White)
                }
                Text("Budget du voyage", style = MaterialTheme.typography.titleMedium, color = BrandGold)
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(12.dp))
                SummaryCard(total = total, paid = paid, remaining = remaining)

                Spacer(Modifier.height(18.dp))
                Text("STATISTIQUES", style = MaterialTheme.typography.labelSmall, color = BrandGold, letterSpacing = 2.sp)
                Spacer(Modifier.height(8.dp))
                StatsRow(items = items)

                Spacer(Modifier.height(16.dp))
                Text("RÉPARTITION PAR POSTE", style = MaterialTheme.typography.labelSmall, color = BrandGold, letterSpacing = 2.sp)
                Spacer(Modifier.height(8.dp))
                BreakdownCard(items = items, total = total)

                Spacer(Modifier.height(18.dp))
                Text("DÉPENSES", style = MaterialTheme.typography.labelSmall, color = BrandGold, letterSpacing = 2.sp)
                Spacer(Modifier.height(8.dp))

                items.forEach { item ->
                    BudgetRow(
                        item = item,
                        onTogglePaid = { store.setPaid(item.id, !item.paid); version++ },
                        onDelete = if (item.custom) {
                            { store.removeCustom(item.id); version++ }
                        } else {
                            null
                        },
                    )
                    Spacer(Modifier.height(10.dp))
                }

                Spacer(Modifier.height(4.dp))
                AddRow { adding = true }
                Spacer(Modifier.height(36.dp))
            }
        }
    }

    if (adding) {
        AddPaymentDialog(
            onDismiss = { adding = false },
            onConfirm = { item -> store.addCustom(item); version++; adding = false },
        )
    }
}

@Composable
private fun SummaryCard(total: Long, paid: Long, remaining: Long) {
    val ratio = if (total == 0L) 0f else paid.toFloat() / total
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(BrandSurfaceHigh, BrandSurface)))
            .border(1.dp, BrandGold.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
            .padding(20.dp),
    ) {
        Text("RESTE À PAYER", style = MaterialTheme.typography.labelSmall, color = BrandMist, letterSpacing = 2.sp)
        Spacer(Modifier.height(4.dp))
        Text(BudgetMath.formatEuros(remaining), fontSize = 34.sp, fontWeight = FontWeight.Bold, color = BrandGoldLight)
        Spacer(Modifier.height(14.dp))
        Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(BrandNight)) {
            Box(modifier = Modifier.fillMaxWidth(ratio).height(10.dp).clip(RoundedCornerShape(5.dp)).background(Brush.horizontalGradient(listOf(BrandAuroraTeal, BrandIce))))
        }
        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Metric("Payé", BudgetMath.formatEuros(paid), BrandAuroraTeal)
            Metric("Total", BudgetMath.formatEuros(total), Color.White)
        }
    }
}

@Composable
private fun Metric(label: String, value: String, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = BrandMist)
        Text(value, style = MaterialTheme.typography.titleMedium, color = color)
    }
}

/** Trois statistiques clés : prestations réglées, plus gros poste, part payée. */
@Composable
private fun StatsRow(items: List<BudgetItem>) {
    val total = BudgetMath.totalCents(items)
    val paid = BudgetMath.paidCents(items)
    val paidPct = if (total == 0L) 0 else (paid * 100 / total).toInt()
    val biggest = BudgetStats.biggestPoste(items)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile(Modifier.weight(1f), "Réglées", "${BudgetStats.paidCount(items)}/${items.size}", "prestations")
        StatTile(Modifier.weight(1f), "Payé", "$paidPct %", "du total")
        StatTile(Modifier.weight(1f), "Plus gros poste", biggest?.poste ?: "—", biggest?.let { BudgetMath.formatEuros(it.totalCents) } ?: "")
    }
}

@Composable
private fun StatTile(modifier: Modifier, label: String, value: String, hint: String) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(BrandSurface.copy(alpha = 0.92f))
            .border(1.dp, BrandGold.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
            .padding(12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = BrandMist)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, color = BrandGoldLight, maxLines = 1)
        if (hint.isNotBlank()) Text(hint, style = MaterialTheme.typography.labelSmall, color = BrandMist, maxLines = 1)
    }
}

/** Barres empilées par poste : largeur = part du total, remplissage payé / restant. */
@Composable
private fun BreakdownCard(items: List<BudgetItem>, total: Long) {
    val postes = BudgetStats.byPoste(items)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BrandSurface.copy(alpha = 0.92f))
            .border(1.dp, BrandGold.copy(alpha = 0.2f), RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        postes.forEachIndexed { index, stat ->
            if (index > 0) Spacer(Modifier.height(14.dp))
            PosteBar(stat = stat, total = total)
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendDot(BrandAuroraTeal, "Payé")
            LegendDot(BrandGold, "Reste à payer")
        }
    }
}

@Composable
private fun PosteBar(stat: BudgetStats.PosteStat, total: Long) {
    val share = BudgetStats.share(stat.totalCents, total)
    val pct = (share * 100).toInt()
    val accent = posteAccent(stat.poste)
    val paidWithin = if (stat.totalCents == 0L) 0f else (stat.paidCents.toDouble() / stat.totalCents).toFloat()

    Column {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(posteIcon(stat.poste), fontSize = 15.sp)
            Spacer(Modifier.size(8.dp))
            Text(stat.poste, style = MaterialTheme.typography.titleSmall, color = Color.White, modifier = Modifier.weight(1f))
            Text(BudgetMath.formatEuros(stat.totalCents), style = MaterialTheme.typography.titleSmall, color = accent)
            Spacer(Modifier.size(8.dp))
            Text("$pct %", style = MaterialTheme.typography.labelMedium, color = BrandMist)
        }
        Spacer(Modifier.height(6.dp))
        // Piste pleine largeur ; segment = part du total ; segment scindé payé/restant.
        Box(modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(BrandNight)) {
            Box(modifier = Modifier.fillMaxWidth(share.coerceIn(0.02f, 1f)).height(12.dp).clip(RoundedCornerShape(6.dp))) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (paidWithin > 0f) {
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(paidWithin).background(BrandAuroraTeal))
                    }
                    Box(modifier = Modifier.fillMaxSize().background(BrandGold.copy(alpha = 0.85f)))
                }
            }
        }
        if (stat.remainingCents > 0L) {
            Spacer(Modifier.height(3.dp))
            Text("reste ${BudgetMath.formatEuros(stat.remainingCents)} · ${stat.count} prestation${if (stat.count > 1) "s" else ""}", style = MaterialTheme.typography.labelSmall, color = BrandMist)
        } else {
            Spacer(Modifier.height(3.dp))
            Text("entièrement payé · ${stat.count} prestation${if (stat.count > 1) "s" else ""}", style = MaterialTheme.typography.labelSmall, color = BrandAuroraTeal)
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(color))
        Spacer(Modifier.size(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = BrandMist)
    }
}

private fun posteIcon(poste: String): String = when (poste) {
    "Vols" -> "✈️"
    "Hébergements" -> "🏡"
    "Voitures" -> "🚗"
    "Ferries" -> "⛴️"
    else -> "🎫"
}

private fun posteAccent(poste: String): Color = when (poste) {
    "Vols" -> BrandIce
    "Hébergements" -> BrandGoldLight
    "Voitures" -> BrandGold
    "Ferries" -> BrandAuroraTeal
    else -> BrandAuroraViolet
}

@Composable
private fun BudgetRow(item: BudgetItem, onTogglePaid: () -> Unit, onDelete: (() -> Unit)?) {
    val accent = if (item.paid) BrandAuroraTeal else BrandGold
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BrandSurface.copy(alpha = 0.92f))
            .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
            .padding(14.dp)
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Pastille statut
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(if (item.paid) BrandAuroraTeal else BrandSurfaceHigh)
                .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(15.dp))
                .clickable(onClick = onTogglePaid),
            contentAlignment = Alignment.Center,
        ) {
            if (item.paid) Icon(Icons.Rounded.Check, contentDescription = "Payé", tint = BrandNight, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.category, style = MaterialTheme.typography.labelMedium, color = accent)
            Text(item.label, style = MaterialTheme.typography.titleSmall, color = Color.White)
            val sub = listOf(item.dates, item.schedule).filter { it.isNotBlank() }.joinToString(" · ")
            if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall, color = BrandMist)
            if (!item.paid && item.note.isNotBlank()) {
                Text("● ${item.note}", style = MaterialTheme.typography.labelSmall, color = BrandEmber)
            }
        }
        Spacer(Modifier.size(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(BudgetMath.formatEuros(item.amountCents), style = MaterialTheme.typography.titleSmall, color = if (item.paid) BrandMist else Color.White)
            Text(if (item.paid) "payé" else "à payer", style = MaterialTheme.typography.labelSmall, color = accent)
            if (onDelete != null) {
                Spacer(Modifier.height(4.dp))
                Icon(
                    Icons.Rounded.Clear,
                    contentDescription = "Supprimer",
                    tint = BrandEmber,
                    modifier = Modifier.size(18.dp).clip(RoundedCornerShape(9.dp)).clickable(onClick = onDelete),
                )
            }
        }
    }
}

@Composable
private fun AddRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, BrandAuroraTeal.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, tint = BrandAuroraTeal, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(8.dp))
        Text("Ajouter un paiement", style = MaterialTheme.typography.labelLarge, color = BrandAuroraTeal)
    }
}

@Composable
private fun AddPaymentDialog(onDismiss: () -> Unit, onConfirm: (BudgetItem) -> Unit) {
    var category by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var paid by remember { mutableStateOf(false) }

    fun amountCents(): Long? {
        val normalized = amount.trim().replace(" ", "").replace("€", "").replace(',', '.')
        val value = normalized.toDoubleOrNull() ?: return null
        return Math.round(value * 100)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = label.isNotBlank() && amountCents() != null,
                onClick = {
                    onConfirm(
                        BudgetItem(
                            id = "extra-${System.currentTimeMillis()}",
                            category = category.trim().ifBlank { "Extra" },
                            label = label.trim(),
                            dates = "",
                            schedule = "",
                            amountCents = amountCents() ?: 0L,
                            paid = paid,
                            custom = true,
                        ),
                    )
                },
            ) { Text("Ajouter", color = BrandGold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler", color = BrandMist) } },
        title = { Text("Nouveau paiement") },
        text = {
            Column {
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Catégorie (Extra, Restaurant…)") }, singleLine = true, colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Description") }, singleLine = true, colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Montant (€)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { paid = !paid }.padding(6.dp)) {
                    Box(
                        modifier = Modifier.size(24.dp).clip(RoundedCornerShape(12.dp)).background(if (paid) BrandAuroraTeal else BrandSurfaceHigh),
                        contentAlignment = Alignment.Center,
                    ) { if (paid) Icon(Icons.Rounded.Check, contentDescription = null, tint = BrandNight, modifier = Modifier.size(15.dp)) }
                    Spacer(Modifier.size(8.dp))
                    Text(if (paid) "Déjà payé" else "À payer", color = BrandMist)
                }
            }
        },
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = BrandIce,
    unfocusedBorderColor = BrandMist.copy(alpha = 0.4f),
    focusedLabelColor = BrandIce,
    unfocusedLabelColor = BrandMist,
    cursorColor = BrandIce,
)
