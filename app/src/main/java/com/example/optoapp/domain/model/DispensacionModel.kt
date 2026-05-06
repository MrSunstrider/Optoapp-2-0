package com.example.optoapp.domain.model

import java.time.LocalDate

data class DispensacionModel(
    val id: String,
    val ot: String = "",
    val monturaId: String = "",
    val pacienteId: String,
    val fecha: LocalDate,
    val opticaId: String,
    val tipoMontura: String = "",
    val materialMontura: String = "",
    val tipoLente: String = "",
    val materialLente: String = "",
    val tratamientos: List<String> = emptyList(),
    val colorLente: String = "",
    val notasDiseno: String = "",
    val origenMontura: String = "",
    val tipoAro: String = "",
    val descripcionMontura: String = "",
    val montoTotal: Double = 0.0,
    val metodoPago: String = "",
    val montoPagado: Double = 0.0,
    val estadoEntrega: String = "Pendiente",
    val fechaVencimientoGarantia: LocalDate? = null,
    val distanciaLente: String = "",
    val altura: String = "",
    val subTipoBifocal: String = ""
)
