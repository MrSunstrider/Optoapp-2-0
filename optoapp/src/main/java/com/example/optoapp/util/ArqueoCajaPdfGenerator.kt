package com.example.optoapp.util

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.optoapp.data.arqueo.ArqueoCaja

/**
 * Generates a simple PDF report for an arqueo de caja record.
 * Uses android.graphics.pdf.PdfDocument for compatibility.
 */
object ArqueoCajaPdfGenerator {

    private const val PAGE_WIDTH = 595  // A4 width in points
    private const val PAGE_HEIGHT = 842 // A4 height in points
    private const val MARGIN = 40f

    fun generate(arqueo: ArqueoCaja, opticaName: String): ByteArray {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
        }
        val subtitlePaint = Paint().apply {
            textSize = 14f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply { textSize = 11f }
        val headerPaint = Paint().apply {
            textSize = 12f
            isFakeBoldText = true
        }

        var y = MARGIN

        // Title
        canvas.drawText("ARQUEO DE CAJA", MARGIN, y, titlePaint)
        y += 30f

        // Optica + date
        canvas.drawText("Óptica: $opticaName", MARGIN, y, bodyPaint)
        y += 18f
        canvas.drawText("Fecha: ${arqueo.fecha}", MARGIN, y, bodyPaint)
        y += 18f
        canvas.drawText("Cerrado por: ${arqueo.cerradoPor}", MARGIN, y, bodyPaint)
        y += 18f
        val selladoText = if (arqueo.sellado) "Estado: SELLADO" else "Estado: PENDIENTE"
        canvas.drawText(selladoText, MARGIN, y, bodyPaint)
        y += 30f

        // Fondo de caja
        canvas.drawText("Fondo de Caja: s/. ${"%.2f".format(arqueo.fondoCaja)}", MARGIN, y, subtitlePaint)
        y += 28f

        // Per-method table header
        canvas.drawText("Método", MARGIN, y, headerPaint)
        canvas.drawText("Contado", MARGIN + 150f, y, headerPaint)
        canvas.drawText("Cobrado", MARGIN + 270f, y, headerPaint)
        canvas.drawText("Diferencia", MARGIN + 390f, y, headerPaint)
        y += 6f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, bodyPaint)
        y += 14f

        // Table rows
        drawMethodRow(canvas, y, "Efectivo", arqueo.efectivoContado, arqueo.efectivoCobrado); y += 18f
        drawMethodRow(canvas, y, "Tarjeta", arqueo.tarjetaContado, arqueo.tarjetaCobrado); y += 18f
        drawMethodRow(canvas, y, "Transferencia", arqueo.transferenciaContado, arqueo.transferenciaCobrado); y += 18f
        drawMethodRow(canvas, y, "Móvil", arqueo.movilContado, arqueo.movilCobrado); y += 18f

        // Total row
        y += 6f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, bodyPaint)
        y += 14f
        val totalContado = arqueo.efectivoContado + arqueo.tarjetaContado +
                arqueo.transferenciaContado + arqueo.movilContado
        val totalCobrado = arqueo.efectivoCobrado + arqueo.tarjetaCobrado +
                arqueo.transferenciaCobrado + arqueo.movilCobrado
        drawMethodRow(canvas, y, "TOTAL", totalContado, totalCobrado)
        y += 24f

        // Diferencia total
        canvas.drawText(
            "Diferencia Total: s/. ${"%.2f".format(arqueo.diferenciaTotal)}",
            MARGIN, y, subtitlePaint
        )

        document.finishPage(page)

        val output = java.io.ByteArrayOutputStream()
        document.writeTo(output)
        document.close()

        return output.toByteArray()
    }

    private fun drawMethodRow(
        canvas: Canvas,
        y: Float,
        method: String,
        contado: Double,
        cobrado: Double
    ) {
        val paint = Paint().apply { textSize = 11f }
        val diff = contado - cobrado
        canvas.drawText(method, MARGIN, y, paint)
        canvas.drawText("s/. ${"%.2f".format(contado)}", MARGIN + 150f, y, paint)
        canvas.drawText("s/. ${"%.2f".format(cobrado)}", MARGIN + 270f, y, paint)
        canvas.drawText("s/. ${"%.2f".format(diff)}", MARGIN + 390f, y, paint)
    }
}
