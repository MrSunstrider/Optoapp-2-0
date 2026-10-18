package com.example.optoapp.util

import android.content.Context
import com.example.optoapp.data.Montura
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object InventarioMonturasPdfGenerator {

    fun generate(context: Context, monturas: List<Montura>): File {
        val dir = File(context.cacheDir, "recetas").apply { mkdirs() }
        val file = File(dir, "reporte-inventario-${System.currentTimeMillis()}.pdf")

        val total = monturas.size
        val activas = monturas.count { it.activo }
        val criticas = monturas.count { it.activo && it.stockActual <= it.stockMinimo }
        val stock = monturas.sumOf { it.stockActual }
        val valorCosto = monturas.sumOf { it.stockActual * it.costo }
        val valorPrecio = monturas.sumOf { it.stockActual * it.precio }

        val pdf = ReportPdfCanvas()
        pdf.start("Inventario de monturas", "Snapshot de catálogo y stock")
        pdf.drawKpis(
            listOf(
                ReportPdfKpi("Modelos", total.toString()),
                ReportPdfKpi("Activas", activas.toString()),
                ReportPdfKpi("Críticas", criticas.toString()),
                ReportPdfKpi("Stock u.", stock.toString()),
            ),
        )
        pdf.drawMutedLine("Valor costo: S/ ${valorCosto.toMoney()}  ·  Valor venta: S/ ${valorPrecio.toMoney()}")

        val cols = listOf(
            ReportPdfColumn("SKU", ReportPdfTheme.MARGIN, 72f),
            ReportPdfColumn("Marca / producto", ReportPdfTheme.MARGIN + 76f, 175f),
            ReportPdfColumn("Color/Talla", ReportPdfTheme.MARGIN + 258f, 90f),
            ReportPdfColumn("Stock", ReportPdfTheme.MARGIN + 356f, 42f),
            ReportPdfColumn("Precio", ReportPdfTheme.MARGIN + 406f, 70f),
        )
        pdf.drawSectionTitle("Detalle")
        if (monturas.isEmpty()) {
            pdf.drawMutedLine("Sin monturas en inventario")
        } else {
            pdf.drawTableHeader(cols)
            val rows = monturas.sortedWith(
                compareByDescending<Montura> { it.activo && it.stockActual <= it.stockMinimo }
                    .thenBy { it.marca.lowercase(Locale.getDefault()) }
                    .thenBy { it.modelo.lowercase(Locale.getDefault()) },
            )
            rows.forEachIndexed { index, m ->
                val critical = m.activo && m.stockActual <= m.stockMinimo
                val stockLabel = if (critical) "${m.stockActual} !" else "${m.stockActual}"
                pdf.drawTableRow(
                    cols,
                    listOf(
                        m.sku,
                        "${m.marca} ${m.modelo}".trim(),
                        "${m.color.ifBlank { "-" }}/${m.talla.ifBlank { "-" }}",
                        stockLabel,
                        "S/ ${m.precio.toMoney()}",
                    ),
                    rowIndex = index,
                    warn = critical,
                )
            }
        }

        val doc = pdf.finish()
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun Double.toMoney(): String = String.format(Locale.forLanguageTag("es-PE"), "%.2f", this)
}
