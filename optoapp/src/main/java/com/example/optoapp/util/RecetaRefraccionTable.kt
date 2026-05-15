package com.example.optoapp.util

import android.graphics.Canvas
import android.graphics.Path
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.optoapp.data.EvaluacionClinica

/**
 * Draws the complete prescription table: distance vision, near vision, and prisms.
 * VL / VP / triangle labels. All titles centered. Prisma box closes at bottom.
 */
object RefraccionTableBuilder {

    data class Cell(val text: String, val align: Layout.Alignment = Layout.Alignment.ALIGN_CENTER)
    data class Row(val label: String, val cells: List<Cell>)

    data class TableData(
        val hasRx: Boolean,
        val distRows: List<Row>,
        val nearRows: List<Row>,
        val prismaRows: List<Row>
    )

    fun prepareData(eval: EvaluacionClinica): TableData {
        val hasRx = eval.recetaOdEsf.isNotBlank() || eval.recetaOdCil.isNotBlank() ||
            eval.recetaOiEsf.isNotBlank() || eval.recetaOiCil.isNotBlank()

        fun dash(s: String) = if (s.isBlank()) "—" else s

        val distRows = listOf(
            Row("OD", listOf(
                Cell(dash(eval.recetaOdEsf)),
                Cell(dash(eval.recetaOdCil)),
                Cell(dash(eval.recetaOdEje)),
                Cell(dash(eval.dipLejos)),
                Cell(dash(eval.recetaOdAv.ifBlank { eval.avCcOdLejos })),
                Cell(dash(eval.avCcAoPx))
            )),
            Row("OI", listOf(
                Cell(dash(eval.recetaOiEsf)),
                Cell(dash(eval.recetaOiCil)),
                Cell(dash(eval.recetaOiEje)),
                Cell(dash(eval.dipCerca)),
                Cell(dash(eval.recetaOiAv.ifBlank { eval.avCcOiLejos })),
                Cell(dash(eval.avCcAoPx))
            ))
        )

        val nearRows = listOf(
            Row("OD", listOf(
                Cell(dash(eval.addCercaOd)),
                Cell(dash(eval.addIntermediaOd)),
                Cell(dash(eval.dipCerca)),
                Cell(dash(eval.avCcAoCerca.ifBlank { eval.avScAoCerca }))
            )),
            Row("OI", listOf(
                Cell(dash(eval.addCercaOi)),
                Cell(dash(eval.addIntermediaOi)),
                Cell(dash(eval.dipCerca)),
                Cell(dash(eval.avCcAoCerca.ifBlank { eval.avScAoCerca }))
            ))
        )

        val prismaRows = listOf(
            Row("OD", listOf(
                Cell(dash(eval.prismaOdValor)),
                Cell(dash(eval.prismaOdBase)),
                Cell("—")
            )),
            Row("OI", listOf(
                Cell(dash(eval.prismaOiValor)),
                Cell(dash(eval.prismaOiBase)),
                Cell("—")
            ))
        )

        return TableData(hasRx = hasRx, distRows = distRows, nearRows = nearRows, prismaRows = prismaRows)
    }

    fun draw(
        eval: EvaluacionClinica,
        canvas: Canvas,
        yPos: Float,
        ensureSpace: (Float) -> Unit,
        drawSl: (StaticLayout, Float, Float) -> Unit,
        layoutText: (String, TextPaint, Int, Layout.Alignment) -> StaticLayout,
        advance: (Float) -> Unit
    ): Float {
        val data = prepareData(eval)
        if (!data.hasRx) return yPos

        val distRows = data.distRows
        val nearRows = data.nearRows
        val prismaRows = data.prismaRows

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

        val xLab = margin
        val xEsf = xLab + labelColW
        val xCil = xEsf + colEsf
        val xEje = xCil + colCil
        val xDip = xEje + colEje
        val xAv  = xDip + colDip
        val xAvAo = xAv + colAv
        val xRight = xAvAo + colAvAo

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
        val sectionTitleH = 20f
        val gap = 6f

        val pad = 8f
        val distH = sectionTitleH + rowH + distRows.size * rowH
        val nearH = sectionTitleH + rowH + nearRows.size * rowH
        val prismaBodyH = rowH + prismaRows.size * rowH
        val totalH = pad + distH + gap + nearH + gap + sectionTitleH + prismaBodyH + pad + 8f

        ensureSpace(totalH + 16f)

        var cy = yPos + pad

        fun fullDivider(yy: Float) {
            canvas.drawLine(margin, yy, xRight, yy, PdfStyle.gridStrokePaint)
        }

        // ─── 1. Visión Lejana ─────────────────────────────────────────────────
        val distTitleSl = sl("Visión Lejana", PdfStyle.sectionPaint, innerW.toInt(), Layout.Alignment.ALIGN_CENTER)
        drawSl(distTitleSl, margin, cy)
        cy += sectionTitleH

        val distTableTop = cy

        // Header: VL in first cell
        canvas.drawRect(margin, cy, xRight, cy + rowH, PdfStyle.headerStripPaint)
        val distHeaders = listOf("VL", "Esfera", "Cilindro", "Eje", "DIP/DNP", "AV", "AV / AO")
        val distXPos = listOf(xLab, xEsf, xCil, xEje, xDip, xAv, xAvAo)
        val distWidths = listOf(labelColW, colEsf, colCil, colEje, colDip, colAv, colAvAo)
        distHeaders.forEachIndexed { i, h ->
            val hs = sl(h, headerPaint, (distWidths[i] - 4).toInt().coerceAtLeast(8), Layout.Alignment.ALIGN_CENTER)
            drawSl(hs, distXPos[i] + 2, cy + (rowH - hs.height) / 2f)
        }
        cy += rowH

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
                canvas.drawLine(xRight, ly, xRight, ly + rowH, PdfStyle.gridStrokePaint)
                ly += rowH
            }
        }
        drawGridLines(distTableTop, 1 + distRows.size, listOf(margin, xEsf, xCil, xEje, xDip, xAv, xAvAo, xRight))

        cy += gap
        fullDivider(cy)
        cy += gap

        // ─── 2. Visión Próxima ─────────────────────────────────────────────────
        // 6 cols: label | Cerca | Intermedia | DIP/DNP | AV | AV/AO
        val nearColCercaInter = (colEsf + colCil + colEje) / 2f

        val nearTitleSl = sl("Visión Próxima", PdfStyle.sectionPaint, innerW.toInt(), Layout.Alignment.ALIGN_CENTER)
        drawSl(nearTitleSl, margin, cy)
        cy += sectionTitleH

        val nearTableTop = cy

        canvas.drawRect(margin, cy, xRight, cy + rowH, PdfStyle.headerStripPaint)
        val nearHeaders = listOf("VP", "Cerca", "Intermedia", "DIP/DNP", "AV", "AV / AO")
        val nearXPos = listOf(xLab, xEsf, xEsf + nearColCercaInter, xDip, xAv, xAvAo)
        val nearWidths = listOf(labelColW, nearColCercaInter, nearColCercaInter, colDip, colAv, colAvAo)
        nearHeaders.forEachIndexed { i, h ->
            val hs = sl(h, headerPaint, (nearWidths[i] - 4).toInt().coerceAtLeast(8), Layout.Alignment.ALIGN_CENTER)
            drawSl(hs, nearXPos[i] + 2, cy + (rowH - hs.height) / 2f)
        }
        cy += rowH

        nearRows.forEachIndexed { ri, row ->
            val bgPaint = if (ri > 0 && ri % 2 == 0) PdfStyle.altRowPaint else null
            if (bgPaint != null) canvas.drawRect(margin, cy, xRight, cy + rowH, bgPaint)
            val labelSl = sl(row.label, labelPaint, (labelColW - 4).toInt().coerceAtLeast(8), Layout.Alignment.ALIGN_CENTER)
            drawSl(labelSl, xLab + 2, cy + (rowH - labelSl.height) / 2f)
            val nearDataX = listOf(xEsf, xEsf + nearColCercaInter, xDip, xAv, xAvAo)
            val nearDataW = listOf(nearColCercaInter, nearColCercaInter, colDip, colAv, colAvAo)
            row.cells.forEachIndexed { ci, cell ->
                val cw = (nearDataW[ci] - 4).toInt().coerceAtLeast(8)
                val cellSl = sl(cell.text, valuePaint, cw, cell.align)
                drawSl(cellSl, nearDataX[ci] + 2, cy + (rowH - cellSl.height) / 2f)
            }
            cy += rowH
        }

        // Near grid – 6 columns with DIP/DNP / AV separator
        var nly = nearTableTop
        val nearTotalRows = 1 + nearRows.size
        for (i in 0 until nearTotalRows) {
            canvas.drawLine(margin, nly, xRight, nly, PdfStyle.gridStrokePaint)
            canvas.drawLine(margin, nly, margin, nly + rowH, PdfStyle.gridStrokePaint)
            canvas.drawLine(xRight, nly, xRight, nly + rowH, PdfStyle.gridStrokePaint)
            canvas.drawLine(xEsf, nly, xEsf, nly + rowH, PdfStyle.gridStrokePaint)
            canvas.drawLine(xEsf + nearColCercaInter, nly, xEsf + nearColCercaInter, nly + rowH, PdfStyle.gridStrokePaint)
            canvas.drawLine(xDip, nly, xDip, nly + rowH, PdfStyle.gridStrokePaint)
            canvas.drawLine(xAv, nly, xAv, nly + rowH, PdfStyle.gridStrokePaint)
            nly += rowH
        }

        cy += gap
        fullDivider(cy)
        cy += 4f

        // ─── 3. Prismas ─────────────────────────────────────────────────────────
        val oneSeventh = innerW / 7f
        val prismaColW = oneSeventh
        val pLabel = margin
        val pPD = pLabel + prismaColW
        val pBase = pPD + prismaColW
        val pEje = pBase + prismaColW
        val pRight = pEje + prismaColW

        val prismaTitleSl = sl("Prisma", PdfStyle.sectionPaint, (pRight - margin).toInt(), Layout.Alignment.ALIGN_CENTER)
        drawSl(prismaTitleSl, margin, cy)
        cy += sectionTitleH

        val prismaTableTop = cy

        // Triangle centered in label cell
        val triSize = 18f
        val triCX = pLabel + labelColW / 2f
        val triCY = cy + rowH / 2f
        val triangle = Path().apply {
            moveTo(triCX, triCY - triSize / 2f)
            lineTo(triCX + triSize / 2f, triCY + triSize / 2f)
            lineTo(triCX - triSize / 2f, triCY + triSize / 2f)
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

        // Prisma grid lines + bottom close
        var ply = prismaTableTop
        val prismaTotalRows = 1 + prismaRows.size
        for (i in 0 until prismaTotalRows) {
            canvas.drawLine(pLabel, ply, pRight, ply, PdfStyle.gridStrokePaint)
            canvas.drawLine(pLabel, ply, pLabel, ply + rowH, PdfStyle.gridStrokePaint)
            canvas.drawLine(pRight, ply, pRight, ply + rowH, PdfStyle.gridStrokePaint)
            canvas.drawLine(pPD, ply, pPD, ply + rowH, PdfStyle.gridStrokePaint)
            canvas.drawLine(pBase, ply, pBase, ply + rowH, PdfStyle.gridStrokePaint)
            canvas.drawLine(pEje, ply, pEje, ply + rowH, PdfStyle.gridStrokePaint)
            ply += rowH
        }
        // Bottom closing line for prisma box
        canvas.drawLine(pLabel, cy, pRight, cy, PdfStyle.gridStrokePaint)

        // Outer right border continuous
        canvas.drawLine(xRight, distTableTop, xRight, cy, PdfStyle.gridStrokePaint)

        cy += pad

        val finalY = cy
        advance(finalY - yPos)
        return finalY
    }
}
