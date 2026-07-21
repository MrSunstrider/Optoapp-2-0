package com.example.optoapp.util

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.ServicioExtra
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object ReporteFinancieroPdfGenerator {
    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 32f
    private const val ROW_H = 18f

    fun generate(
        context: Context,
        dispensaciones: List<DispensacionOptica>,
        serviciosExtra: List<ServicioExtra> = emptyList(),
        periodo: String,
        totalVendido: Double,
        porCobrar: Double,
        ticketPromedio: Double,
        pagosSumByDispensacion: Map<String, Double> = emptyMap(),
        aCuentaSumByServicio: Map<String, Double> = emptyMap(),
    ): File {
        val dir = File(context.cacheDir, "reportes").apply { mkdirs() }
        val file = File(dir, "reporte-financiero-${System.currentTimeMillis()}.pdf")

        val doc = PdfDocument()
        val titlePaint = Paint().apply {
            textSize = 16f
            isFakeBoldText = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint().apply {
            textSize = 11f
            isFakeBoldText = true
        }
        val textPaint = Paint().apply { textSize = 10f }
        val headerPaint = Paint().apply {
            textSize = 10f
            isFakeBoldText = true
        }

        var pageNum = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
        var canvas = page.canvas
        var y = MARGIN

        fun drawHeader() {
            canvas.drawText("Reporte Financiero", MARGIN, y, titlePaint)
            y += 16f
            val labelFecha = when (periodo) {
                "Diario" -> "Día de hoy"
                "Semanal" -> "Esta semana"
                "Mensual" -> "Mensual"
                "Anual" -> "Anual"
                else -> "Total"
            }
            canvas.drawText("Período: $labelFecha", MARGIN, y, subtitlePaint)
            y += 14f
            canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, textPaint)
            y += 10f

            canvas.drawText("Total Vendido: S/ ${totalVendido.toMoney()}", MARGIN, y, subtitlePaint)
            y += 13f
            canvas.drawText("Por Cobrar:    S/ ${porCobrar.toMoney()}", MARGIN, y, subtitlePaint)
            y += 13f
            canvas.drawText("Ticket Promedio: S/ ${ticketPromedio.toMoney()}", MARGIN, y, subtitlePaint)
            y += 16f

            canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, textPaint)
            y += 10f

            canvas.drawText("Fecha", MARGIN, y, headerPaint)
            canvas.drawText("Pago", MARGIN + 120f, y, headerPaint)
            canvas.drawText("Total", MARGIN + 340f, y, headerPaint)
            canvas.drawText("Saldo", MARGIN + 450f, y, headerPaint)
            y += 8f
            canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, textPaint)
            y += 12f
        }

        fun newPage() {
            doc.finishPage(page)
            pageNum += 1
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
            canvas = page.canvas
            y = MARGIN
            drawHeader()
        }

        drawHeader()

        dispensaciones.forEach { disp ->
            if (y + ROW_H > PAGE_H - MARGIN) newPage()
            val date = DateUtils.formatLocalized(disp.fecha)
            val montoPagado = pagosSumByDispensacion[disp.id] ?: 0.0
            val saldo = disp.montoTotal - montoPagado
            canvas.drawText(date.take(12), MARGIN, y, textPaint)
            canvas.drawText(disp.metodoPago.take(20), MARGIN + 120f, y, textPaint)
            canvas.drawText("S/ ${disp.montoTotal.toMoney()}", MARGIN + 340f, y, textPaint)
            canvas.drawText("S/ ${saldo.toMoney()}", MARGIN + 450f, y, textPaint)
            y += ROW_H
        }

        if (serviciosExtra.isNotEmpty()) {
            if (y + ROW_H > PAGE_H - MARGIN) newPage()
            y += 6f
            canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, textPaint)
            y += 10f
            canvas.drawText("SERVICIOS EXTRA", MARGIN, y, subtitlePaint)
            y += 14f
            canvas.drawText("Descripción", MARGIN, y, headerPaint)
            canvas.drawText("Monto", MARGIN + 340f, y, headerPaint)
            canvas.drawText("Pagado", MARGIN + 450f, y, headerPaint)
            y += 8f
            canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, textPaint)
            y += 12f

            serviciosExtra.forEach { serv ->
                if (y + ROW_H > PAGE_H - MARGIN) newPage()
                val aCuenta = aCuentaSumByServicio[serv.id] ?: 0.0
                canvas.drawText(serv.descripcion.take(28), MARGIN, y, textPaint)
                canvas.drawText("S/ ${serv.montoTotal.toMoney()}", MARGIN + 340f, y, textPaint)
                canvas.drawText("S/ ${aCuenta.toMoney()}", MARGIN + 450f, y, textPaint)
                y += ROW_H
            }
        }

        doc.finishPage(page)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun Double.toMoney(): String = String.format(Locale.forLanguageTag("es-PE"), "%.2f", this)
}
