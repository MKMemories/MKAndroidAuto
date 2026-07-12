package com.mkmemories.copilot.feature.carnet.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.mkmemories.copilot.feature.carnet.CarnetEntry
import com.mkmemories.copilot.feature.carnet.CarnetPhase
import com.mkmemories.copilot.feature.carnet.CarnetVoyage
import com.mkmemories.copilot.feature.carnet.EntryKind
import java.io.File
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Carnet de souvenirs PDF — la version « album » du voyage : couverture magazine,
 * une page par phase, chaque étape avec son icône, ses horaires, ses liens et ses
 * photos, plus les carnets d'inspirations. Pensé pour l'impression et le partage.
 */
object CarnetPdfExporter {

    private const val NIGHT = 0xFF0D1420.toInt()
    private const val GOLD = 0xFFE8B84B.toInt()
    private const val GOLD_LIGHT = 0xFFF6DE9A.toInt()
    private const val ICE = 0xFF38D6FF.toInt()
    private const val TEAL = 0xFF2EE6A8.toInt()
    private const val VIOLET = 0xFF8B5CF6.toInt()
    private const val EMBER = 0xFFFF6B2C.toInt()
    private const val INK = 0xFF1C2430.toInt()
    private const val INK_SOFT = 0xFF5A6B7E.toInt()
    private const val LINK_BLUE = 0xFF1A73E8.toInt()

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 52f
    private val CONTENT_W = PAGE_W - 2 * MARGIN
    private val dateFmt = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)
    private val timeFmt = DateTimeFormatter.ofPattern("HH'h'mm")

    fun export(context: Context, carnet: CarnetVoyage): File {
        val document = PdfDocument()
        drawCover(document, carnet)
        drawPhases(document, carnet)

        val dir = File(context.cacheDir, "pdf").apply { mkdirs() }
        val slug = carnet.title.lowercase(Locale.FRENCH)
            .replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "carnet" }
        val file = File(dir, "carnet-souvenirs-$slug.pdf")
        file.outputStream().use { document.writeTo(it) }
        document.close()
        return file
    }

    // -- Couverture ---------------------------------------------------------

    private fun drawCover(document: PdfDocument, carnet: CarnetVoyage) {
        val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create())
        val canvas = page.canvas
        canvas.drawColor(NIGHT)

        // Cadre or
        val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = GOLD
        }
        canvas.drawRoundRect(28f, 28f, PAGE_W - 28f, PAGE_H - 28f, 14f, 14f, frame)

        canvas.drawText("CARNET DE SOUVENIRS", MARGIN, 150f, textPaint(12f, GOLD, letterSpacing = 0.3f))

        // Titre
        val title = textPaint(40f, GOLD_LIGHT, Typeface.create(Typeface.SERIF, Typeface.BOLD))
        var y = 210f
        staticLayout(carnet.title, title, CONTENT_W.toInt()).let {
            canvas.withTranslation(MARGIN, y) { it.draw(this) }
            y += it.height + 16f
        }

        canvas.drawText(carnet.subtitle, MARGIN, y, textPaint(16f, ICE)); y += 40f

        // Ruban aurore
        val ribbon = Paint().apply {
            shader = LinearGradient(MARGIN, 0f, PAGE_W - MARGIN, 0f, intArrayOf(ICE, TEAL, VIOLET, EMBER), null, Shader.TileMode.CLAMP)
        }
        canvas.drawRoundRect(MARGIN, y, PAGE_W - MARGIN, y + 6f, 3f, 3f, ribbon); y += 44f

        canvas.drawText(carnet.travelers, MARGIN, y, textPaint(17f, Color.WHITE, Typeface.create(Typeface.SERIF, Typeface.ITALIC))); y += 30f
        canvas.drawText(
            "${carnet.start.format(dateFmt)}  —  ${carnet.end.format(dateFmt)}",
            MARGIN, y, textPaint(13f, 0xFFAFC3D6.toInt()),
        )

        canvas.drawText("Souvenirs éternels — voyage complet, partagé avec amour.", MARGIN, PAGE_H - 74f, textPaint(11f, GOLD_LIGHT, Typeface.create(Typeface.SERIF, Typeface.ITALIC)))
        canvas.drawText("MK Copilot", MARGIN, PAGE_H - 52f, textPaint(11f, GOLD))
        document.finishPage(page)
    }

    // -- Phases -------------------------------------------------------------

    /** Curseur de page mutable : centralise page/canvas/y et gère les sauts de page. */
    private class Cursor(val document: PdfDocument) {
        var pageNumber = 2
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNumber).create())
        var canvas: Canvas = page.canvas.also { it.drawColor(Color.WHITE) }
        var y = 66f

        fun newPage() {
            footer(canvas, pageNumber)
            document.finishPage(page)
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNumber).create())
            canvas = page.canvas.also { it.drawColor(Color.WHITE) }
            y = 66f
        }

        fun room(needed: Float) { if (y + needed > PAGE_H - 64f) newPage() }

        fun finish() {
            footer(canvas, pageNumber)
            document.finishPage(page)
        }

        private fun footer(canvas: Canvas, pageNumber: Int) {
            canvas.drawText("MK Copilot — Carnet de souvenirs — page $pageNumber", MARGIN, PAGE_H - 34f, textPaint(8.5f, 0xFF9AA7B5.toInt()))
        }
    }

    private fun drawPhases(document: PdfDocument, carnet: CarnetVoyage) {
        val c = Cursor(document)
        carnet.phases.forEach { phase ->
            c.room(74f)
            c.canvas.drawText(phase.dateLabel.uppercase(Locale.FRENCH), MARGIN, c.y, textPaint(10f, 0xFF3E9AC4.toInt(), letterSpacing = 0.2f)); c.y += 20f
            c.canvas.drawText(phase.title, MARGIN, c.y, textPaint(22f, 0xFFB8860B.toInt(), Typeface.create(Typeface.SERIF, Typeface.BOLD))); c.y += 8f
            c.canvas.drawRect(MARGIN, c.y, PAGE_W - MARGIN, c.y + 1.4f, Paint().apply { color = GOLD }); c.y += 8f
            c.canvas.drawText(phase.place, MARGIN, c.y + 12f, textPaint(11f, INK_SOFT)); c.y += 30f

            phase.entries.forEach { entry -> drawEntry(c, entry) }

            if (phase.inspirations.isNotEmpty()) {
                c.room(30f)
                c.canvas.drawText("✦ Carnet d'inspirations", MARGIN + 4f, c.y, textPaint(12f, 0xFFB8860B.toInt(), Typeface.create(Typeface.SERIF, Typeface.BOLD))); c.y += 18f
                phase.inspirations.forEach { insp ->
                    val layout = staticLayout(insp, textPaint(10.5f, INK_SOFT), (CONTENT_W - 20).toInt())
                    c.room(layout.height + 8f)
                    c.canvas.drawText("•", MARGIN + 4f, c.y + 9f, textPaint(11f, GOLD))
                    c.canvas.withTranslation(MARGIN + 20f, c.y) { layout.draw(this) }
                    c.y += layout.height + 8f
                }
            }
            c.y += 18f
        }
        c.finish()
    }

    /** Dessine une étape (icône, titre, horaire, détail, liens, photos) sur le curseur. */
    private fun drawEntry(c: Cursor, entry: CarnetEntry) {
        c.room(52f)
        val textX = MARGIN + 34f

        c.canvas.drawText(entry.kind.glyph(), MARGIN, c.y + 6f, textPaint(18f, INK))
        val timeLabel = entry.time?.format(timeFmt)
        val header = if (timeLabel != null) "${entry.title}   ·   $timeLabel" else entry.title
        c.canvas.drawText(header, textX, c.y + 4f, textPaint(13f, INK, Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)))
        c.y += 4f

        if (entry.detail.isNotBlank()) {
            val layout = staticLayout(entry.detail, textPaint(10.5f, INK_SOFT), (CONTENT_W - 34).toInt())
            c.room(layout.height + 6f)
            c.y += 14f
            c.canvas.withTranslation(textX, c.y) { layout.draw(this) }
            c.y += layout.height
        }

        entry.links.forEach { link ->
            val txt = "→ ${link.label} : ${link.url}"
            val layout = staticLayout(txt, textPaint(9.5f, LINK_BLUE), (CONTENT_W - 34).toInt())
            c.room(layout.height + 6f)
            c.y += 12f
            c.canvas.withTranslation(textX, c.y) { layout.draw(this) }
            c.y += layout.height
        }

        if (entry.photos.isNotEmpty()) {
            val gap = 10f
            val cellW = (CONTENT_W - 34 - gap) / 2f
            var col = 0
            var rowTopY = c.y + 12f
            var rowH = 0f
            c.y = rowTopY
            entry.photos.forEach { path ->
                val bmp = decode(path) ?: return@forEach
                val h = cellW * bmp.height / bmp.width
                if (col == 0) {
                    c.room(h + 12f)
                    rowTopY = c.y
                    rowH = h
                } else {
                    rowH = maxOf(rowH, h)
                }
                val x = textX + col * (cellW + gap)
                c.canvas.drawRoundRect(RectF(x, rowTopY, x + cellW, rowTopY + h), 6f, 6f, Paint().apply { color = 0xFFECECEC.toInt() })
                c.canvas.drawBitmap(bmp, null, RectF(x, rowTopY, x + cellW, rowTopY + h), Paint(Paint.FILTER_BITMAP_FLAG))
                bmp.recycle()
                col++
                if (col == 2) { col = 0; c.y = rowTopY + rowH + gap }
            }
            if (col == 1) c.y = rowTopY + rowH + gap
        }

        c.y += 16f
    }

    private fun decode(path: String): Bitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0) null
        else BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = (bounds.outWidth / 700).coerceAtLeast(1) })
    } catch (e: Exception) {
        null
    }

    private fun EntryKind.glyph(): String = when (this) {
        EntryKind.DRIVE -> "🧭"
        EntryKind.FLIGHT -> "✈"
        EntryKind.FERRY -> "⛴"
        EntryKind.CAR -> "🚗"
        EntryKind.LODGING -> "🏡"
        EntryKind.VISIT -> "📍"
    }

    private fun textPaint(size: Float, color: Int, typeface: Typeface = Typeface.SANS_SERIF, letterSpacing: Float = 0f) =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = size
            this.color = color
            this.typeface = typeface
            this.letterSpacing = letterSpacing
        }

    private fun staticLayout(text: String, paint: TextPaint, width: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(2.5f, 1f)
            .build()

    private inline fun Canvas.withTranslation(x: Float, y: Float, block: Canvas.() -> Unit) {
        save(); translate(x, y); block(); restore()
    }
}
