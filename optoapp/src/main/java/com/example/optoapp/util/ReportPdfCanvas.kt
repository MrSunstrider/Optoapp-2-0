package com.example.optoapp.util

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint

data class ReportPdfKpi(val label: String, val value: String)

data class ReportPdfColumn(val title: String, val x: Float, val width: Float)

class ReportPdfCanvas {
    private val doc = PdfDocument()
    private var pageNum = 0
    private lateinit var page: PdfDocument.Page
    private lateinit var canvas: Canvas
    private var y = 0f
    private var documentTitle = ""

    val contentWidth: Float
        get() = ReportPdfTheme.PAGE_W - (ReportPdfTheme.MARGIN * 2)

    fun currentY(): Float = y

    fun start(title: String, subtitle: String) {
        documentTitle = title
        newPageInternal(drawBandTitle = false)
        drawHeaderBand(title, subtitle)
        y = ReportPdfTheme.HEADER_BAND_H + 18f
    }

    fun drawKpis(items: List<ReportPdfKpi>) {
        if (items.isEmpty()) return
        val gap = 8f
        val count = items.size.coerceAtMost(4)
        val cellW = (contentWidth - gap * (count - 1)) / count
        val cellH = 42f
        ensureSpace(cellH + 14f)
        items.take(count).forEachIndexed { index, kpi ->
            val left = ReportPdfTheme.MARGIN + index * (cellW + gap)
            val rect = RectF(left, y, left + cellW, y + cellH)
            canvas.drawRoundRect(rect, ReportPdfTheme.KPI_RADIUS, ReportPdfTheme.KPI_RADIUS, ReportPdfTheme.kpiFillPaint)
            canvas.drawRoundRect(rect, ReportPdfTheme.KPI_RADIUS, ReportPdfTheme.KPI_RADIUS, ReportPdfTheme.kpiStrokePaint)
            canvas.drawText(kpi.label, left + 10f, y + 14f, ReportPdfTheme.kpiLabelPaint)
            canvas.drawText(kpi.value.take(22), left + 10f, y + 30f, ReportPdfTheme.kpiValuePaint)
        }
        y += cellH + 14f
    }

    fun drawSectionTitle(title: String) {
        ensureSpace(22f)
        val barTop = y
        canvas.drawRect(
            ReportPdfTheme.MARGIN,
            barTop,
            ReportPdfTheme.MARGIN + 3f,
            barTop + 14f,
            ReportPdfTheme.accentPaint,
        )
        canvas.drawText(title, ReportPdfTheme.MARGIN + 10f, barTop + 11f, ReportPdfTheme.sectionPaint)
        y += 20f
    }

    fun drawMutedLine(text: String) {
        ensureSpace(ReportPdfTheme.ROW_H)
        canvas.drawText(text.take(90), ReportPdfTheme.MARGIN, y + 11f, ReportPdfTheme.mutedPaint)
        y += ReportPdfTheme.ROW_H
    }

    fun drawBodyLine(text: String, warn: Boolean = false) {
        val paint = if (warn) ReportPdfTheme.bodyWarnPaint else ReportPdfTheme.bodyPaint
        wrapText(text, paint, contentWidth).forEach { line ->
            ensureSpace(ReportPdfTheme.ROW_H)
            canvas.drawText(line, ReportPdfTheme.MARGIN, y + 11f, paint)
            y += ReportPdfTheme.ROW_H
        }
    }

    fun drawTableHeader(columns: List<ReportPdfColumn>) {
        ensureSpace(ReportPdfTheme.TABLE_HEADER_H + 4f)
        val top = y
        val bottom = y + ReportPdfTheme.TABLE_HEADER_H
        canvas.drawRect(
            ReportPdfTheme.MARGIN,
            top,
            ReportPdfTheme.PAGE_W - ReportPdfTheme.MARGIN,
            bottom,
            ReportPdfTheme.tableHeaderFillPaint,
        )
        columns.forEach { col ->
            canvas.drawText(col.title, col.x, top + 13f, ReportPdfTheme.tableHeaderPaint)
        }
        y = bottom + 2f
    }

    fun drawTableRow(
        columns: List<ReportPdfColumn>,
        values: List<String>,
        rowIndex: Int,
        warn: Boolean = false,
    ) {
        ensureSpace(ReportPdfTheme.ROW_H + 2f)
        val top = y
        val bottom = y + ReportPdfTheme.ROW_H
        if (rowIndex % 2 == 1) {
            canvas.drawRect(
                ReportPdfTheme.MARGIN,
                top,
                ReportPdfTheme.PAGE_W - ReportPdfTheme.MARGIN,
                bottom,
                ReportPdfTheme.zebraFillPaint,
            )
        }
        val paint = if (warn) ReportPdfTheme.bodyWarnPaint else ReportPdfTheme.bodyPaint
        columns.forEachIndexed { i, col ->
            val raw = values.getOrElse(i) { "" }
            val maxChars = (col.width / 5.2f).toInt().coerceAtLeast(4)
            canvas.drawText(raw.take(maxChars), col.x, top + 12f, paint)
        }
        y = bottom
    }

    fun ensureSpace(need: Float) {
        if (y + need > ReportPdfTheme.PAGE_H - ReportPdfTheme.BOTTOM_SAFE) {
            newPageInternal(drawBandTitle = true)
        }
    }

    fun finish(): PdfDocument {
        finishCurrentPage()
        return doc
    }

    private fun newPageInternal(drawBandTitle: Boolean) {
        if (pageNum > 0) finishCurrentPage()
        pageNum += 1
        page = doc.startPage(
            PdfDocument.PageInfo.Builder(ReportPdfTheme.PAGE_W, ReportPdfTheme.PAGE_H, pageNum).create(),
        )
        canvas = page.canvas
        if (drawBandTitle) {
            drawContinuationBand()
            y = ReportPdfTheme.HEADER_BAND_H + 14f
        } else {
            y = ReportPdfTheme.MARGIN
        }
    }

    private fun drawHeaderBand(title: String, subtitle: String) {
        canvas.drawRect(0f, 0f, ReportPdfTheme.PAGE_W.toFloat(), ReportPdfTheme.HEADER_BAND_H, ReportPdfTheme.headerBandPaint)
        canvas.drawRect(0f, ReportPdfTheme.HEADER_BAND_H - 3f, ReportPdfTheme.PAGE_W.toFloat(), ReportPdfTheme.HEADER_BAND_H, ReportPdfTheme.accentPaint)
        canvas.drawText(title, ReportPdfTheme.MARGIN, 26f, ReportPdfTheme.headerTitlePaint)
        if (subtitle.isNotBlank()) {
            canvas.drawText(subtitle, ReportPdfTheme.MARGIN, 44f, ReportPdfTheme.headerSubtitlePaint)
        }
    }

    private fun drawContinuationBand() {
        val label = if (documentTitle.isBlank()) "Continuación" else "$documentTitle (cont.)"
        val bandH = ReportPdfTheme.HEADER_BAND_H * 0.65f
        canvas.drawRect(0f, 0f, ReportPdfTheme.PAGE_W.toFloat(), bandH, ReportPdfTheme.headerBandPaint)
        val paint = TextPaint(ReportPdfTheme.headerTitlePaint).apply {
            textSize = 13f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText(label, ReportPdfTheme.MARGIN, 24f, paint)
    }

    private fun finishCurrentPage() {
        val footerY = ReportPdfTheme.PAGE_H - 22f
        canvas.drawLine(
            ReportPdfTheme.MARGIN,
            footerY - 8f,
            ReportPdfTheme.PAGE_W - ReportPdfTheme.MARGIN,
            footerY - 8f,
            ReportPdfTheme.rulePaint,
        )
        canvas.drawText("OptoApp", ReportPdfTheme.MARGIN, footerY, ReportPdfTheme.footerPaint)
        val pageLabel = "Página $pageNum"
        val pageW = ReportPdfTheme.footerPaint.measureText(pageLabel)
        canvas.drawText(
            pageLabel,
            ReportPdfTheme.PAGE_W - ReportPdfTheme.MARGIN - pageW,
            footerY,
            ReportPdfTheme.footerPaint,
        )
        doc.finishPage(page)
    }

    companion object {
        fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
            if (text.isEmpty()) return listOf("")
            if (paint.measureText(text) <= maxWidth) return listOf(text)
            val lines = mutableListOf<String>()
            var remaining = text
            while (remaining.isNotEmpty()) {
                if (paint.measureText(remaining) <= maxWidth) {
                    lines += remaining
                    break
                }
                var end = 1
                var low = 1
                var high = remaining.length
                while (low <= high) {
                    val mid = (low + high) ushr 1
                    if (paint.measureText(remaining, 0, mid) <= maxWidth) {
                        end = mid
                        low = mid + 1
                    } else {
                        high = mid - 1
                    }
                }
                val breakAt = remaining.lastIndexOf(' ', end).takeIf { it > 0 } ?: end.coerceAtLeast(1)
                lines += remaining.substring(0, breakAt).trimEnd()
                remaining = remaining.substring(breakAt).trimStart()
            }
            return lines.ifEmpty { listOf(text) }
        }
    }
}
