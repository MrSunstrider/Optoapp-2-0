package com.example.optoapp.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.optoapp.data.LocalDateSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Entity(
    tableName = "pacientes",
    indices = [
        Index(value = ["nombreCompleto"]),
        Index(value = ["opticaId"])
    ]
)
@Serializable
data class Paciente(
    @PrimaryKey val id: String,
    @SerialName("nombreCompleto")
    val nombreCompleto: String,
    val edad: Int,
    val telefono: String,
    @SerialName("fechaCreacion")
    @Serializable(with = LocalDateSerializer::class)
    val fechaCreacion: LocalDate,
    val dni: String? = null,
    @Serializable(with = LocalDateSerializer::class)
    val fechaNacimiento: LocalDate? = null,
    val sexo: String? = null,
    val email: String? = null,
    @SerialName("historiaOptometrica")
    val historiaOptometrica: String? = null,
    val direccion: String? = null,
    val distrito: String? = null,
    val ocupacion: String? = null,
    val acompanante: String? = null,
    val hobbies: String? = null,
    val ultimasEtiquetas: List<String> = emptyList(),
    @SerialName("opticaId")
    val opticaId: String = "mi_optica_base"
)
