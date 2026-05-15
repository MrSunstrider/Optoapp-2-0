package com.example.optoapp.util

import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.optoapp.data.EvaluacionClinica

/**
 * Draws the complete prescription table: distance vision, near vision, and prisms.
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

        val pageW = PdfStyle.PAGE_W
        val margin = PdfStyle.MARGIN
        val innerW = pageW - 2 * margin
        val colW = innerW / 6f  // 6 columns: Esf, Cil, Eje, DIP/DNP, AV, AV/AO

        val x0 = margin
        val x1 = x0 + colW
        val x2 = x1 + colW
        val x3 = x2 + colW
        val x4 = x3 + colW
        val x5 = x4 + colW
        val x6 = x5 + colW

        val labelColW = 36f
        val dataStartX = x0 + labelColW
        val dataW = innerW - labelColW
        val dColW = dataW / 6f

        val dx0 = dataStartX
        val dx1 = dx0 + dColW
        val dx2 = dx1 + dColW
        val dx3 = dx2 + dColW
        val dx4 = dx3 + dColW
        val dx5 = dx4 + dColW
        val dx6 = dx5 + dColW

        fun sl(text: String, paint: TextPaint, w: Int, align: Layout.Alignment): StaticLayout =
            StaticLayout.Builder.obtain(text, 0, text.length, paint, w.coerceAtLeast(8))
                .setAlignment(align)
                .setLineSpacing(0f, 1.15f)
                .setIncludePad(false)
                .build()

        data class Cell(val text: String, val align: Layout.Alignment)
        data class Row(val label: String, val cells: List<Cell>)

        val rowH = 28f
        val headerPaint = PdfStyle.tableHeaderPaint
        val labelPaint = PdfStyle.labelPaint
        val valuePaint = PdfStyle.rxValuePaint

        fun dash(s: String) = if (s.isBlank()) "—" else s

        // ── Section 1: Visión Lejana ──────────────────────────────────────────
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

        // ── Section 2: Visión Próxima ─────────────────────────────────────────
        val nearRows = listOf(
            Row("OD", listOf(
                Cell(dash(eval.addCercaOd), Layout.Alignment.ALIGN_CENTER),
                Cell(dash(eval.addIntermediaOd), Layout.Alignment.ALIGN_CENTER)
            )),
            Row("OI", listOf(
                Cell(dash(eval.addCercaOi), Layout.Alignment.ALIGN_CENTER),
                Cell(dash(eval.addIntermediaOi), Layout.Alignment.ALIGN_CENTER)
            ))
        )

        // ── Section 3: Prismas ────────────────────────────────────────────────
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

        // ── Calculate total height ─────────────────────────────────────────────
        val sectionTitleH = 24f
        val distHeaderH = rowH
        val distBodyH = rowH * distRows.size
        val nearHeaderH = rowH
        val nearBodyH = rowH * nearRows.size
        val prismaHeaderH = rowH
        val prismaBodyH = rowH * prismaRows.size

        val gap = 8f
        val pad = 10f

        val distBlockH = sectionTitleH + distHeaderH + distBodyH + gap
        val nearBlockH = sectionTitleH + nearHeaderH + nearBodyH + gap
        val prismaBlockH = sectionTitleH + prismaHeaderH + prismaBodyH + gap

        val totalH = pad + distBlockH + nearBlockH + prismaBlockH + pad

        val sectionTPaint = PdfStyle.sectionPaint
        val sectionW = innerW.toInt()

        ensureSpace(totalH + 20f)

        var cy = yPos + pad

        // ─── Helper to draw a section ─────────────────────────────────────────
        fun drawSection(
            title: String,
            headerLabels: List<String>,
            rows: List<Row>,
            startX: Float,
            colWs: List<Float>,
            extraCellCount: Int = 0,
            drawExtra: ((Float, Float, Float) -> Unit)? = null
        ) {
            // Section title
            val titleSl = sl(title, sectionTPaint, sectionW, Layout.Alignment.ALIGN_NORMAL)
            drawSl(titleSl, margin, cy)
            cy += sectionTitleH

            val tableTop = cy
            val hCount = headerLabels.size
            val totalColW = colWs.sum()
            val endX = startX + totalColW

            // Header row background
            canvas.drawRect(startX, cy, endX, cy + rowH, PdfStyle.headerStripPaint)

            // Header cells
            var hx = startX
            headerLabels.forEachIndexed { i, h ->
                val hs = sl(h, headerPaint, (colWs[i] - 4).toInt().coerceAtLeast(8), Layout.Alignment.ALIGN_CENTER)
                drawSl(hs, hx + 2, cy + (rowH - hs.height) / 2f)
                hx += colWs[i]
            }
            cy += rowH

            // Data rows
            rows.forEachIndexed { ri, row ->
                val rh = rowH
                if (ri > 0 && ri % 2 == 0) {
                    canvas.drawRect(startX, cy, endX, cy + rh, PdfStyle.altRowPaint)
                }
                // Label cell
                val labelSl = sl(row.label, labelPaint, (colWs[0] - 4).toInt().coerceAtLeast(8), Layout.Alignment.ALIGN_CENTER)
                drawSl(labelSl, startX + 2, cy + (rh - labelSl.height) / 2f)

                // Data cells
                var cx = startX + colWs[0]
                row.cells.forEachIndexed { ci, cell ->
                    val cellSl = sl(cell.text, valuePaint, (colWs[ci + 1] - 4).toInt().coerceAtLeast(8), cell.align)
                    drawSl(cellSl, cx + 2, cy + (rh - cellSl.height) / 2f)
                    cx += colWs[ci + 1]
                }

                // Draw extra cells if any (e.g. for prism split)
                if (extraCellCount > 0 && drawExtra != null) {
                    drawExtra(startX, cy, rh)
                }

                cy += rh
            }

            // Grid lines
            canvas.drawLine(startX, tableTop, endX, tableTop, PdfStyle.gridStrokePaint)
            var ly = tableTop
            val totalRows = 1 + rows.size // header + data
            for (i in 0 until totalRows) {
                canvas.drawLine(startX, ly, endX, ly, PdfStyle.gridStrokePaint)
                canvas.drawLine(startX, ly, startX, ly + rowH, PdfStyle.gridStrokePaint)
                canvas.drawLine(endX, ly, endX, ly + rowH, PdfStyle.gridStrokePaint)
                // Vertical dividers
                var vx = startX
                for (w in colWs) {
                    vx += w
                    canvas.drawLine(vx, ly, vx, ly + rowH, PdfStyle.gridStrokePaint)
                }
                ly += rowH
            }
        }

        // ── 1. Visión Lejana ──────────────────────────────────────────────────
        drawSection(
            title = "Visión Lejana",
            headerLabels = listOf("", "Esfera", "Cilindro", "Eje", "DIP/DNP", "AV", "AV / AO"),
            rows = distRows,
            startX = dataStartX - labelColW,
            colWs = listOf(labelColW, dColW, dColW, dColW, dColW, dColW, dColW)
        )
        cy += gap

        // ── 2. Visión Próxima ─────────────────────────────────────────────────
        val nearColW = (dataW + labelColW) / 3f  // 3 columns: label, Cerca, Intermedia
        val nearX0 = margin
        val nearX1 = nearX0 + nearColW
        val nearX2 = nearX1 + nearColW

        // Section title
        val nearTitleSl = sl("Visión Próxima", sectionTPaint, sectionW, Layout.Alignment.ALIGN_NORMAL)
        drawSl(nearTitleSl, margin, cy)
        cy += sectionTitleH

        val nearTableTop = cy
        val nearEndX = nearX0 + nearColW * 3

        // Header
        canvas.drawRect(nearX0, cy, nearEndX, cy + rowH, PdfStyle.headerStripPaint)
        val nearHeaders = listOf("", "Cerca", "Intermedia")
        var nhx = nearX0
        nearHeaders.forEach { h ->
            val hs = sl(h, headerPaint, (nearColW - 4).toInt().coerceAtLeast(8), Layout.Alignment.ALIGN_CENTER)
            drawSl(hs, nhx + 2, cy + (rowH - hs.height) / 2f)
            nhx += nearColW
        }
        cy += rowH

        // Data rows
        nearRows.forEachIndexed { ri, row ->
            val rh = rowH
            if (ri > 0 && ri % 2 == 0) {
                canvas.drawRect(nearX0, cy, nearEndX, cy + rh, PdfStyle.altRowPaint)
            }
            val labelSl = sl(row.label, labelPaint, (nearColW - 4).toInt().coerceAtLeast(8), Layout.Alignment.ALIGN_CENTER)
            drawSl(labelSl, nearX0 + 2, cy + (rh - labelSl.height) / 2f)

            row.cells.forEachIndexed { ci, cell ->
                val cx = nearX1 + ci * nearColW
                val cellSl = sl(cell.text, valuePaint, (nearColW - 4).toInt().coerceAtLeast(8), cell.align)
                drawSl(cellSl, cx + 2, cy + (rh - cellSl.height) / 2f)
            }
            cy += rh
        }

        // Near grid lines
        canvas.drawLine(nearX0, nearTableTop, nearEndX, nearTableTop, PdfStyle.gridStrokePaint)
        var nly = nearTableTop
        val nearTotalRows = 1 + nearRows.size
        for (i in 0 until nearTotalRows) {
            canvas.drawLine(nearX0, nly, nearEndX, nly, PdfStyle.gridStrokePaint)
            canvas.drawLine(nearX0, nly, nearX0, nly + rowH, PdfStyle.gridStrokePaint)
            canvas.drawLine(nearEndX, nly, nearEndX, nly + rowH, PdfStyle.gridStrokePaint)
            canvas.drawLine(nearX1, nly, nearX1, nly + rowH, PdfStyle.gridStrokePaint)
            canvas.drawLine(nearX2, nly, nearX2, nly + rowH, PdfStyle.gridStrokePaint)
            nly += rowH
        }

        cy += gap

        // ── 3. Prismas ─────────────────────────────────────────────────────────
        // Prisma solo ocupa 4 de las 7 columnas de ancho
        val oneSeventh = innerW / 7f
        val prismaWidth = oneSeventh * 4f
        val prismaColW = oneSeventh  // 4 columnas iguales
        val pX0 = margin
        val pX1 = pX0 + prismaColW
        val pX2 = pX1 + prismaColW
        val pX3 = pX2 + prismaColW
        val pEndX = pX0 + prismaWidth

        // Separador horizontal superior (ancho completo) + inicio del bloque
        canvas.drawLine(margin, cy, margin + innerW, cy, PdfStyle.gridStrokePaint)

        // Triangle in top-left of prisma section
        val triSize = 16f
        val triX = pX0 + 6f
        val triY = cy + 6f
        val triangle = Path().apply {
            moveTo(triX, triY)                         // top vertex
            lineTo(triX + triSize / 2f, triY + triSize) // bottom-right
            lineTo(triX - triSize / 2f, triY + triSize) // bottom-left
            close()
        }
        canvas.drawPath(triangle, PdfStyle.prismaTrianglePaint)

        // Section title
        val pTitleSl = sl("Prisma", sectionTPaint, sectionW, Layout.Alignment.ALIGN_NORMAL)
        drawSl(pTitleSl, margin + 28f, cy)
        cy += sectionTitleH

        val prismaTableTop = cy

        // Header
        canvas.drawRect(pX0, cy, pEndX, cy + rowH, PdfStyle.headerStripPaint)
        val prismaHeaders = listOf("", "PD", "Base", "Eje")
        var phx = pX0
        prismaHeaders.forEach { h ->
            val hs = sl(h, headerPaint, (prismaColW - 4).toInt().coerceAtLeast(8), Layout.Alignment.ALIGN_CENTER)
            drawSl(hs, phx + 2, cy + (rowH - hs.height) / 2f)
            phx += prismaColW
        }
        cy += rowH

        // Data rows
        prismaRows.forEachIndexed { ri, row ->
            val rh = rowH
            if (ri > 0 && ri % 2 == 0) {
                canvas.drawRect(pX0, cy, pEndX, cy + rh, PdfStyle.altRowPaint)
            }
            val labelSl = sl(row.label, labelPaint, (prismaColW - 4).toInt().coerceAtLeast(8), Layout.Alignment.ALIGN_CENTER)
            drawSl(labelSl, pX0 + 2, cy + (rh - labelSl.height) / 2f)

            row.cells.forEachIndexed { ci, cell ->
                val cx = pX1 + ci * prismaColW
                val cellSl = sl(cell.text, valuePaint, (prismaColW - 4).toInt().coerceAtLeast(8), cell.align)
                drawSl(cellSl, cx + 2, cy + (rh - cellSl.height) / 2f)
            }
            cy += rh
        }

        // Prisma grid lines — solo 4/7 del ancho
        canvas.drawLine(pX0, prismaTableTop, pEndX, prismaTableTop, PdfStyle.gridStrokePaint)
        var ply = prismaTableTop
        val prismaTotalRows = 1 + prismaRows.size
        for (i in 0 until prismaTotalRows) {
            canvas.drawLine(pX0, ply, pEndX, ply, PdfStyle.gridStrokePaint)
            canvas.drawLine(pX0, ply, pX0, ply + rowH, PdfStyle.gridStrokePaint)
            canvas.drawLine(pEndX, ply, pEndX, ply + rowH, PdfStyle.gridStrokePaint)
            canvas.drawLine(pX1, ply, pX1, ply + rowH, PdfStyle.gridStrokePaint)
            canvas.drawLine(pX2, ply, pX2, ply + rowH, PdfStyle.gridStrokePaint)
            canvas.drawLine(pX3, ply, pX3, ply + rowH, PdfStyle.gridStrokePaint)
            ply += rowH
        }

        cy += pad

        val finalY = cy
        advance(finalY - yPos)
        return finalY
    }
}
