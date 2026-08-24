package com.example.optoapp.util

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.optoapp.domain.PagoEffect
import com.example.optoapp.viewmodel.CierreCajaUiState
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * Day-close PDF from already-aggregated Cierre totals.
 * Callers MUST pass PagoEffect-derived [cobradoHoy] / [totalesPorMetodo] (not raw sum(monto)).
 */
object CierreCajaPdfGenerator {
    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 32f
    private const val ROW_H = 16f

    fun dayCloseLines(
        state: CierreCajaUiState,
        cobradoHoy: Double,
        totalesPorMetodo: Map<String, Double>,
        contado: Double? = null,
    ): List<String> {
        val lines = mutableListOf<String>()
        lines += "Cierre de Caja"
        lines += "Fecha: ${state.fecha}"
        lines += "Cobrado: S/ ${money(cobradoHoy)}"
        lines += "Ventas del día: S/ ${money(state.ventasHoy)}"
        lines += "Cobros atrasados: S/ ${money(state.cobrosAtrasados)}"
        lines += "Ventas registradas: S/ ${money(state.totalGeneral)}"
        lines += "Saldo pendiente: S/ ${money(state.saldoPendiente)}"
        lines += "Métodos de pago"
        totalesPorMetodo.entries.sortedBy { it.key }.forEach { (metodo, monto) ->
            val label = metodo.ifBlank { "Sin espec." }
            lines += "$label: S/ ${money(monto)}"
        }
        val efectivoNet = totalesPorMetodo["Efectivo"] ?: 0.0
        if (contado != null) {
            lines += "Contado: S/ ${money(contado)}"
            lines += "Diferencia: S/ ${money(contado - efectivoNet)}"
        }
        if (state.pagosDisplay.isNotEmpty()) {
            lines += "Cobros recibidos (${state.pagosDisplay.size})"
            state.pagosDisplay.forEach { item ->
                lines += "${item.label} | ${item.pago.tipo} | ${item.pago.metodoPago} | S/ ${money(PagoEffect.signedAmount(item.pago.tipo, item.pago.monto))}"
            }
        }
        return lines
    }

    fun generate(
        context: Context,
        state: CierreCajaUiState,
        cobradoHoy: Double,
        totalesPorMetodo: Map<String, Double>,
        contado: Double? = null,
    ): File {
        val dir = File(context.cacheDir, "cierre_caja").apply { mkdirs() }
        val file = File(dir, "cierre-${state.fecha}-${System.currentTimeMillis()}.pdf")
        val lines = dayCloseLines(state, cobradoHoy, totalesPorMetodo, contado)

        val doc = PdfDocument()
        val titlePaint = Paint().apply {
            textSize = 16f
            isFakeBoldText = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textPaint = Paint().apply { textSize = 10f }
        val boldPaint = Paint().apply {
            textSize = 11f
            isFakeBoldText = true
        }

        var pageNum = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
        var canvas = page.canvas
        var y = MARGIN

        fun newPage() {
            doc.finishPage(page)
            pageNum += 1
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
            canvas = page.canvas
            y = MARGIN
        }

        lines.forEachIndexed { index, line ->
            if (y + ROW_H > PAGE_H - MARGIN) newPage()
            val paint = when {
                index == 0 -> titlePaint
                line == "Métodos de pago" || line.startsWith("Cobros recibidos") -> boldPaint
                else -> textPaint
            }
            canvas.drawText(line.take(90), MARGIN, y, paint)
            y += if (index == 0) 20f else ROW_H
        }

        doc.finishPage(page)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun money(value: Double): String =
        String.format(Locale.US, "%.2f", value)
}
