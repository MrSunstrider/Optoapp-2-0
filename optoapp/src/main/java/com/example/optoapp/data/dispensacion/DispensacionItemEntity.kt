package com.example.optoapp.data

import androidx.room.ColumnInfo
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
        childColumns = ["dispensacion_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index(value = ["dispensacion_id"]),
        Index(value = ["optica_id"])
    ]
)
@Serializable
data class DispensacionItem(
    @PrimaryKey val id: String,
    @SerialName("dispensacion_id")
    @ColumnInfo(name = "dispensacion_id")
    val dispensacionId: String,

    //── Lente ────────────────────────────────────────────────────────────
    @SerialName("tipo_lente")
    @ColumnInfo(name = "tipo_lente")
    val tipoLente: String = "",
    @SerialName("material_lente")
    @ColumnInfo(name = "material_lente")
    val materialLente: String = "",
    val tratamientos: List<String> = emptyList(),
    @SerialName("color_lente")
    @ColumnInfo(name = "color_lente")
    val colorLente: String = "",
    @SerialName("distancia_lente")
    @ColumnInfo(name = "distancia_lente")
    val distanciaLente: String = "",
    val altura: String = "",
    @SerialName("sub_tipo_bifocal")
    @ColumnInfo(name = "sub_tipo_bifocal")
    val subTipoBifocal: String = "",
    @SerialName("notas_diseno")
    @ColumnInfo(name = "notas_diseno")
    val notasDiseno: String = "",

    //── Montura (cada item puede tener su propia montura) ────────────────
    @SerialName("montura_id")
    @ColumnInfo(name = "montura_id")
    val monturaId: String = "",
    @SerialName("origen_montura")
    @ColumnInfo(name = "origen_montura")
    val origenMontura: String = "",
    @SerialName("tipo_aro")
    @ColumnInfo(name = "tipo_aro")
    val tipoAro: String = "",
    @SerialName("material_montura")
    @ColumnInfo(name = "material_montura")
    val materialMontura: String = "",
    @SerialName("descripcion_montura")
    @ColumnInfo(name = "descripcion_montura")
    val descripcionMontura: String = "",
    @SerialName("tipo_montura")
    @ColumnInfo(name = "tipo_montura")
    val tipoMontura: String = "",

    @SerialName("optica_id")
    @ColumnInfo(name = "optica_id")
    val opticaId: String = "mi_optica_base"
)
