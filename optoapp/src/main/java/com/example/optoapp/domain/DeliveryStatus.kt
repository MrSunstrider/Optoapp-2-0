package com.example.optoapp.domain

import java.time.LocalDate

object OrderStatusPolicy {
    const val PENDIENTE = "Pendiente"
    const val ENTREGADO = "Entregado"
    const val ANULADO = "Anulado"
    const val RECLAMADA = "Reclamada"

    private val TERMINAL = setOf(ANULADO, RECLAMADA)
    private val ACTIVE = listOf(PENDIENTE, ENTREGADO)

    fun isTerminal(estado: String): Boolean = estado.trim() in TERMINAL

    fun isEditable(estado: String): Boolean = !isTerminal(estado)

    fun canCancel(estado: String): Boolean = estado.trim() in ACTIVE

    fun canClaim(estado: String): Boolean = estado.trim() == ENTREGADO

    /** Terminal estados have no undo; Anulado and Reclamada are reachable only through cancel and claim. */
    fun canTransition(from: String, to: String): Boolean {
        val target = to.trim()
        return when {
            from.trim() == target -> true
            isTerminal(from) -> false
            target == ANULADO -> canCancel(from)
            target == RECLAMADA -> canClaim(from)
            else -> target in ACTIVE
        }
    }

    fun selectableEstados(current: String): List<String> =
        if (isTerminal(current)) listOf(current.trim()) else ACTIVE

    fun requireEditable(estado: String, operation: String) {
        check(isEditable(estado)) { "La orden está ${terminalAdjective(estado)} y no se puede $operation." }
    }

    /** Estados are stored with servicio-era gender ("Anulado"); messages refer to "la orden". */
    private fun terminalAdjective(estado: String): String = when (estado.trim()) {
        ANULADO -> "anulada"
        RECLAMADA -> "reclamada"
        else -> estado.trim().lowercase()
    }
}

fun isPendingDelivery(estado: String, fechaEntrega: LocalDate?): Boolean =
    estado == OrderStatusPolicy.PENDIENTE && fechaEntrega == null

/** A cancelled or claimed order is closed, so editing its delivery date must not reopen it. */
fun estadoAfterFechaEntrega(currentEstado: String, fechaEntrega: LocalDate?): String {
    if (OrderStatusPolicy.isTerminal(currentEstado)) return currentEstado
    return if (fechaEntrega != null) OrderStatusPolicy.ENTREGADO else OrderStatusPolicy.PENDIENTE
}
