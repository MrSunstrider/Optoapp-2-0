package com.example.optoapp.util

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.graphics.withTranslation
import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.Paciente

/**
 * Builder para generar [PdfDocument] de fórmula optométrica / resumen clínico.
 *
 * Cada método retorna `this` para encadenamiento. Llamar a [build] al final
 * para obtener el documento completo con pie de página.
 */
class RecetaPdfBuilder {

    private val doc = PdfDocument()
    private var pageNum = 0
    private var page = doc.startPage(
        PdfDocument.PageInfo.Builder(PdfStyle.PAGE_W, PdfStyle.PAGE_H, ++pageNum).create()
    )
    private var canvas: Canvas = page.canvas
    private var y = 0f

    // ─── Public API ─────────────────────────────────────────────────────────

    /**
     * Dibuja la cabecera del PDF: título, subtítulo, tarjeta de paciente.
     */
    fun addHeader(paciente: Paciente, eval: EvaluacionClinica): RecetaPdfBuilder {
        initPage()

        val title = layoutText("Fórmula optométrica", PdfStyle.titlePaint, contentWidth())
        drawStaticLayout(title, PdfStyle.MARGIN, y)
        advance(title.height + 4f)

        val subtitle = layoutText(
            "Resumen clínico · Documento para el paciente",
            PdfStyle.subtitlePaint,
            contentWidth()
        )
        drawStaticLayout(subtitle, PdfStyle.MARGIN, y)
        advance(subtitle.height + 18f)

        drawHorizontalRule()
        advance(16f)

        // Patient card
        val patientLines = buildString {
            appendLine(paciente.nombreCompleto)
            append("Edad ${paciente.edad} años")
            if (paciente.telefono.isNotBlank()) append("  ·  ${paciente.telefono}")
            appendLine()
            appendLine("Evaluación: ${DateUtils.formatLocalized(eval.fecha)}")
        }
        val patientInnerW = contentWidth() - (2 * PdfStyle.CARD_PAD).toInt()
        val patientTitle = layoutText("Paciente", PdfStyle.labelPaint, patientInnerW)
        val patientBody = layoutText(patientLines.trimEnd(), PdfStyle.bodyPaint, patientInnerW)
        val cardH = PdfStyle.CARD_PAD + patientTitle.height + 6f + patientBody.height + PdfStyle.CARD_PAD

        ensureSpace(cardH + 24f)

        val cardRect = RectF(
            PdfStyle.MARGIN, y,
            PdfStyle.PAGE_W - PdfStyle.MARGIN, y + cardH
        )
        canvas.drawRoundRect(cardRect, PdfStyle.CARD_RADIUS, PdfStyle.CARD_RADIUS, PdfStyle.cardFillPaint)
        canvas.drawRoundRect(cardRect, PdfStyle.CARD_RADIUS, PdfStyle.CARD_RADIUS, PdfStyle.cardStrokePaint)
        drawStaticLayout(patientTitle, PdfStyle.MARGIN + PdfStyle.CARD_PAD, y + PdfStyle.CARD_PAD)
        drawStaticLayout(
            patientBody,
            PdfStyle.MARGIN + PdfStyle.CARD_PAD,
            y + PdfStyle.CARD_PAD + patientTitle.height + 6f
        )
        advance(cardH + 22f)

        return this
    }

    /**
     * Dibuja la tabla de refracción, AV, ADD y DIP.
     */
    fun addRefraccion(eval: EvaluacionClinica): RecetaPdfBuilder {
        val showOd = eval.recetaOdEsf.isNotBlank() || eval.recetaOdCil.isNotBlank()
        val showOi = eval.recetaOiEsf.isNotBlank() || eval.recetaOiCil.isNotBlank()
        val hasAnyRxTable = showOd || showOi ||
            eval.recetaOdAv.isNotBlank() || eval.recetaOiAv.isNotBlank() ||
            eval.avCcOdLejos.isNotBlank() || eval.avCcOiLejos.isNotBlank() ||
            eval.avCcOdCerca.isNotBlank() || eval.avCcOiCerca.isNotBlank() ||
            eval.addCercaOd.isNotBlank() || eval.addCercaOi.isNotBlank() ||
            eval.addIntermediaOd.isNotBlank() || eval.addIntermediaOi.isNotBlank() ||
            eval.addAv.isNotBlank() ||
            eval.dipLejos.isNotBlank() || eval.dipIntermedio.isNotBlank() || eval.dipCerca.isNotBlank()

        if (!hasAnyRxTable) return this

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
            }
        }

        fun dash(s: String) = if (s.isBlank()) "—" else s

        val avCcOd = eval.avCcOdLejos.ifBlank { eval.recetaOdAv }
        val avCcOi = eval.avCcOiLejos.ifBlank { eval.recetaOiAv }

        val gridRows = buildList {
            add(RxGridRow.Head("Medida", "OD", "OI"))
            add(
                RxGridRow.Three(
                    "Fórmula (lejos)",
                    fmtRx(eval.recetaOdEsf, eval.recetaOdCil, eval.recetaOdEje),
                    fmtRx(eval.recetaOiEsf, eval.recetaOiCil, eval.recetaOiEje)
                )
            )
            add(RxGridRow.Three("AV CC", dash(avCcOd), dash(avCcOi)))
            add(RxGridRow.Merged("AV CC AO", dash(eval.avCcAoPx)))
            add(RxGridRow.Three("ADD", fmtAdd(true), fmtAdd(false)))
            add(RxGridRow.Three("AV cerca", dash(eval.avCcOdCerca), dash(eval.avCcOiCerca)))
            if (eval.addAv.isNotBlank()) {
                add(RxGridRow.Merged("AV con adición (referencia)", eval.addAv))
            }
            add(RxGridRow.Merged("DIP lejos (mm)", dash(eval.dipLejos)))
            add(RxGridRow.Merged("DIP intermedio (mm)", dash(eval.dipIntermedio)))
            add(RxGridRow.Merged("DIP cerca (mm)", dash(eval.dipCerca)))
        }

        val innerTabW = PdfStyle.PAGE_W - 2 * PdfStyle.MARGIN
        val labelColW = innerTabW * 0.30f
        val dataColW = (innerTabW - labelColW) / 2f
        val xLab = PdfStyle.MARGIN
        val xOd = xLab + labelColW
        val xOi = xOd + dataColW
        val xEnd = PdfStyle.MARGIN + innerTabW

        fun slCell(
            text: String,
            paint: TextPaint,
            w: Int,
            align: Layout.Alignment
        ) = StaticLayout.Builder.obtain(text, 0, text.length, paint, w.coerceAtLeast(8))
            .setAlignment(align)
            .setLineSpacing(0f, 1.18f)
            .setIncludePad(false)
            .build()

        fun rowHeight(row: RxGridRow): Float {
            val pad = 2 * PdfStyle.TABLE_CELL_PAD
            val wL = (labelColW - 2 * PdfStyle.TABLE_CELL_PAD).toInt().coerceAtLeast(8)
            val wD = (dataColW - 2 * PdfStyle.TABLE_CELL_PAD).toInt().coerceAtLeast(8)
            val wM = ((dataColW * 2) - 2 * PdfStyle.TABLE_CELL_PAD).toInt().coerceAtLeast(8)
            return when (row) {
                is RxGridRow.Head -> {
                    val a = slCell(row.l, PdfStyle.tableHeaderPaint, wL, Layout.Alignment.ALIGN_NORMAL)
                    val b = slCell(row.od, PdfStyle.tableHeaderPaint, wD, Layout.Alignment.ALIGN_CENTER)
                    val c = slCell(row.oi, PdfStyle.tableHeaderPaint, wD, Layout.Alignment.ALIGN_CENTER)
                    maxOf(a.height, b.height, c.height) + pad
                }
                is RxGridRow.Three -> {
                    val a = slCell(row.label, PdfStyle.labelPaint, wL, Layout.Alignment.ALIGN_NORMAL)
                    val b = slCell(row.od, PdfStyle.rxValuePaint, wD, Layout.Alignment.ALIGN_CENTER)
                    val c = slCell(row.oi, PdfStyle.rxValuePaint, wD, Layout.Alignment.ALIGN_CENTER)
                    maxOf(a.height, b.height, c.height) + pad
                }
                is RxGridRow.Merged -> {
                    val a = slCell(row.label, PdfStyle.labelPaint, wL, Layout.Alignment.ALIGN_NORMAL)
                    val b = slCell(row.value, PdfStyle.rxValuePaint, wM, Layout.Alignment.ALIGN_CENTER)
                    maxOf(a.height, b.height) + pad
                }
            }
        }

        val titleSl = layoutText("Prescripción y medidas", PdfStyle.sectionPaint, innerTabW.toInt())
        val titleBlockH = titleSl.height + 10f
        val rowHeights = gridRows.map { rowHeight(it) }
        val sumRows = rowHeights.sum()
        val tableOuterPad = 12f
        val totalBlockH = titleBlockH + sumRows + 2 * tableOuterPad + 4f

        ensureSpace(totalBlockH + 24f)

        val boxTop = y
        val boxRect = RectF(PdfStyle.MARGIN, boxTop, PdfStyle.PAGE_W - PdfStyle.MARGIN, boxTop + totalBlockH)
        canvas.drawRoundRect(boxRect, PdfStyle.CARD_RADIUS, PdfStyle.CARD_RADIUS, PdfStyle.cardFillPaint)
        canvas.drawRoundRect(boxRect, PdfStyle.CARD_RADIUS, PdfStyle.CARD_RADIUS, PdfStyle.cardStrokePaint)

        var ty = boxTop + tableOuterPad
        drawStaticLayout(titleSl, PdfStyle.MARGIN + tableOuterPad, ty)
        ty += titleBlockH

        val tableBodyTop = ty
        var rowY = ty
        gridRows.forEachIndexed { idx, row ->
            val rh = rowHeights[idx]
            when {
                row is RxGridRow.Head ->
                    canvas.drawRect(xLab, rowY, xEnd, rowY + rh, PdfStyle.headerStripPaint)
                idx > 0 && idx % 2 == 0 ->
                    canvas.drawRect(xLab, rowY, xEnd, rowY + rh, PdfStyle.altRowPaint)
            }
            val wL = (labelColW - 2 * PdfStyle.TABLE_CELL_PAD).toInt().coerceAtLeast(8)
            val wD = (dataColW - 2 * PdfStyle.TABLE_CELL_PAD).toInt().coerceAtLeast(8)
            val wM = ((dataColW * 2) - 2 * PdfStyle.TABLE_CELL_PAD).toInt().coerceAtLeast(8)
            when (row) {
                is RxGridRow.Head -> {
                    val slL = slCell(row.l, PdfStyle.tableHeaderPaint, wL, Layout.Alignment.ALIGN_NORMAL)
                    val slO = slCell(row.od, PdfStyle.tableHeaderPaint, wD, Layout.Alignment.ALIGN_CENTER)
                    val slI = slCell(row.oi, PdfStyle.tableHeaderPaint, wD, Layout.Alignment.ALIGN_CENTER)
                    val base = rowY + PdfStyle.TABLE_CELL_PAD
                    drawStaticLayout(slL, xLab + PdfStyle.TABLE_CELL_PAD, base)
                    drawStaticLayout(slO, xOd + PdfStyle.TABLE_CELL_PAD, base)
                    drawStaticLayout(slI, xOi + PdfStyle.TABLE_CELL_PAD, base)
                }
                is RxGridRow.Three -> {
                    val slL = slCell(row.label, PdfStyle.labelPaint, wL, Layout.Alignment.ALIGN_NORMAL)
                    val slO = slCell(row.od, PdfStyle.rxValuePaint, wD, Layout.Alignment.ALIGN_CENTER)
                    val slI = slCell(row.oi, PdfStyle.rxValuePaint, wD, Layout.Alignment.ALIGN_CENTER)
                    val base = rowY + PdfStyle.TABLE_CELL_PAD
                    drawStaticLayout(slL, xLab + PdfStyle.TABLE_CELL_PAD, base)
                    drawStaticLayout(slO, xOd + PdfStyle.TABLE_CELL_PAD, base)
                    drawStaticLayout(slI, xOi + PdfStyle.TABLE_CELL_PAD, base)
                }
                is RxGridRow.Merged -> {
                    val slL = slCell(row.label, PdfStyle.labelPaint, wL, Layout.Alignment.ALIGN_NORMAL)
                    val slV = slCell(row.value, PdfStyle.rxValuePaint, wM, Layout.Alignment.ALIGN_CENTER)
                    val base = rowY + PdfStyle.TABLE_CELL_PAD
                    drawStaticLayout(slL, xLab + PdfStyle.TABLE_CELL_PAD, base)
                    drawStaticLayout(slV, xOd + PdfStyle.TABLE_CELL_PAD, base)
                }
            }
            rowY += rh
        }

        // Grid lines
        canvas.drawLine(xLab, tableBodyTop, xEnd, tableBodyTop, PdfStyle.gridStrokePaint)
        var ly = tableBodyTop
        gridRows.forEachIndexed { idx, row ->
            val h = rowHeights[idx]
            canvas.drawLine(xLab, ly + h, xEnd, ly + h, PdfStyle.gridStrokePaint)
            canvas.drawLine(xLab, ly, xLab, ly + h, PdfStyle.gridStrokePaint)
            canvas.drawLine(xEnd, ly, xEnd, ly + h, PdfStyle.gridStrokePaint)
            canvas.drawLine(xOd, ly, xOd, ly + h, PdfStyle.gridStrokePaint)
            if (row is RxGridRow.Head || row is RxGridRow.Three) {
                canvas.drawLine(xOi, ly, xOi, ly + h, PdfStyle.gridStrokePaint)
            }
            ly += h
        }

        advance(totalBlockH + 18f)
        return this
    }

    /**
     * Dibuja la sección de diagnóstico y condiciones clínicas.
     */
    fun addDiagnostico(eval: EvaluacionClinica): RecetaPdfBuilder {
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
        return this
    }

    /**
     * Dibuja "Condiciones asociadas" a partir de flags booleanos.
     */
    fun addCondicionesAsociadas(eval: EvaluacionClinica): RecetaPdfBuilder {
        val cond = buildList {
            if (eval.otrosPresbicia) add("Presbicia")
            if (eval.otrosAnisometropia) add("Anisometropía")
            if (eval.otrosAmbliopia) add("Ambliopía")
        }.joinToString("\n")
        if (cond.isNotBlank()) section("Condiciones asociadas", cond)
        return this
    }

    /**
     * Dibuja "Prismas" a partir de los valores y bases OD/OI.
     */
    fun addPrismas(eval: EvaluacionClinica): RecetaPdfBuilder {
        val prisma = buildString {
            if (eval.prismaOdValor.isNotBlank()) appendLine("OD: ${eval.prismaOdValor} (base ${eval.prismaOdBase})")
            if (eval.prismaOiValor.isNotBlank()) appendLine("OI: ${eval.prismaOiValor} (base ${eval.prismaOiBase})")
        }
        if (prisma.isNotBlank()) section("Prismas", prisma)
        return this
    }

    /**
     * Dibuja "Queratometría" a partir de K1/K2 OD/OI.
     */
    fun addQueratometria(eval: EvaluacionClinica): RecetaPdfBuilder {
        val q = buildString {
            val ko = eval.k1Od.isNotBlank() || eval.k2Od.isNotBlank()
            val ki = eval.k1Oi.isNotBlank() || eval.k2Oi.isNotBlank()
            if (ko) appendLine("OD: ${eval.k1Od} / ${eval.k2Od}")
            if (ki) appendLine("OI: ${eval.k1Oi} / ${eval.k2Oi}")
        }
        if (q.isNotBlank()) section("Queratometría", q)
        return this
    }

    /**
     * Dibuja "Contactología" cuando hay datos de LC.
     */
    fun addContactologia(eval: EvaluacionClinica): RecetaPdfBuilder {
        if (eval.lcOdEsf.isNotBlank() || eval.lcOiEsf.isNotBlank()) {
            section(
                "Contactología",
                "Incluye datos de adaptación de lentes de contacto (evaluación completa en la aplicación)."
            )
        }
        return this
    }

    /**
     * Dibuja "Plan de tratamiento" cuando hay contenido.
     */
    fun addPlanTratamiento(eval: EvaluacionClinica): RecetaPdfBuilder {
        if (eval.planTratamiento.isNotBlank()) {
            section("Plan de tratamiento", eval.planTratamiento)
        }
        return this
    }

    /**
     * Dibuja "Observaciones" cuando hay contenido.
     */
    fun addObservaciones(eval: EvaluacionClinica): RecetaPdfBuilder {
        if (eval.observaciones.isNotBlank()) {
            section("Observaciones", eval.observaciones)
        }
        return this
    }

    /**
     * Dibuja "Seguimiento" con próxima cita y fecha de control.
     */
    fun addSeguimiento(eval: EvaluacionClinica): RecetaPdfBuilder {
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
        return this
    }

    /**
     * Dibuja una sección genérica con barra de acento lateral.
     */
    fun section(title: String, body: String) {
        if (body.isBlank()) return
        val innerW = contentWidth() - PdfStyle.SECTION_BAR_W.toInt() - 12
        val t = layoutText(title, PdfStyle.sectionPaint, innerW)
        val b = layoutText(body.trim(), PdfStyle.bodyPaint, innerW)
        val contentH = t.height + 8f + b.height
        val blockH = contentH + 16f
        if (!spaceAvailable(blockH)) newPage()

        val blockTop = y
        val barRect = RectF(
            PdfStyle.MARGIN, blockTop,
            PdfStyle.MARGIN + PdfStyle.SECTION_BAR_W, blockTop + contentH
        )
        canvas.drawRoundRect(barRect, 2f, 2f, PdfStyle.sectionBarPaint)

        drawStaticLayout(t, PdfStyle.MARGIN + PdfStyle.SECTION_BAR_W + 12f, blockTop)
        drawStaticLayout(b, PdfStyle.MARGIN + PdfStyle.SECTION_BAR_W + 12f, blockTop + t.height + 8f)
        advance(contentH + 18f)
    }

    /**
     * Finaliza el documento y retorna el [PdfDocument] con pie de página.
     */
    fun build(): PdfDocument {
        advance(8f)
        drawHorizontalRule()
        advance(14f)

        val footerText = "Documento generado con OptoApp · No sustituye la valoración clínica presencial ni la firma del profesional."
        val footerLayout = layoutText(
            footerText,
            PdfStyle.smallPaint,
            contentWidth(),
            Layout.Alignment.ALIGN_CENTER
        )
        val footerH = footerLayout.height + 8f
        if (!spaceAvailable(footerH)) newPage()
        drawStaticLayout(footerLayout, PdfStyle.MARGIN, y)
        advance(footerLayout.height.toFloat())

        finishPage()
        return doc
    }

    // ─── Internal helpers ───────────────────────────────────────────────────

    private fun initPage() {
        drawPageTopAccent()
        y = contentStartY()
    }

    private fun contentStartY(): Float = PdfStyle.ACCENT_BAR_H + 28f

    private fun drawPageTopAccent() {
        canvas.drawRect(0f, 0f, PdfStyle.PAGE_W.toFloat(), PdfStyle.ACCENT_BAR_H, PdfStyle.accentBarPaint)
    }

    private fun finishPage() {
        doc.finishPage(page)
    }

    private fun spaceAvailable(need: Float): Boolean =
        y + need <= PdfStyle.PAGE_H - PdfStyle.BOTTOM_SAFE

    private fun ensureSpace(need: Float) {
        if (!spaceAvailable(need)) newPage()
    }

    private fun newPage() {
        finishPage()
        page = doc.startPage(
            PdfDocument.PageInfo.Builder(PdfStyle.PAGE_W, PdfStyle.PAGE_H, ++pageNum).create()
        )
        canvas = page.canvas
        initPage()
    }

    private fun drawStaticLayout(sl: StaticLayout, x: Float, startY: Float) {
        canvas.withTranslation(x, startY) { sl.draw(this) }
    }

    private fun layoutText(
        text: String,
        paint: TextPaint,
        width: Int,
        align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
    ): StaticLayout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
        .setAlignment(align)
        .setLineSpacing(0f, 1.22f)
        .setIncludePad(false)
        .build()

    private fun advance(yDelta: Float) {
        y += yDelta
    }

    private fun drawHorizontalRule() {
        canvas.drawLine(PdfStyle.MARGIN, y, PdfStyle.PAGE_W - PdfStyle.MARGIN, y, PdfStyle.rulePaint)
    }

    private fun contentWidth(): Int = (PdfStyle.PAGE_W - 2 * PdfStyle.MARGIN).toInt()
}

/**
 * Filas de la cuadrícula de refracción dentro del PDF.
 */
internal sealed class RxGridRow {
    data class Head(val l: String, val od: String, val oi: String) : RxGridRow()
    data class Three(val label: String, val od: String, val oi: String) : RxGridRow()
    data class Merged(val label: String, val value: String) : RxGridRow()
}
