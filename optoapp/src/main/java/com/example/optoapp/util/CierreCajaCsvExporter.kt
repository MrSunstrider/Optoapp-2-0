package com.example.optoapp.util

import android.content.Context
import com.example.optoapp.domain.PagoEffect
import com.example.optoapp.viewmodel.CierreCajaUiState
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * Day-close CSV from already-aggregated Cierre totals.
 * Callers MUST pass PagoEffect-derived [cobradoHoy] / [totalesPorMetodo] (not raw sum(monto)).
 */
object CierreCajaCsvExporter {
    private const val BOM = "\uFEFF"

    fun writeToCache(
        context: Context,
        state: CierreCajaUiState,
        cobradoHoy: Double,
        totalesPorMetodo: Map<String, Double>,
        contado: Double? = null,
    ): File {
        val dir = File(context.cacheDir, "cierre_caja").apply { mkdirs() }
        val file = File(dir, "cierre-${state.fecha}-${System.currentTimeMillis()}.csv")
        file.writeText(toCsv(state, cobradoHoy, totalesPorMetodo, contado), StandardCharsets.UTF_8)
        return file
    }

    fun toCsv(
        state: CierreCajaUiState,
        cobradoHoy: Double,
        totalesPorMetodo: Map<String, Double>,
        contado: Double? = null,
    ): String {
        val sb = StringBuilder(BOM)
        val efectivoNet = totalesPorMetodo["Efectivo"] ?: 0.0
        val diferencia = contado?.let { it - efectivoNet }

        sb.appendLine("Fecha,Cobrado,VentasDia,CobrosAtrasados,VentasRegistradas,SaldoPendiente,Contado,Diferencia")
        sb.append(state.fecha)
        sb.append(',')
        sb.append(money(cobradoHoy)).append(',')
        sb.append(money(state.ventasHoy)).append(',')
        sb.append(money(state.cobrosAtrasados)).append(',')
        sb.append(money(state.totalGeneral)).append(',')
        sb.append(money(state.saldoPendiente)).append(',')
        sb.append(contado?.let { money(it) }.orEmpty()).append(',')
        sb.append(diferencia?.let { money(it) }.orEmpty())
        sb.appendLine()
        sb.appendLine()

        sb.appendLine("Metodo,Monto")
        totalesPorMetodo.entries
            .sortedBy { it.key }
            .forEach { (metodo, monto) ->
                val label = metodo.ifBlank { "Sin espec." }
                sb.append(escape(label)).append(',').append(money(monto)).appendLine()
            }
        sb.appendLine()

        sb.appendLine("Cobros")
        sb.appendLine("Fecha,Tipo,Metodo,Monto,Label,EsCobroAtrasado")
        state.pagosDisplay.forEach { item ->
            val p = item.pago
            sb.append(p.fecha).append(',')
            sb.append(escape(p.tipo)).append(',')
            sb.append(escape(p.metodoPago)).append(',')
            sb.append(money(PagoEffect.signedAmount(p.tipo, p.monto))).append(',')
            sb.append(escape(item.label)).append(',')
            sb.append(item.esCobroAtrasado).appendLine()
        }
        return sb.toString()
    }

    private fun money(value: Double): String =
        String.format(Locale.US, "%.2f", value)

    private fun escape(value: String): String {
        if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            return "\"" + value.replace("\"", "\"\"") + "\""
        }
        return value
    }
}
