package com.example.optoapp.domain

import java.time.LocalDate

data class Deudor(
    val pacienteNombre: String,
    val pacienteTelefono: String,
    val ventaId: String,
    val ventaFecha: LocalDate,
    val montoTotal: Double,
    val totalPagado: Double,
    val saldo: Double,
    val diasDeuda: Int,
    val pacienteId: String = ""
)
