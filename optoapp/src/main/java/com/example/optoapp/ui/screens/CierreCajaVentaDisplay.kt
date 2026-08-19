package com.example.optoapp.ui.screens

import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.Pago
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.domain.PagoEffect
import com.example.optoapp.viewmodel.PagoDisplayItem
import java.time.LocalDate

fun dispensacionVentaTitle(disp: DispensacionOptica): String =
    if (disp.ot.isNotBlank()) "OT ${disp.ot}" else "Dispensación ${disp.id.take(8)}"

fun dispensacionVentaSubtitle(disp: DispensacionOptica): String {
    val lente = listOf(disp.tipoLente, disp.materialLente).filter { it.isNotBlank() }.joinToString(" · ")
    if (lente.isNotBlank()) return lente
    if (disp.descripcionMontura.isNotBlank()) return disp.descripcionMontura
    return disp.tipoMontura.ifBlank { "Dispensación óptica" }
}

fun servicioVentaOtLine(serv: ServicioExtra): String? =
    serv.ot.takeIf { it.isNotBlank() }?.let { "OT $it" }

fun heroCobradoLabel(selectedDate: LocalDate, today: LocalDate): String = when (selectedDate) {
    today -> "COBRADO HOY"
    today.minusDays(1) -> "COBRADO AYER"
    else -> "TOTAL COBRADO"
}

fun matchesCierreCajaSearch(haystack: String, query: String): Boolean {
    val q = query.trim()
    if (q.isBlank()) return true
    return haystack.lowercase().contains(q.lowercase())
}

fun dispensacionSearchHaystack(disp: DispensacionOptica, pacienteNombre: String = ""): String =
    listOf(
        dispensacionVentaTitle(disp),
        dispensacionVentaSubtitle(disp),
        disp.ot,
        disp.estadoEntrega,
        pacienteNombre,
    ).joinToString(" ")

fun servicioSearchHaystack(serv: ServicioExtra, pacienteNombre: String = ""): String =
    listOf(
        serv.ot,
        serv.descripcion,
        serv.estado,
        serv.metodoPago,
        pacienteNombre,
    ).joinToString(" ")

fun pagoDisplaySearchHaystack(item: PagoDisplayItem, pacienteNombre: String = ""): String =
    pagoDisplaySearchHaystack(
        label = item.label,
        tipoEntidad = item.tipoEntidad,
        metodoPago = item.pago.metodoPago,
        tipo = item.pago.tipo,
        pacienteNombre = pacienteNombre,
    )

fun pagoDisplaySearchHaystack(
    label: String,
    tipoEntidad: String,
    metodoPago: String,
    tipo: String,
    pacienteNombre: String = "",
): String = listOf(label, tipoEntidad, metodoPago, tipo, pacienteNombre).joinToString(" ")

fun filterPagoDisplayItems(
    items: List<PagoDisplayItem>,
    query: String,
    pacienteNombres: Map<String, String>,
): List<PagoDisplayItem> = items.filter { item ->
    val nombre = item.pacienteId?.let { pacienteNombres[it] }.orEmpty()
    matchesCierreCajaSearch(pagoDisplaySearchHaystack(item, nombre), query)
}

fun filterDispensaciones(
    items: List<DispensacionOptica>,
    query: String,
    pacienteNombres: Map<String, String>,
): List<DispensacionOptica> = items.filter { disp ->
    val nombre = pacienteNombres[disp.pacienteId].orEmpty()
    matchesCierreCajaSearch(dispensacionSearchHaystack(disp, nombre), query)
}

fun pagosEffectByDispensacion(pagos: List<Pago>): Map<String, Double> =
    pagos.mapNotNull { pago -> pago.dispensacionId?.let { it to pago } }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, rows) -> rows.sumOf { PagoEffect.signedAmount(it.tipo, it.monto) } }

fun pagosEffectByServicio(pagos: List<Pago>): Map<String, Double> =
    pagos.mapNotNull { pago -> pago.servicioExtraId?.let { it to pago } }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, rows) -> rows.sumOf { PagoEffect.signedAmount(it.tipo, it.monto) } }

/** Same-day ledger wins over a downloaded/stale parent cache (OT 4582 dual-writer). */
fun cierreVentaPagado(cachePagado: Double, entityId: String, ledgerById: Map<String, Double>): Double =
    ledgerById[entityId] ?: cachePagado

fun filterServiciosExtra(
    items: List<ServicioExtra>,
    query: String,
    pacienteNombres: Map<String, String>,
): List<ServicioExtra> = items.filter { serv ->
    val nombre = serv.pacienteId?.let { pacienteNombres[it] }.orEmpty()
    matchesCierreCajaSearch(servicioSearchHaystack(serv, nombre), query)
}
