package com.example.optoapp.domain

import java.time.LocalDate

/** Waiting for delivery: still Pendiente and no delivery date recorded. */
fun isPendingDelivery(estado: String, fechaEntrega: LocalDate?): Boolean =
    estado == "Pendiente" && fechaEntrega == null

/**
 * Assigning a delivery date means Entregado. Clearing it returns to Pendiente.
 * Anulado/Reclamada keep their estado.
 */
fun estadoAfterFechaEntrega(currentEstado: String, fechaEntrega: LocalDate?): String {
    if (currentEstado == "Anulado" || currentEstado == "Reclamada") return currentEstado
    return if (fechaEntrega != null) "Entregado" else "Pendiente"
}
