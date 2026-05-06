package com.example.optoapp.domain.model

import java.time.LocalDate

data class PacienteModel(
    val id: String,
    val nombreCompleto: String,
    val edad: Int,
    val telefono: String,
    val fechaCreacion: LocalDate,
    val dni: String? = null,
    val fechaNacimiento: LocalDate? = null,
    val sexo: String? = null,
    val email: String? = null,
    val historiaOptometrica: String? = null,
    val direccion: String? = null,
    val distrito: String? = null,
    val ocupacion: String? = null,
    val acompanante: String? = null,
    val hobbies: String? = null,
    val ultimasEtiquetas: List<String> = emptyList(),
    val opticaId: String
)
