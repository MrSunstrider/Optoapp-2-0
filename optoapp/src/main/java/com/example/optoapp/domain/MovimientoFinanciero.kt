package com.example.optoapp.domain

import java.time.LocalDate

data class MovimientoFinanciero(
    val id: String,
    val fecha: LocalDate,
    val tipo: TipoMovimiento,
    val origen: Origen,
    val origenId: String,
    val montoTotal: Double,
    val montoPagado: Double,
    val costo: Double,
    val pacienteId: String,
    val opticaId: String,
    val descripcion: String,
    val vinculadoA: String? = null,
)

enum class TipoMovimiento {
    VENTA,
    ANULACION,
    RECLAMO,
    REGALO,
    ABONO,
}

enum class Origen {
    DISPENSACION,
    SERVICIO,
    REGALO,
}
