package com.example.optoapp.util

import android.graphics.Canvas
import android.graphics.Path
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.optoapp.data.EvaluacionClinica

/**
 * Draws the complete prescription table: distance vision, near vision, and prisms.
 * The rightmost column (AV/AO) spans the full height and the outer right border is continuous.
 */
object RefraccionTableBuilder {

    fun draw(
        eval: EvaluacionClinica,
        canvas: Canvas,
        yPos: Float,
        ensureSpace: (Float) -> Unit,
        drawSl: (StaticLayout, Float, Float) -> Unit,
        layoutText: (String, TextPaint, Int, Layout.Alignment) -> StaticLayout,
        advance: (Float) -> Unit
    ): Float {
        val hasRx = eval.recetaOdEsf.isNotBlank() || eval.recetaOdCil.isNotBlank() ||
            eval.recetaOiEsf.isNotBlank() || eval.recetaOiCil.isNotBlank()

        if (!hasRx) return yPos

        val margin = PdfStyle.MARGIN
        val innerW = PdfStyle.PAGE_W - 2 * margin
        val labelColW = 36f
        val dataW = innerW - labelColW

        // Column distribution: Esf(1) Cil(1) Eje(1) DIP/DNP(1) AV(1) AV/AO(2) = 7 units
        val unitW = dataW / 7f
        val colEsf = unitW
        val colCil = unitW
        val colEje = unitW
        val colDip = unitW
        val colAv  = unitW
        val colAvAo = unitW * 2f

        // X positions for distance columns
        val xLab = margin
        val xEsf = xLab + labelColW
        val xCil = xEsf + colEsf
        val xEje = xCil + colCil
        val xDip = xEje + colEje
        val xAv  = xDip + colDip
        val xAvAo = xAv + colAv
        val xRight = xAvAo + colAvAo  // outer right edge = full width

        fun sl(text: String, paint: TextPaint, w: Int, align: Layout.Alignment): StaticLayout =
            StaticLayout.Builder.obtain(text, 0, text.length, paint, w.coerceAtLeast(8))
                .setAlignment(align)
                .setLineSpacing(0f, 1.15f)
                .setIncludePad(false)
                .build()

        val headerPaint = PdfStyle.tableHeaderPaint
        val labelPaint = PdfStyle.labelPaint
        val valuePaint = PdfStyle.rxValuePaint
        val rowH = 28f
        val sectionTitleH = 22f
        val gap = 6f

        fun dash(s: String) = if (s.isBlank()) "—" else s

        data class Cell(val text: String, val align: Layout.Alignment)
        data class Row(val label: String, val cells: List<Cell>)

        // ── Distance Vision ─────────────────────────────────────────────────────
        val distRows = listOf(
            Row("OD", listOf(
                Cell(dash(eval.recetaOdEsf), Layout.Alignment.ALIGN_CENTER),
                Cell(dash(eval.recetaOdCil), Layout.Alignment.ALIGN_CENTER),
                Cell(dash(eval.recetaOdEje), Layout.Alignment.ALIGN_CENTER),
                Cell(dash(eval.dipLejos), Layout.Alignment.ALIGN_CENTER),
                Cell(dash(eval.recetaOdAv.ifBlank { eval.avCcOdLejos }), Layout.Alignment.ALIGN_CENTER),
                Cell(dash(eval.avCcAoPx), Layout.Alignment.ALIGN_CENTER)
            )),
            Row("OI", listOf(
                Cell(dash(eval.recetaOiEsf), Layout.Alignment.ALIGN_CENTER),
                Cell(dash(eval.recetaOiCil), Layout.Alignment.ALIGN_CENTER),
                Cell(dash(eval.recetaOiEje), Layout.Alignment.ALIGN_CENTER),
                Cell(dash(eval.dipCerca), Layout.Alignment.ALIGN_CENTER),
                Cell(dash(eval.recetaOiAv.ifBlank { eval.avCcOiLejos }), Layout.Alignment.ALIGN_CENTER),
                Cell(dash(eval.avCcAoPx), Layout.Alignment.ALIGN_CENTER)
            ))
        )

        // ── Near Vision ─────────────────────────────────────────────────────────
        val nearRows = listOf(
            Row("OD", listOf(
                Cell(dash(eval.addCercaOd), Layout.Alignment.ALIGN_CENTER),
                Cell(dash(eval.addIntermediaOd), Layout.Alignment.ALIGN_CENTER),
                Cell(dash(eval.dipCerca), Layout.Alignment.ALIGN_CENTER),
                Cell(dash(eval.avCcAoCerca.ifBlank { eval.avScAoCerca }), Layout.Alignment.ALIGN_CENTER)
            )),
            Row("OI", listOf(
                Cell(dash(eval.addCercaOi), Layout.Alignment.ALIGN_CENTER),
                Cell(dash(eval.addIntermediaOi), Layout.Alignment.ALIGN_CENTER),
                Cell(dash(eval.dipCerca), Layout.Alignment.ALIGN_CENTER),
                Cell(dash(eval.avCcAoCerca.ifBlank { eval.avScAoCerca }), Layout.Alignment.ALIGN_CENTER)
            ))
        )

        // ── Prismas ─────────────────────────────────────────────────────────────
        val prismaRows = listOf(
            Row("OD", listOf(
                Cell(dash(eval.prismaOdValor), Layout.Alignment.ALIGN_CENTER),
                Cell(dash(eval.prismaOdBase), Layout.Alignment.ALIGN_CENTER),
                Cell("", Layout.Alignment.ALIGN_CENTER)
            )),
            Row("OI", listOf(
                Cell(dash(eval.prismaOiValor), Layout.Alignment.ALIGN_CENTER),
                Cell(dash(eval.prismaOiBase), Layout.Alignment.ALIGN_CENTER),
                Cell("", Layout.Alignment.ALIGN_CENTER)
            ))
        )

        // ── Calculate heights ───────────────────────────────────────────────────
        val pad = 8f
        val distH = sectionTitleH + rowH + distRows.size * rowH
        val nearH = sectionTitleH + rowH + nearRows.size * rowH
        // Prisma solo 4/7 de ancho, calculated later
        val prismaBodyH = rowH + prismaRows.size * rowH
        val totalH = pad + distH + gap + nearH + gap + sectionTitleH + prismaBodyH + pad + 8f

        ensureSpace(totalH + 16f)

        var cy = yPos + pad

        // Helper: draw a divider line across full width
        fun fullDivider(yy: Float) {
            canvas.drawLine(margin, yy, xRight, yy, PdfStyle.gridStrokePaint)
        }

        // ─── 1. Visión Lejana ─────────────────────────────────────────────────
        val distTitleSl = sl("Visión Lejana", PdfStyle.sectionPaint, innerW.toInt(), Layout.Alignment.ALIGN_NORMAL)
        drawSl(distTitleSl, margin, cy)
        cy += sectionTitleH

        val tableTop = cy

        // Header row with background
        canvas.drawRect(margin, cy, xRight, cy + rowH, PdfStyle.headerStripPaint)
        val distHeaders = listOf("", "Esfera", "Cilindro", "Eje", "DIP/DNP", "AV", "AV / AO")
        val distXPos = listOf(xLab, xEsf, xCil, xEje, xDip, xAv, xAvAo)
        val distWidths = listOf(labelColW, colEsf, colCil, colEje, colDip, colAv, colAvAo)
        distHeaders.forEachIndexed { i, h ->
            val hs = sl(h, headerPaint, (distWidths[i] - 4).toInt().coerceAtLeast(8), Layout.Alignment.ALIGN_CENTER)
            drawSl(hs, distXPos[i] + 2, cy + (rowH - hs.height) / 2f)
        }
        cy += rowH

        // Distance data rows
        distRows.forEachIndexed { ri, row ->
            val bgPaint = if (ri > 0 && ri % 2 == 0) PdfStyle.altRowPaint else null
            if (bgPaint != null) canvas.drawRect(margin, cy, xRight, cy + rowH, bgPaint)
            val labelSl = sl(row.label, labelPaint, (labelColW - 4).toInt().coerceAtLeast(8), Layout.Alignment.ALIGN_CENTER)
            drawSl(labelSl, xLab + 2, cy + (rowH - labelSl.height) / 2f)
            val dataX = listOf(xEsf, xCil, xEje, xDip, xAv, xAvAo)
            val dataWList = listOf(colEsf, colCil, colEje, colDip, colAv, colAvAo)
            row.cells.forEachIndexed { ci, cell ->
                val cw = (dataWList[ci] - 4).toInt().coerceAtLeast(8)
                val cellSl = sl(cell.text, valuePaint, cw, cell.align)
                drawSl(cellSl, dataX[ci] + 2, cy + (rowH - cellSl.height) / 2f)
            }
            cy += rowH
        }

        // Distance grid
        fun drawGridLines(topY: Float, rows: Int, xs: List<Float>) {
            val endX = xs.last()
            var ly = topY
            for (i in 0 until rows) {
                canvas.drawLine(xs[0], ly, endX, ly, PdfStyle.gridStrokePaint)
                xs.forEach { x -> canvas.drawLine(x, ly, x, ly + rowH, PdfStyle.gridStrokePaint) }
                // Rightmost border: AV/AO column right edge (xRight)
                canvas.drawLine(xRight, ly, xRight, ly + rowH, PdfStyle.gridStrokePaint)
                ly += rowH
            }
        }

        drawGridLines(tableTop, 1 + distRows.size, listOf(margin, xEsf, xCil, xEje, xDip, xAv, xAvAo, xRight))

        // Full-width separator after distance
        cy += gap
        fullDivider(cy)
        cy += gap

        // ─── 2. Visión Próxima ─────────────────────────────────────────────────
        // Columns: label | Cerca | Intermedia | DIP/DNP | AV / AO (solo ambos ojos)
        // Cerca e Intermedia reemplazan a Esfera, Cilindro, Eje (ocupan sus mismas posiciones)
        // DIP/DNP mantiene misma posición; AV/AO ocupa el espacio de AV + AV/AO combinado
        val nearColCercaInter = (colEsf + colCil + colEje) / 2f  // dividir espacio de 3 cols en 2
        val nearColAvAo = colAv + colAvAo  // AV/AO combinado ocupa AV + AV/AO

        val nearTitleSl = sl("Visión Próxima", PdfStyle.sectionPaint, innerW.toInt(), Layout.Alignment.ALIGN_NORMAL)
        drawSl(nearTitleSl, margin, cy)
        cy += sectionTitleH

        val nearTableTop = cy

        // Header
        canvas.drawRect(margin, cy, xRight, cy + rowH, PdfStyle.headerStripPaint)
        val nearHeaders = listOf("", "Cerca", "Intermedia", "DIP/DNP", "AV / AO")
        val nearXPos = listOf(xLab, xEsf, xEsf + nearColCercaInter, xDip, xAv)
        val nearWidths = listOf(labelColW, nearColCercaInter, nearColCercaInter, colDip, nearColAvAo)
        nearHeaders.forEachIndexed { i, h ->
            val hs = sl(h, headerPaint, (nearWidths[i] - 4).toInt().coerceAtLeast(8), Layout.Alignment.ALIGN_CENTER)
            drawSl(hs, nearXPos[i] + 2, cy + (rowH - hs.height) / 2f)
        }
        cy += rowH

        // Near data rows
        nearRows.forEachIndexed { ri, row ->
            val bgPaint = if (ri > 0 && ri % 2 == 0) PdfStyle.altRowPaint else null
            if (bgPaint != null) canvas.drawRect(margin, cy, xRight, cy + rowH, bgPaint)
            val labelSl = sl(row.label, labelPaint, (labelColW - 4).toInt().coerceAtLeast(8), Layout.Alignment.ALIGN_CENTER)
            drawSl(labelSl, xLab + 2, cy + (rowH - labelSl.height) / 2f)
            val nearDataX = listOf(xEsf, xEsf + nearColCercaInter, xDip, xAv)
            val nearDataW = listOf(nearColCercaInter, nearColCercaInter, colDip, nearColAvAo)
            row.cells.forEachIndexed { ci, cell ->
                val cw = (nearDataW[ci] - 4).toInt().coerceAtLeast(8)
                val cellSl = sl(cell.text, valuePaint, cw, cell.align)
                drawSl(cellSl, nearDataX[ci] + 2, cy + (rowH - cellSl.height) / 2f)
            }
            cy += rowH
        }

        // Near grid lines – 5 columns
        var nly = nearTableTop
        val nearTotalRows = 1 + nearRows.size
        for (i in 0 until nearTotalRows) {
            canvas.drawLine(margin, nly, xRight, nly, PdfStyle.gridStrokePaint)
            canvas.drawLine(margin, nly, margin, nly + rowH, PdfStyle.gridStrokePaint)
            canvas.drawLine(xRight, nly, xRight, nly + rowH, PdfStyle.gridStrokePaint)
            canvas.drawLine(xEsf, nly, xEsf, nly + rowH, PdfStyle.gridStrokePaint)
            canvas.drawLine(xEsf + nearColCercaInter, nly, xEsf + nearColCercaInter, nly + rowH, PdfStyle.gridStrokePaint)
            canvas.drawLine(xDip, nly, xDip, nly + rowH, PdfStyle.gridStrokePaint)
            nly += rowH
        }

        // Full-width separator after near
        cy += gap
        fullDivider(cy)
        cy += 4f

        // ─── 3. Prismas ─────────────────────────────────────────────────────────
        // Prisma solo ocupa 4 de las 7 columnas de ancho
        val oneSeventh = innerW / 7f
        val prismaColW = oneSeventh
        val pLabel = margin
        val pPD = pLabel + prismaColW
        val pBase = pPD + prismaColW
        val pEje = pBase + prismaColW
        val pRight = pEje + prismaColW  // right edge of prisma box (4/7)
        val prismaTitleSl = sl("Prisma", PdfStyle.sectionPaint, innerW.toInt(), Layout.Alignment.ALIGN_NORMAL)
        drawSl(prismaTitleSl, margin + 4f, cy)
        cy += sectionTitleH

        val prismaTableTop = cy

        // Draw triangle icon
        val triSize = 16f
        val triX = pLabel + 4f
        val triY = cy + 4f
        val triangle = Path().apply {
            moveTo(triX, triY)
            lineTo(triX + triSize / 2f, triY + triSize)
            lineTo(triX - triSize / 2f, triY + triSize)
            close()
        }
        canvas.drawPath(triangle, PdfStyle.prismaTrianglePaint)

        // Header
        canvas.drawRect(pLabel, cy, pRight, cy + rowH, PdfStyle.headerStripPaint)
        val prismaHeaders = listOf("", "PD", "Base", "Eje")
        val prismaXPos = listOf(pLabel, pPD, pBase, pEje)
        prismaHeaders.forEachIndexed { i, h ->
            val hs = sl(h, headerPaint, (prismaColW - 4).toInt().coerceAtLeast(8), Layout.Alignment.ALIGN_CENTER)
            drawSl(hs, prismaXPos[i] + 2, cy + (rowH - hs.height) / 2f)
        }
        cy += rowH

        // Prisma data rows
        prismaRows.forEachIndexed { ri, row ->
            val bgPaint = if (ri > 0 && ri % 2 == 0) PdfStyle.altRowPaint else null
            if (bgPaint != null) canvas.drawRect(pLabel, cy, pRight, cy + rowH, bgPaint)
            val labelSl = sl(row.label, labelPaint, (prismaColW - 4).toInt().coerceAtLeast(8), Layout.Alignment.ALIGN_CENTER)
            drawSl(labelSl, pLabel + 2, cy + (rowH - labelSl.height) / 2f)
            row.cells.forEachIndexed { ci, cell ->
                val cx = pPD + ci * prismaColW
                val cw = (prismaColW - 4).toInt().coerceAtLeast(8)
                val cellSl = sl(cell.text, valuePaint, cw, cell.align)
                drawSl(cellSl, cx + 2, cy + (rowH - cellSl.height) / 2f)
            }
            cy += rowH
        }

        // Prisma grid lines – only within 4/7 width; outer right border continues
        var ply = prismaTableTop
        val prismaTotalRows = 1 + prismaRows.size
        for (i in 0 until prismaTotalRows) {
            canvas.drawLine(pLabel, ply, pRight, ply, PdfStyle.gridStrokePaint)
            canvas.drawLine(pLabel, ply, pLabel, ply + rowH, PdfStyle.gridStrokePaint)
            // Prisma right edge at 4/7
            canvas.drawLine(pRight, ply, pRight, ply + rowH, PdfStyle.gridStrokePaint)
            canvas.drawLine(pPD, ply, pPD, ply + rowH, PdfStyle.gridStrokePaint)
            canvas.drawLine(pBase, ply, pBase, ply + rowH, PdfStyle.gridStrokePaint)
            canvas.drawLine(pEje, ply, pEje, ply + rowH, PdfStyle.gridStrokePaint)
            ply += rowH
        }

        // Close the outer right border at the bottom of prisma (full width)
        canvas.drawLine(xRight, prismaTableTop, xRight, cy, PdfStyle.gridStrokePaint)

        cy += pad

        val finalY = cy
        advance(finalY - yPos)
        return finalY
    }
}
