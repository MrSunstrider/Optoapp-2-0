package com.example.optoapp.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Item individual de lente dentro de una dispensación.
 *
 * Permite registrar múltiples lentes (lejos, cerca, intermedio, etc.)
 * bajo una misma orden de trabajo (OT). Cada item representa UN lente
 * con sus características ópticas específicas.
 *
 * La dispensación padre (header) conserva montura, montos y estado de entrega.
 */
@Entity(
    tableName = "dispensacion_items",
    foreignKeys = [ForeignKey(
        entity = DispensacionOptica::class,
        parentColumns = ["id"],
        childColumns = ["dispensacionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index(value = ["dispensacionId"]),
        Index(value = ["opticaId"])
    ]
)
@Serializable
data class DispensacionItem(
    @PrimaryKey val id: String,
    @SerialName("dispensacion_id")
    val dispensacionId: String,

    //── Lente ────────────────────────────────────────────────────────────
    @SerialName("tipo_lente")
    val tipoLente: String = "",
    @SerialName("material_lente")
    val materialLente: String = "",
    val tratamientos: List<String> = emptyList(),
    @SerialName("color_lente")
    val colorLente: String = "",
    @SerialName("distancia_lente")
    val distanciaLente: String = "",
    val altura: String = "",
    @SerialName("sub_tipo_bifocal")
    val subTipoBifocal: String = "",
    @SerialName("notas_diseno")
    val notasDiseno: String = "",

    //── Montura (cada item puede tener su propia montura) ────────────────
    @SerialName("montura_id")
    val monturaId: String = "",
    @SerialName("origen_montura")
    val origenMontura: String = "",
    @SerialName("tipo_aro")
    val tipoAro: String = "",
    @SerialName("material_montura")
    val materialMontura: String = "",
    @SerialName("descripcion_montura")
    val descripcionMontura: String = "",
    @SerialName("tipo_montura")
    val tipoMontura: String = "",

    @SerialName("optica_id")
    val opticaId: String = "mi_optica_base"
)
