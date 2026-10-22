package com.example.optoapp.util

import android.content.Context
import android.graphics.Paint
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.ServicioExtra
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object ReporteFinancieroPdfGenerator {

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
        extras: ReportePdfExtras = ReportePdfExtras(),
    ): File {
        val dir = File(context.cacheDir, "reportes").apply { mkdirs() }
        val file = File(dir, "reporte-financiero-${System.currentTimeMillis()}.pdf")

        val periodLabel = when (periodo) {
            "Diario" -> "Día de hoy"
            "Semanal" -> "Esta semana"
            "Mensual" -> "Mensual"
            "Anual" -> "Anual"
            else -> "Total"
        }

        val pdf = ReportPdfCanvas()
        pdf.start("Reporte financiero", "Período: $periodLabel")
        pdf.drawKpis(
            listOf(
                ReportPdfKpi("Total vendido", "S/ ${totalVendido.toMoney()}"),
                ReportPdfKpi("Por cobrar", "S/ ${porCobrar.toMoney()}"),
                ReportPdfKpi("Ticket prom.", "S/ ${ticketPromedio.toMoney()}"),
            ),
        )

        val otCols = listOf(
            ReportPdfColumn("Fecha", ReportPdfTheme.MARGIN, 80f),
            ReportPdfColumn("Pago", ReportPdfTheme.MARGIN + 90f, 160f),
            ReportPdfColumn("Total", ReportPdfTheme.MARGIN + 300f, 90f),
            ReportPdfColumn("Saldo", ReportPdfTheme.MARGIN + 420f, 90f),
        )
        pdf.drawSectionTitle("Dispensaciones / OT")
        if (dispensaciones.isEmpty()) {
            pdf.drawMutedLine("Sin dispensaciones en el período")
        } else {
            pdf.drawTableHeader(otCols)
            dispensaciones.forEachIndexed { index, disp ->
                val montoPagado = pagosSumByDispensacion[disp.id] ?: 0.0
                val saldo = disp.montoTotal - montoPagado
                pdf.drawTableRow(
                    otCols,
                    listOf(
                        DateUtils.formatLocalized(disp.fecha),
                        disp.metodoPago.ifBlank { "—" },
                        "S/ ${disp.montoTotal.toMoney()}",
                        "S/ ${saldo.toMoney()}",
                    ),
                    rowIndex = index,
                    warn = saldo > 0.009,
                )
            }
        }

        if (serviciosExtra.isNotEmpty()) {
            val servCols = listOf(
                ReportPdfColumn("Descripción", ReportPdfTheme.MARGIN, 260f),
                ReportPdfColumn("Monto", ReportPdfTheme.MARGIN + 300f, 90f),
                ReportPdfColumn("Pagado", ReportPdfTheme.MARGIN + 420f, 90f),
            )
            pdf.drawSectionTitle("Servicios extra")
            pdf.drawTableHeader(servCols)
            serviciosExtra.forEachIndexed { index, serv ->
                val aCuenta = aCuentaSumByServicio[serv.id] ?: 0.0
                pdf.drawTableRow(
                    servCols,
                    listOf(
                        serv.descripcion.ifBlank { "Sin descripción" },
                        "S/ ${serv.montoTotal.toMoney()}",
                        "S/ ${aCuenta.toMoney()}",
                    ),
                    rowIndex = index,
                )
            }
        }

        drawExtras(pdf, extras)

        val doc = pdf.finish()
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun drawExtras(pdf: ReportPdfCanvas, extras: ReportePdfExtras) {
        extras.comparison?.let { cmp ->
            pdf.drawSectionTitle("Comparativo vs período anterior")
            val vendidoPct = cmp.vendidoDeltaPct()?.let { String.format(Locale.US, "%.1f%%", it) } ?: "—"
            val margenPct = cmp.margenDeltaPct()?.let { String.format(Locale.US, "%.1f%%", it) } ?: "—"
            pdf.drawBodyLine("Vendido delta: S/ ${cmp.vendidoDelta.toMoney()} ($vendidoPct)")
            pdf.drawBodyLine("Margen delta: S/ ${cmp.margenDelta.toMoney()} ($margenPct)")
            pdf.drawBodyLine("Transacciones delta: ${cmp.transaccionesDelta}")
        }
        extras.pacientes?.takeIf { p ->
            p.altas != 0 || p.hombres != 0 || p.mujeres != 0 || p.sinSexo != 0 ||
                p.pediatricos != 0 || p.adultos != 0 || p.adultosMayores != 0
        }?.let { p ->
            pdf.drawSectionTitle("Pacientes")
            pdf.drawKpis(
                listOf(
                    ReportPdfKpi("Altas", p.altas.toString()),
                    ReportPdfKpi("Hombres", p.hombres.toString()),
                    ReportPdfKpi("Mujeres", p.mujeres.toString()),
                    ReportPdfKpi("Sin dato", p.sinSexo.toString()),
                ),
            )
            pdf.drawKpis(
                listOf(
                    ReportPdfKpi("Pediatricos", p.pediatricos.toString()),
                    ReportPdfKpi("Adultos", p.adultos.toString()),
                    ReportPdfKpi("Adulto mayor", p.adultosMayores.toString()),
                ),
            )
        }
        extras.inventario?.takeIf { inv ->
            inv.modelos > 0 || inv.stockTotal > 0 || inv.lowStock > 0 || inv.salidasVentaPeriodo > 0
        }?.let { inv ->
            pdf.drawSectionTitle("Inventario (resumen)")
            pdf.drawKpis(
                listOf(
                    ReportPdfKpi("Modelos", inv.modelos.toString()),
                    ReportPdfKpi("Stock", inv.stockTotal.toString()),
                    ReportPdfKpi("Bajo mín.", inv.lowStock.toString()),
                    ReportPdfKpi("Salidas", inv.salidasVentaPeriodo.toString()),
                ),
            )
            inv.stockPorTipoAro.takeIf { it.isNotEmpty() }?.let { buckets ->
                pdf.drawBodyLine(
                    "Tipo de aro: " + buckets.joinToString(" · ") { "${it.label} ${it.unidades}" },
                )
            }
            inv.stockPorMaterial.takeIf { it.isNotEmpty() }?.let { buckets ->
                pdf.drawBodyLine(
                    "Material: " + buckets.joinToString(" · ") { "${it.label} ${it.unidades}" },
                )
            }
            inv.topSalidasModelo.takeIf { it.isNotEmpty() }?.let { buckets ->
                pdf.drawBodyLine(
                    "Top salidas: " + buckets.joinToString(" · ") { "${it.label} ${it.unidades}" },
                )
            }
        }
        extras.clinico?.takeIf { it.evaluaciones > 0 }?.let { c ->
            pdf.drawSectionTitle("Clínico")
            pdf.drawBodyLine("Evaluaciones: ${c.evaluaciones}")
            if (c.porSexo.isNotEmpty()) {
                pdf.drawBodyLine("Sexo: " + c.porSexo.joinToString(" · ") { "${it.label} ${it.unidades}" })
            }
            if (c.porEdad.isNotEmpty()) {
                pdf.drawBodyLine("Edad: " + c.porEdad.joinToString(" · ") { "${it.label} ${it.unidades}" })
            }
            if (c.porEmetropia.isNotEmpty()) {
                pdf.drawBodyLine("Refracción: " + c.porEmetropia.joinToString(" · ") { "${it.label} ${it.unidades}" })
            }
            c.porDiagnostico.take(12).forEach { (label, count) ->
                pdf.drawBodyLine("$label: $count")
            }
        }
        extras.equipo?.takeIf { it.filas.isNotEmpty() || it.sinAtribuirCount > 0 }?.let { e ->
            pdf.drawSectionTitle("Equipo")
            pdf.drawMutedLine("Comisión: ${String.format(Locale.US, "%.1f", e.comisionPct)}%")
            val cols = listOf(
                ReportPdfColumn("Vendedor", ReportPdfTheme.MARGIN, 180f),
                ReportPdfColumn("Vendido", ReportPdfTheme.MARGIN + 200f, 100f),
                ReportPdfColumn("Tx", ReportPdfTheme.MARGIN + 320f, 50f),
                ReportPdfColumn("Comisión", ReportPdfTheme.MARGIN + 390f, 100f),
            )
            pdf.drawTableHeader(cols)
            e.filas.forEachIndexed { index, row ->
                pdf.drawTableRow(
                    cols,
                    listOf(
                        row.displayName,
                        "S/ ${row.vendido.toMoney()}",
                        row.transacciones.toString(),
                        "S/ ${row.comision.toMoney()}",
                    ),
                    rowIndex = index,
                )
            }
            if (e.sinAtribuirCount > 0) {
                pdf.drawBodyLine(
                    "Sin atribuir: S/ ${e.sinAtribuirVendido.toMoney()} (${e.sinAtribuirCount})",
                    warn = true,
                )
            }
        }
    }

    private fun Double.toMoney(): String = String.format(Locale.forLanguageTag("es-PE"), "%.2f", this)

    internal fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> =
        ReportPdfCanvas.wrapText(text, paint, maxWidth)
}
