package com.example.optoapp.util

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.optoapp.data.Montura
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object InventarioMonturasPdfGenerator {
    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 32f
    private const val ROW_H = 18f

    fun generate(context: Context, monturas: List<Montura>): File {
        val dir = File(context.cacheDir, "recetas").apply { mkdirs() }
        val file = File(dir, "reporte-inventario-${System.currentTimeMillis()}.pdf")

        val doc = PdfDocument()
        val titlePaint = Paint().apply {
            textSize = 15f
            isFakeBoldText = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
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
            canvas.drawText("Reporte de Inventario General", MARGIN, y, titlePaint)
            y += 18f
            val total = monturas.size
            val activas = monturas.count { it.activo }
            val criticas = monturas.count { it.activo && it.stockActual <= it.stockMinimo }
            val stock = monturas.sumOf { it.stockActual }
            val valorCosto = monturas.sumOf { it.stockActual * it.costo }
            val valorPrecio = monturas.sumOf { it.stockActual * it.precio }
            canvas.drawText(
                "Total: $total  |  Activas: $activas  |  Críticas: $criticas  |  Stock: $stock",
                MARGIN,
                y,
                textPaint,
            )
            y += 14f
            canvas.drawText(
                "Valor costo: S/ ${valorCosto.toMoney()}  |  Valor venta: S/ ${valorPrecio.toMoney()}",
                MARGIN,
                y,
                textPaint,
            )
            y += 16f
            canvas.drawText("SKU", MARGIN, y, headerPaint)
            canvas.drawText("Marca / Producto", MARGIN + 85f, y, headerPaint)
            canvas.drawText("Color/Talla", MARGIN + 290f, y, headerPaint)
            canvas.drawText("Stock", MARGIN + 410f, y, headerPaint)
            canvas.drawText("Precio", MARGIN + 465f, y, headerPaint)
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

        val rows = monturas.sortedWith(
            compareByDescending<Montura> { it.activo && it.stockActual <= it.stockMinimo }
                .thenBy { it.marca.lowercase(Locale.getDefault()) }
                .thenBy { it.modelo.lowercase(Locale.getDefault()) },
        )
        rows.forEach { m ->
            if (y + ROW_H > PAGE_H - MARGIN) newPage()
            val stockLabel = if (m.activo && m.stockActual <= m.stockMinimo) "${m.stockActual} !" else "${m.stockActual}"
            canvas.drawText(m.sku.take(16), MARGIN, y, textPaint)
            canvas.drawText("${m.marca} ${m.modelo}".take(34), MARGIN + 85f, y, textPaint)
            canvas.drawText("${m.color.ifBlank { "-" }}/${m.talla.ifBlank { "-" }}".take(20), MARGIN + 290f, y, textPaint)
            canvas.drawText(stockLabel, MARGIN + 410f, y, textPaint)
            canvas.drawText("S/ ${m.precio.toMoney()}", MARGIN + 465f, y, textPaint)
            y += ROW_H
        }

        doc.finishPage(page)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun Double.toMoney(): String = String.format(Locale.forLanguageTag("es-PE"), "%.2f", this)
}
