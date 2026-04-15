package com.example.optoapp.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.Paciente
import java.io.File
import java.io.FileOutputStream

/**
 * Genera un PDF tipo receta / resumen clínico a partir de la última evaluación:
 * refracción final y DIP en dos columnas; plan de tratamiento antes de próxima cita.
 */
object RecetaEvaluacionPdfGenerator {

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 48f
    private const val BOTTOM_SAFE = 56f
    private const val TABLE_CELL_PAD = 8f
    private const val ACCENT_BAR_H = 5f
    private const val CARD_RADIUS = 10f
    private const val CARD_PAD = 14f
    private const val SECTION_BAR_W = 4f

    private val COLOR_ACCENT = Color.rgb(30, 91, 140)
    private val COLOR_ACCENT_LIGHT = Color.rgb(232, 240, 248)
    private val COLOR_TEXT = Color.rgb(28, 28, 30)
    private val COLOR_TEXT_MUTED = Color.rgb(88, 92, 102)
    private val COLOR_BORDER = Color.rgb(210, 218, 228)
    private val COLOR_CARD_FILL = Color.rgb(252, 253, 255)
    private val COLOR_RULE = Color.rgb(230, 235, 242)

    private sealed class RxGridRow {
        data class Head(val l: String, val od: String, val oi: String) : RxGridRow()
        data class Three(val label: String, val od: String, val oi: String) : RxGridRow()
        data class Merged(val label: String, val value: String) : RxGridRow()
    }

    fun generate(context: Context, paciente: Paciente, eval: EvaluacionClinica): File {
        val dir = File(context.cacheDir, "recetas").apply { mkdirs() }
        val safeName = "receta_${paciente.id.take(12)}_${eval.fecha}.pdf"
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val out = File(dir, safeName)

        val doc = PdfDocument()
        var pageNum = 0
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, ++pageNum).create())
        var canvas = page.canvas
        var y = 0f

        val accentBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_ACCENT }
        val cardFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_CARD_FILL
            style = Paint.Style.FILL
        }
        val cardStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val headerStripPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_ACCENT_LIGHT }
        val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_RULE
            strokeWidth = 1f
        }
        val sectionBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_ACCENT }

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT
            textSize = 20f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = 10f
        }
        val sectionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ACCENT
            textSize = 11.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val rxValuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val tableHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ACCENT
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val smallPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = 8.5f
        }

        fun finishPage() {
            doc.finishPage(page)
        }

        fun drawPageTopAccent(c: Canvas) {
            c.drawRect(0f, 0f, PAGE_W.toFloat(), ACCENT_BAR_H, accentBarPaint)
        }

        fun contentStartY(): Float = ACCENT_BAR_H + 28f

        fun newPage() {
            finishPage()
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, ++pageNum).create())
            canvas = page.canvas
            drawPageTopAccent(canvas)
            y = contentStartY()
        }

        drawPageTopAccent(canvas)
        y = contentStartY()

        fun spaceAvailable(need: Float): Boolean = y + need <= PAGE_H - BOTTOM_SAFE

        fun drawStaticLayout(sl: StaticLayout, x: Float, startY: Float) {
            canvas.save()
            canvas.translate(x, startY)
            sl.draw(canvas)
            canvas.restore()
        }

        fun layoutText(
            text: String,
            paint: TextPaint,
            width: Int,
            align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
        ): StaticLayout =
            StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                .setAlignment(align)
                .setLineSpacing(0f, 1.22f)
                .setIncludePad(false)
                .build()

        fun advance(yDelta: Float) {
            y += yDelta
        }

        fun drawHorizontalRule(atY: Float) {
            canvas.drawLine(MARGIN, atY, PAGE_W - MARGIN, atY, rulePaint)
        }

        // ─── Cabecera ───
        val title = layoutText("Receta optométrica", titlePaint, (PAGE_W - 2 * MARGIN).toInt())
        drawStaticLayout(title, MARGIN, y)
        advance(title.height + 4f)

        val subtitle = layoutText(
            "Resumen clínico · Documento para el paciente",
            subtitlePaint,
            (PAGE_W - 2 * MARGIN).toInt()
        )
        drawStaticLayout(subtitle, MARGIN, y)
        advance(subtitle.height + 18f)

        drawHorizontalRule(y)
        advance(16f)

        // ─── Tarjeta paciente ───
        val patientLines = buildString {
            appendLine(paciente.nombreCompleto)
            append("Edad ${paciente.edad} años")
            if (paciente.telefono.isNotBlank()) append("  ·  ${paciente.telefono}")
            appendLine()
            appendLine("Evaluación: ${DateUtils.formatLocalized(eval.fecha)}")
        }
        val patientInnerW = (PAGE_W - 2 * MARGIN - 2 * CARD_PAD).toInt()
        val patientTitle = layoutText("Paciente", labelPaint, patientInnerW)
        val patientBody = layoutText(patientLines.trimEnd(), bodyPaint, patientInnerW)
        val cardH = CARD_PAD + patientTitle.height + 6f + patientBody.height + CARD_PAD

        if (!spaceAvailable(cardH + 24f)) newPage()

        val cardRect = RectF(MARGIN, y, PAGE_W - MARGIN, y + cardH)
        canvas.drawRoundRect(cardRect, CARD_RADIUS, CARD_RADIUS, cardFillPaint)
        canvas.drawRoundRect(cardRect, CARD_RADIUS, CARD_RADIUS, cardStrokePaint)
        drawStaticLayout(patientTitle, MARGIN + CARD_PAD, y + CARD_PAD)
        drawStaticLayout(patientBody, MARGIN + CARD_PAD, y + CARD_PAD + patientTitle.height + 6f)
        advance(cardH + 22f)

        // ─── Tabla: refracción, AV lejos/cerca, ADD, DIP (celdas) ───
        val showOd = eval.recetaOdEsf.isNotBlank() || eval.recetaOdCil.isNotBlank()
        val showOi = eval.recetaOiEsf.isNotBlank() || eval.recetaOiCil.isNotBlank()
        val hasAnyRxTable =
            showOd || showOi ||
                eval.recetaOdAv.isNotBlank() || eval.recetaOiAv.isNotBlank() ||
                eval.avCcOdLejos.isNotBlank() || eval.avCcOiLejos.isNotBlank() ||
                eval.avCcOdCerca.isNotBlank() || eval.avCcOiCerca.isNotBlank() ||
                eval.addCercaOd.isNotBlank() || eval.addCercaOi.isNotBlank() ||
                eval.addIntermediaOd.isNotBlank() || eval.addIntermediaOi.isNotBlank() ||
                eval.addAv.isNotBlank() ||
                eval.dipLejos.isNotBlank() || eval.dipIntermedio.isNotBlank() || eval.dipCerca.isNotBlank()

        if (hasAnyRxTable) {
            fun fmtRx(esf: String, cil: String, eje: String): String =
                if (esf.isBlank() && cil.isBlank() && eje.isBlank()) "—"
                else "${esf.ifBlank { "—" }} / ${cil.ifBlank { "—" }} × ${eje.ifBlank { "—" }}°"

            fun fmtAdd(odSide: Boolean): String {
                val cerca = if (odSide) eval.addCercaOd else eval.addCercaOi
                val inter = if (odSide) eval.addIntermediaOd else eval.addIntermediaOi
                return buildString {
                    if (cerca.isNotBlank()) append(cerca)
                    if (inter.isNotBlank()) {
                        if (isNotEmpty()) append("\n")
                        append("Intermedio: ").append(inter)
                    }
                    if (isBlank()) append("—")
                }.toString()
            }

            fun dash(s: String) = if (s.isBlank()) "—" else s

            val avLejosOd = eval.recetaOdAv.ifBlank { eval.avCcOdLejos }
            val avLejosOi = eval.recetaOiAv.ifBlank { eval.avCcOiLejos }

            val gridRows = buildList {
                add(RxGridRow.Head("Medida", "OD", "OI"))
                add(
                    RxGridRow.Three(
                        "Refracción final",
                        fmtRx(eval.recetaOdEsf, eval.recetaOdCil, eval.recetaOdEje),
                        fmtRx(eval.recetaOiEsf, eval.recetaOiCil, eval.recetaOiEje)
                    )
                )
                add(RxGridRow.Three("AV lejos", dash(avLejosOd), dash(avLejosOi)))
                add(RxGridRow.Three("ADD", fmtAdd(true), fmtAdd(false)))
                add(RxGridRow.Three("AV cerca", dash(eval.avCcOdCerca), dash(eval.avCcOiCerca)))
                if (eval.addAv.isNotBlank()) {
                    add(RxGridRow.Merged("AV con adición (referencia)", eval.addAv))
                }
                add(RxGridRow.Merged("DIP lejos (mm)", dash(eval.dipLejos)))
                add(RxGridRow.Merged("DIP intermedio (mm)", dash(eval.dipIntermedio)))
                add(RxGridRow.Merged("DIP cerca (mm)", dash(eval.dipCerca)))
            }

            val innerTabW = PAGE_W - 2 * MARGIN
            val labelColW = innerTabW * 0.30f
            val dataColW = (innerTabW - labelColW) / 2f
            val xLab = MARGIN
            val xOd = xLab + labelColW
            val xOi = xOd + dataColW
            val xEnd = MARGIN + innerTabW

            fun slCell(text: String, paint: TextPaint, w: Int, align: Layout.Alignment) =
                StaticLayout.Builder.obtain(text, 0, text.length, paint, w.coerceAtLeast(8))
                    .setAlignment(align)
                    .setLineSpacing(0f, 1.18f)
                    .setIncludePad(false)
                    .build()

            fun rowHeight(row: RxGridRow): Float {
                val pad = 2 * TABLE_CELL_PAD
                val wL = (labelColW - 2 * TABLE_CELL_PAD).toInt().coerceAtLeast(8)
                val wD = (dataColW - 2 * TABLE_CELL_PAD).toInt().coerceAtLeast(8)
                val wM = ((dataColW * 2) - 2 * TABLE_CELL_PAD).toInt().coerceAtLeast(8)
                return when (row) {
                    is RxGridRow.Head -> {
                        val a = slCell(row.l, tableHeaderPaint, wL, Layout.Alignment.ALIGN_NORMAL)
                        val b = slCell(row.od, tableHeaderPaint, wD, Layout.Alignment.ALIGN_CENTER)
                        val c = slCell(row.oi, tableHeaderPaint, wD, Layout.Alignment.ALIGN_CENTER)
                        maxOf(a.height, b.height, c.height) + pad
                    }
                    is RxGridRow.Three -> {
                        val a = slCell(row.label, labelPaint, wL, Layout.Alignment.ALIGN_NORMAL)
                        val b = slCell(row.od, rxValuePaint, wD, Layout.Alignment.ALIGN_CENTER)
                        val c = slCell(row.oi, rxValuePaint, wD, Layout.Alignment.ALIGN_CENTER)
                        maxOf(a.height, b.height, c.height) + pad
                    }
                    is RxGridRow.Merged -> {
                        val a = slCell(row.label, labelPaint, wL, Layout.Alignment.ALIGN_NORMAL)
                        val b = slCell(row.value, rxValuePaint, wM, Layout.Alignment.ALIGN_CENTER)
                        maxOf(a.height, b.height) + pad
                    }
                }
            }

            val titleSl = layoutText("Prescripción y medidas", sectionPaint, innerTabW.toInt())
            val titleBlockH = titleSl.height + 10f
            val rowHeights = gridRows.map { rowHeight(it) }
            val sumRows = rowHeights.sum()
            val tableOuterPad = 12f
            val totalBlockH = titleBlockH + sumRows + 2 * tableOuterPad + 4f

            if (!spaceAvailable(totalBlockH + 24f)) newPage()

            val boxTop = y
            val boxRect = RectF(MARGIN, boxTop, PAGE_W - MARGIN, boxTop + totalBlockH)
            canvas.drawRoundRect(boxRect, CARD_RADIUS, CARD_RADIUS, cardFillPaint)
            canvas.drawRoundRect(boxRect, CARD_RADIUS, CARD_RADIUS, cardStrokePaint)

            var ty = boxTop + tableOuterPad
            drawStaticLayout(titleSl, MARGIN + tableOuterPad, ty)
            ty += titleBlockH

            val tableBodyTop = ty

            val gridStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_BORDER
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            val altRowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(30, 30, 91, 140) }

            var rowY = ty
            gridRows.forEachIndexed { idx, row ->
                val rh = rowHeights[idx]
                when {
                    row is RxGridRow.Head ->
                        canvas.drawRect(xLab, rowY, xEnd, rowY + rh, headerStripPaint)
                    idx > 0 && idx % 2 == 0 ->
                        canvas.drawRect(xLab, rowY, xEnd, rowY + rh, altRowPaint)
                }
                val wL = (labelColW - 2 * TABLE_CELL_PAD).toInt().coerceAtLeast(8)
                val wD = (dataColW - 2 * TABLE_CELL_PAD).toInt().coerceAtLeast(8)
                val wM = ((dataColW * 2) - 2 * TABLE_CELL_PAD).toInt().coerceAtLeast(8)
                when (row) {
                    is RxGridRow.Head -> {
                        val slL = slCell(row.l, tableHeaderPaint, wL, Layout.Alignment.ALIGN_NORMAL)
                        val slO = slCell(row.od, tableHeaderPaint, wD, Layout.Alignment.ALIGN_CENTER)
                        val slI = slCell(row.oi, tableHeaderPaint, wD, Layout.Alignment.ALIGN_CENTER)
                        val base = rowY + TABLE_CELL_PAD
                        drawStaticLayout(slL, xLab + TABLE_CELL_PAD, base)
                        drawStaticLayout(slO, xOd + TABLE_CELL_PAD, base)
                        drawStaticLayout(slI, xOi + TABLE_CELL_PAD, base)
                    }
                    is RxGridRow.Three -> {
                        val slL = slCell(row.label, labelPaint, wL, Layout.Alignment.ALIGN_NORMAL)
                        val slO = slCell(row.od, rxValuePaint, wD, Layout.Alignment.ALIGN_CENTER)
                        val slI = slCell(row.oi, rxValuePaint, wD, Layout.Alignment.ALIGN_CENTER)
                        val base = rowY + TABLE_CELL_PAD
                        drawStaticLayout(slL, xLab + TABLE_CELL_PAD, base)
                        drawStaticLayout(slO, xOd + TABLE_CELL_PAD, base)
                        drawStaticLayout(slI, xOi + TABLE_CELL_PAD, base)
                    }
                    is RxGridRow.Merged -> {
                        val slL = slCell(row.label, labelPaint, wL, Layout.Alignment.ALIGN_NORMAL)
                        val slV = slCell(row.value, rxValuePaint, wM, Layout.Alignment.ALIGN_CENTER)
                        val base = rowY + TABLE_CELL_PAD
                        drawStaticLayout(slL, xLab + TABLE_CELL_PAD, base)
                        drawStaticLayout(slV, xOd + TABLE_CELL_PAD, base)
                    }
                }
                rowY += rh
            }

            // Cuadrícula: bordes por fila (DIP y filas fusionadas sin línea OD|OI)
            canvas.drawLine(xLab, tableBodyTop, xEnd, tableBodyTop, gridStroke)
            var ly = tableBodyTop
            gridRows.forEachIndexed { idx, row ->
                val h = rowHeights[idx]
                canvas.drawLine(xLab, ly + h, xEnd, ly + h, gridStroke)
                canvas.drawLine(xLab, ly, xLab, ly + h, gridStroke)
                canvas.drawLine(xEnd, ly, xEnd, ly + h, gridStroke)
                canvas.drawLine(xOd, ly, xOd, ly + h, gridStroke)
                if (row is RxGridRow.Head || row is RxGridRow.Three) {
                    canvas.drawLine(xOi, ly, xOi, ly + h, gridStroke)
                }
                ly += h
            }

            advance(totalBlockH + 18f)
        }

        fun section(title: String, body: String) {
            if (body.isBlank()) return
            val innerW = (PAGE_W - 2 * MARGIN - SECTION_BAR_W - 12f).toInt()
            val t = layoutText(title, sectionPaint, innerW)
            val b = layoutText(body.trim(), bodyPaint, innerW)
            val contentH = t.height + 8f + b.height
            val blockH = contentH + 16f
            if (!spaceAvailable(blockH)) newPage()

            val blockTop = y
            val barRect = RectF(MARGIN, blockTop, MARGIN + SECTION_BAR_W, blockTop + contentH)
            canvas.drawRoundRect(barRect, 2f, 2f, sectionBarPaint)

            drawStaticLayout(t, MARGIN + SECTION_BAR_W + 12f, blockTop)
            drawStaticLayout(b, MARGIN + SECTION_BAR_W + 12f, blockTop + t.height + 8f)
            advance(contentH + 18f)
        }

        val diag = buildString {
            if (eval.diagnostico.isNotBlank()) appendLine(eval.diagnostico)
            val dOd = eval.diagnosticoOd.firstOrNull().orEmpty()
            val dOi = eval.diagnosticoOi.firstOrNull().orEmpty()
            if (dOd.isNotBlank()) appendLine("OD: $dOd")
            if (dOi.isNotBlank()) appendLine("OI: $dOi")
            if (eval.diagnosticoOtros.isNotEmpty()) {
                appendLine("Otros: ${eval.diagnosticoOtros.joinToString(", ")}")
            }
        }
        if (diag.isNotBlank()) section("Diagnóstico", diag)

        val cond = buildList {
            if (eval.otrosPresbicia) add("Presbicia")
            if (eval.otrosAnisometropia) add("Anisometropía")
            if (eval.otrosAmbliopia) add("Ambliopía")
        }.joinToString("\n")
        if (cond.isNotBlank()) section("Condiciones asociadas", cond)

        val prisma = buildString {
            if (eval.prismaOdValor.isNotBlank()) appendLine("OD: ${eval.prismaOdValor} (base ${eval.prismaOdBase})")
            if (eval.prismaOiValor.isNotBlank()) appendLine("OI: ${eval.prismaOiValor} (base ${eval.prismaOiBase})")
        }
        if (prisma.isNotBlank()) section("Prismas", prisma)

        val q = buildString {
            val ko = eval.k1Od.isNotBlank() || eval.k2Od.isNotBlank()
            val ki = eval.k1Oi.isNotBlank() || eval.k2Oi.isNotBlank()
            if (ko) appendLine("OD: ${eval.k1Od} / ${eval.k2Od}")
            if (ki) appendLine("OI: ${eval.k1Oi} / ${eval.k2Oi}")
        }
        if (q.isNotBlank()) section("Queratometría", q)

        if (eval.lcOdEsf.isNotBlank() || eval.lcOiEsf.isNotBlank()) {
            section(
                "Contactología",
                "Incluye datos de adaptación de lentes de contacto (evaluación completa en la aplicación)."
            )
        }

        if (eval.planTratamiento.isNotBlank()) {
            section("Plan de tratamiento", eval.planTratamiento)
        }

        if (eval.observaciones.isNotBlank()) {
            section("Observaciones", eval.observaciones)
        }

        val proximaBlock = buildString {
            if (eval.proximaCita != null) {
                appendLine("Próxima cita: ${DateUtils.formatLocalized(eval.proximaCita)}")
            }
            if (eval.proximaFechaControl.isNotBlank()) {
                appendLine("Próximo control: ${eval.proximaFechaControl}")
            }
        }
        if (proximaBlock.isNotBlank()) {
            section("Seguimiento", proximaBlock.trim())
        }

        advance(8f)
        drawHorizontalRule(y)
        advance(14f)

        val footerText =
            "Documento generado con OptoApp · No sustituye la valoración clínica presencial ni la firma del profesional."
        val footerLayout = layoutText(footerText, smallPaint, (PAGE_W - 2 * MARGIN).toInt(), Layout.Alignment.ALIGN_CENTER)
        val footerH = footerLayout.height + 8f
        if (!spaceAvailable(footerH)) newPage()
        drawStaticLayout(footerLayout, MARGIN, y)
        advance(footerLayout.height.toFloat())

        doc.finishPage(page)

        FileOutputStream(out).use { fos ->
            doc.writeTo(fos)
        }
        doc.close()
        return out
    }

    fun openPdf(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(Intent.createChooser(intent, "Abrir receta PDF"))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No hay ninguna app para abrir PDF; instala un visor de PDF.", Toast.LENGTH_LONG).show()
        }
    }
}
