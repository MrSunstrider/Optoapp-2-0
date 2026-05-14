package com.example.optoapp.util

import android.graphics.Canvas
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.optoapp.data.EvaluacionClinica

// ── Grid row types ────────────────────────────────────────────────────────────

/**
 * Filas de la cuadrícula de refracción dentro del PDF.
 */
internal sealed class RxGridRow {
    data class Head(val l: String, val od: String, val oi: String) : RxGridRow()
    data class Three(val label: String, val od: String, val oi: String) : RxGridRow()
    data class Merged(val label: String, val value: String) : RxGridRow()
}

// ── Builder ───────────────────────────────────────────────────────────────────

/**
 * Builds and draws the refraction/prescription grid table onto a PDF canvas.
 *
 * Extracted from [RecetaPdfBuilder.addRefraccion] to reduce file size.
 * Call [draw] with the evaluation data and a rendering context.
 */
object RefraccionTableBuilder {

    /**
     * Draws the refraction grid. Returns `true` if any content was drawn.
     *
     * @param eval clinical evaluation with refraction data
     * @param canvas target canvas
     * @param yPos current Y position (will be advanced)
     * @param ensureSpace callback that checks/creates space on the page
     * @param drawSl callback to draw a StaticLayout at (x, y)
     * @param layoutText callback to create a StaticLayout from text+paint
     * @param advance callback to advance Y by a delta
     * @return new Y position after drawing
     */
    fun draw(
        eval: EvaluacionClinica,
        canvas: Canvas,
        yPos: Float,
        ensureSpace: (Float) -> Unit,
        drawSl: (StaticLayout, Float, Float) -> Unit,
        layoutText: (String, TextPaint, Int, Layout.Alignment) -> StaticLayout,
        advance: (Float) -> Unit
    ): Float {
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

        if (!hasAnyRxTable) return yPos

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

        val titleSl = layoutText("Prescripción y medidas", PdfStyle.sectionPaint, innerTabW.toInt(), Layout.Alignment.ALIGN_NORMAL)
        val titleBlockH = titleSl.height + 10f
        val rowHeights = gridRows.map { rowHeight(it) }
        val sumRows = rowHeights.sum()
        val tableOuterPad = 12f
        val totalBlockH = titleBlockH + sumRows + 2 * tableOuterPad + 4f

        ensureSpace(totalBlockH + 24f)

        val boxTop = yPos
        val boxRect = RectF(PdfStyle.MARGIN, boxTop, PdfStyle.PAGE_W - PdfStyle.MARGIN, boxTop + totalBlockH)
        canvas.drawRoundRect(boxRect, PdfStyle.CARD_RADIUS, PdfStyle.CARD_RADIUS, PdfStyle.cardFillPaint)
        canvas.drawRoundRect(boxRect, PdfStyle.CARD_RADIUS, PdfStyle.CARD_RADIUS, PdfStyle.cardStrokePaint)

        var ty = boxTop + tableOuterPad
        drawSl(titleSl, PdfStyle.MARGIN + tableOuterPad, ty)
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
                    drawSl(slL, xLab + PdfStyle.TABLE_CELL_PAD, base)
                    drawSl(slO, xOd + PdfStyle.TABLE_CELL_PAD, base)
                    drawSl(slI, xOi + PdfStyle.TABLE_CELL_PAD, base)
                }
                is RxGridRow.Three -> {
                    val slL = slCell(row.label, PdfStyle.labelPaint, wL, Layout.Alignment.ALIGN_NORMAL)
                    val slO = slCell(row.od, PdfStyle.rxValuePaint, wD, Layout.Alignment.ALIGN_CENTER)
                    val slI = slCell(row.oi, PdfStyle.rxValuePaint, wD, Layout.Alignment.ALIGN_CENTER)
                    val base = rowY + PdfStyle.TABLE_CELL_PAD
                    drawSl(slL, xLab + PdfStyle.TABLE_CELL_PAD, base)
                    drawSl(slO, xOd + PdfStyle.TABLE_CELL_PAD, base)
                    drawSl(slI, xOi + PdfStyle.TABLE_CELL_PAD, base)
                }
                is RxGridRow.Merged -> {
                    val slL = slCell(row.label, PdfStyle.labelPaint, wL, Layout.Alignment.ALIGN_NORMAL)
                    val slV = slCell(row.value, PdfStyle.rxValuePaint, wM, Layout.Alignment.ALIGN_CENTER)
                    val base = rowY + PdfStyle.TABLE_CELL_PAD
                    drawSl(slL, xLab + PdfStyle.TABLE_CELL_PAD, base)
                    drawSl(slV, xOd + PdfStyle.TABLE_CELL_PAD, base)
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
        return boxTop + totalBlockH + 18f
    }
}
