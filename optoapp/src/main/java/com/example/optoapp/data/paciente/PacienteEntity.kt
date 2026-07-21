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
        Index(value = ["opticaId"]),
    ],
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
    val opticaId: String = LEGACY_OPTICA_ID,
    @SerialName("updatedAt")
    val updatedAt: String? = null,
    @SerialName("updatedBy")
    val updatedBy: String? = null,
) {
    companion object {
        const val LEGACY_OPTICA_ID = "mi_optica_base"
    }
}
fun Paciente.esMasculino(): Boolean = sexo.equals("Masculino", ignoreCase = true)
fun Paciente.esFemenino(): Boolean = sexo.equals("Femenino", ignoreCase = true)

