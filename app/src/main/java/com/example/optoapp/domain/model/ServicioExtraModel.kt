package com.example.optoapp.domain.model

import java.time.LocalDate

data class ServicioExtraModel(
    val id: String,
    val ot: String = "",
    val descripcion: String,
    val montoTotal: Double,
    val aCuenta: Double,
    val estado: String,
    val fecha: LocalDate,
    val pacienteId: String? = null,
    val metodoPago: String = "",
    val opticaId: String
)
