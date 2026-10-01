package com.example.optoapp.domain

import java.time.LocalDate

object OrderStatusPolicy {
    const val PENDIENTE = "Pendiente"
    const val ENTREGADO = "Entregado"
    const val ANULADO = "Anulado"
    const val RECLAMADA = "Reclamada"

    private val TERMINAL = setOf(ANULADO, RECLAMADA)

    fun isTerminal(estado: String): Boolean = estado.trim() in TERMINAL
}

/** Waiting for delivery: still Pendiente and no delivery date recorded. */
fun isPendingDelivery(estado: String, fechaEntrega: LocalDate?): Boolean =
    estado == OrderStatusPolicy.PENDIENTE && fechaEntrega == null

/**
 * Assigning a delivery date means Entregado. Clearing it returns to Pendiente.
 * Anulado/Reclamada keep their estado.
 */
fun estadoAfterFechaEntrega(currentEstado: String, fechaEntrega: LocalDate?): String {
    if (OrderStatusPolicy.isTerminal(currentEstado)) return currentEstado
    return if (fechaEntrega != null) OrderStatusPolicy.ENTREGADO else OrderStatusPolicy.PENDIENTE
}
