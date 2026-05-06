package com.example.optoapp.domain.model

import java.time.LocalDate

data class PagoModel(
    val id: String,
    val dispensacionId: String? = null,
    val servicioExtraId: String? = null,
    val fecha: LocalDate,
    val tipo: String,
    val monto: Double,
    val metodoPago: String = "",
    val nota: String = "",
    val opticaId: String
)
