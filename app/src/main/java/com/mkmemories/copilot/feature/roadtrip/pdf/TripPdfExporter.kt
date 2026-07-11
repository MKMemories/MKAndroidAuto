package com.mkmemories.copilot.feature.roadtrip.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.mkmemories.copilot.feature.places.PlaceEnrichment
import com.mkmemories.copilot.feature.roadtrip.Trip
import com.mkmemories.copilot.feature.roadtrip.timeLabel
import java.io.File
import java.time.format.DateTimeFormatter
import java.util.Locale

// ---------------------------------------------------------------------------
// Modèle de mise en page (pur, testable sans Android graphique)
// ---------------------------------------------------------------------------

internal data class PdfStopBlock(
    val number: Int,
    val title: String,
    val subtitle: String?,
    val description: String?,
)

internal data class PdfSection(val dateLabel: String, val stops: List<PdfStopBlock>)

internal object TripPdfLayout {

    private val dateFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH)

    /** Construit les sections du carnet : une par journée, datée à la française. */
    fun buildSections(trip: Trip, descriptions: Map<String, String?>): List<PdfSection> =
        trip.days.sortedBy { it.date }.map { day ->
            PdfSection(
                dateLabel = day.date.format(dateFormatter).replaceFirstChar { it.uppercase(Locale.FRENCH) },
                stops = day.stops.mapIndexed { index, stop ->
                    val locality = stop.locality?.takeIf { !stop.name.contains(it, ignoreCase = true) }
                    PdfStopBlock(
                        number = index + 1,
                        title = stop.name,
                        subtitle = listOfNotNull(stop.timeLabel()?.let { "Rendez-vous à $it" }, locality)
                            .joinToString(" — ")
                            .takeIf { it.isNotBlank() },
                        description = descriptions[stop.locality ?: stop.name],
                    )
                },
            )
        }

    /** Clés d'enrichissement : une requête Wikipédia par localité unique. */
    fun enrichmentKeys(trip: Trip): List<String> =
        trip.days.flatMap { it.stops }.map { it.locality ?: it.name }.distinct()
}

// ---------------------------------------------------------------------------
// Rendu PDF
// ---------------------------------------------------------------------------

/**
 * Carnet de voyage PDF premium : couverture à la charte nordique (charbon
 * nuit, or, ruban aurore), une section par journée, étapes numérotées et
 * descriptions encyclopédiques des villes / îles (Wikipédia).
 */
object TripPdfExporter {

    // Palette de la charte, en ints Android
    private const val NIGHT = 0xFF0D1420.toInt()
    private const val GOLD = 0xFFE8B84B.toInt()
    private const val GOLD_LIGHT = 0xFFF6DE9A.toInt()
    private const val ICE = 0xFF38D6FF.toInt()
    private const val TEAL = 0xFF2EE6A8.toInt()
    private const val VIOLET = 0xFF8B5CF6.toInt()
    private const val EMBER = 0xFFFF6B2C.toInt()
    private const val INK = 0xFF1C2430.toInt()
    private const val INK_SOFT = 0xFF5A6B7E.toInt()

    private const val PAGE_W = 595 // A4 en points
    private const val PAGE_H = 842
    private const val MARGIN = 56f

    /**
     * Génère le carnet et le dépose dans le cache de l'app (à partager via
     * FileProvider). L'enrichissement Wikipédia est fait en amont par l'appelant.
     */
    fun export(context: Context, trip: Trip, descriptions: Map<String, String?>): File {
        val sections = TripPdfLayout.buildSections(trip, descriptions)
        val document = PdfDocument()

        drawCover(document, trip)
        drawSections(document, sections)

        val dir = File(context.cacheDir, "pdf").apply { mkdirs() }
        val slug = trip.name.lowercase(Locale.FRENCH)
            .replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "voyage" }
        val file = File(dir, "carnet-$slug.pdf")
        file.outputStream().use { document.writeTo(it) }
        document.close()
        return file
    }

    /** Récupère les descriptions Wikipédia de toutes les localités du voyage. */
    suspend fun collectDescriptions(trip: Trip): Map<String, String?> =
        TripPdfLayout.enrichmentKeys(trip).associateWith { PlaceEnrichment.describe(it) }

    // -- Couverture ---------------------------------------------------------

    private fun drawCover(document: PdfDocument, trip: Trip) {
        val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create())
        val canvas = page.canvas

        canvas.drawColor(NIGHT)

        // Ruban aurore
        val ribbon = Paint().apply {
            shader = LinearGradient(
                MARGIN, 0f, PAGE_W - MARGIN, 0f,
                intArrayOf(ICE, TEAL, VIOLET, EMBER), null, Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(MARGIN, 300f, PAGE_W - MARGIN, 306f, 3f, 3f, ribbon)

        val overline = textPaint(11f, GOLD, letterSpacing = 0.25f)
        canvas.drawText("CARNET DE VOYAGE", MARGIN, 270f, overline)

        // Titre du voyage, or clair, sur plusieurs lignes si besoin
        val title = textPaint(34f, GOLD_LIGHT, Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD))
        var y = 350f
        StaticLayout.Builder
            .obtain(trip.name, 0, trip.name.length, title, (PAGE_W - 2 * MARGIN).toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()
            .let { layout ->
                canvas.withTranslation(MARGIN, y) { layout.draw(this) }
                y += layout.height + 26f
            }

        val stopsCount = trip.days.sumOf { it.stops.size }
        val dates = trip.days.map { it.date }.sorted()
        val range = if (dates.isEmpty()) "" else {
            val fmt = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)
            if (dates.size == 1) dates.first().format(fmt)
            else "${dates.first().format(fmt)}  →  ${dates.last().format(fmt)}"
        }
        val subtitle = textPaint(14f, Color.WHITE)
        canvas.drawText(range, MARGIN, y, subtitle)
        canvas.drawText(
            "${trip.days.size} jour${if (trip.days.size > 1) "s" else ""} — $stopsCount étape${if (stopsCount > 1) "s" else ""}",
            MARGIN, y + 24f, textPaint(14f, 0xFFAFC3D6.toInt()),
        )

        canvas.drawText("MK Copilot", MARGIN, PAGE_H - 60f, textPaint(12f, GOLD))
        canvas.drawText(
            "Le copilote qui veille sur vous — carnet généré automatiquement",
            MARGIN, PAGE_H - 42f, textPaint(9f, 0xFF8FA3B8.toInt()),
        )
        document.finishPage(page)
    }

    // -- Pages de contenu ----------------------------------------------------

    private fun drawSections(document: PdfDocument, sections: List<PdfSection>) {
        var pageNumber = 2
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNumber).create())
        var canvas = page.canvas
        canvas.drawColor(Color.WHITE)
        var y = 70f

        fun newPage() {
            finishContentPage(document, page, pageNumber)
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNumber).create())
            canvas = page.canvas
            canvas.drawColor(Color.WHITE)
            y = 70f
        }

        fun ensureRoom(needed: Float) {
            if (y + needed > PAGE_H - 70f) newPage()
        }

        sections.forEach { section ->
            ensureRoom(60f)
            // Tête de journée : date or + filet
            canvas.drawText(section.dateLabel, MARGIN, y, textPaint(17f, 0xFFB8860B.toInt(), Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)))
            y += 10f
            canvas.drawRect(MARGIN, y, PAGE_W - MARGIN, y + 1.2f, Paint().apply { color = GOLD })
            y += 26f

            section.stops.forEach { stop ->
                val descLayout = stop.description?.let { staticLayout(it, 10.5f, INK_SOFT, (PAGE_W - 2 * MARGIN - 40).toInt()) }
                val blockHeight = 40f + (descLayout?.height?.toFloat() ?: 0f)
                ensureRoom(blockHeight)

                // Pastille numérotée or
                val cx = MARGIN + 12f
                canvas.drawCircle(cx, y + 6f, 12f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GOLD })
                val num = textPaint(11f, NIGHT, Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD))
                canvas.drawText("${stop.number}", cx - num.measureText("${stop.number}") / 2, y + 10f, num)

                val textX = MARGIN + 40f
                canvas.drawText(stop.title, textX, y + 4f, textPaint(13f, INK, Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)))
                var innerY = y + 4f
                stop.subtitle?.let {
                    innerY += 16f
                    canvas.drawText(it, textX, innerY, textPaint(10.5f, 0xFF7A8AA0.toInt()))
                }
                descLayout?.let { layout ->
                    innerY += 10f
                    canvas.withTranslation(textX, innerY) { layout.draw(this) }
                    innerY += layout.height
                }
                y = maxOf(y + 28f, innerY + 22f)
            }
            y += 14f
        }
        finishContentPage(document, page, pageNumber)
    }

    private fun finishContentPage(document: PdfDocument, page: PdfDocument.Page, pageNumber: Int) {
        val canvas = page.canvas
        canvas.drawText("MK Copilot — page $pageNumber", MARGIN, PAGE_H - 34f, textPaint(8.5f, 0xFF9AA7B5.toInt()))
        val credit = "Descriptions : Wikipédia (CC BY-SA) — Lieux : OpenStreetMap"
        val creditPaint = textPaint(8.5f, 0xFF9AA7B5.toInt())
        canvas.drawText(credit, PAGE_W - MARGIN - creditPaint.measureText(credit), PAGE_H - 34f, creditPaint)
        document.finishPage(page)
    }

    // -- Aides de dessin -----------------------------------------------------

    private fun textPaint(
        size: Float,
        color: Int,
        typeface: Typeface = Typeface.SANS_SERIF,
        letterSpacing: Float = 0f,
    ) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.textSize = size
        this.color = color
        this.typeface = typeface
        this.letterSpacing = letterSpacing
    }

    private fun staticLayout(text: String, size: Float, color: Int, width: Int): StaticLayout =
        StaticLayout.Builder
            .obtain(text, 0, text.length, textPaint(size, color), width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(2.5f, 1f)
            .build()

    private inline fun Canvas.withTranslation(x: Float, y: Float, block: Canvas.() -> Unit) {
        save()
        translate(x, y)
        block()
        restore()
    }
}
