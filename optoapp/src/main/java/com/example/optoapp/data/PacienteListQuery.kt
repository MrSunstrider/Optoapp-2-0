package com.example.optoapp.data

object PacienteListFilters {
    const val TODOS = "Todos"
    const val SALDO_PENDIENTE = "Saldo Pendiente"
    const val ESTADO_ENTREGA = "Estado de entrega"
}

/** Same-day/ledger cobros win over a stale parent cache, like Cierre. No pagos keeps cache. */
fun pagadoParaSaldo(cachePagado: Double, ledgerPagado: Double?): Double = ledgerPagado ?: cachePagado

fun tieneSaldoPendiente(montoTotal: Double, cachePagado: Double, ledgerPagado: Double?): Boolean =
    montoTotal - pagadoParaSaldo(cachePagado, ledgerPagado) > 0

fun pacienteMatchesListQuery(
    query: String,
    nombreCompleto: String,
    id: String,
    telefono: String,
    historiaOptometrica: String?,
    assignedOts: Collection<String> = emptyList(),
): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return true
    return nombreCompleto.contains(q, ignoreCase = true) ||
        id.contains(q, ignoreCase = true) ||
        telefono.contains(q) ||
        historiaOptometrica.orEmpty().contains(q, ignoreCase = true) ||
        assignedOts.any { it.contains(q, ignoreCase = true) }
}
