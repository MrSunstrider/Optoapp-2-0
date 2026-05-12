package com.example.optoapp.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.time.LocalDate

@Entity(
    tableName = "pacientes",
    indices = [
        Index(value = ["nombreCompleto"]),
        Index(value = ["opticaId"])
    ]
)
data class Paciente(
    @PrimaryKey val id: String,
    @SerializedName("nombreCompleto", alternate = ["nombre_completo"])
    val nombreCompleto: String,
    val edad: Int,
    val telefono: String,
    @SerializedName("fechaCreacion", alternate = ["fecha_creacion"])
    val fechaCreacion: LocalDate,
    val dni: String? = null,
    val fechaNacimiento: LocalDate? = null,
    val sexo: String? = null,
    val email: String? = null,
    @SerializedName("historiaOptometrica", alternate = ["historia_optometrica"])
    val historiaOptometrica: String? = null,
    val direccion: String? = null,
    val distrito: String? = null,
    val ocupacion: String? = null,
    val acompanante: String? = null,
    val hobbies: String? = null,
    val ultimasEtiquetas: List<String> = emptyList(),
    @SerializedName("opticaId", alternate = ["optica_id"])
    val opticaId: String = "mi_optica_base"
)
