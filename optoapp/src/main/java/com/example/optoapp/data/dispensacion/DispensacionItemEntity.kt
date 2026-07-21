package com.example.optoapp.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity(
    tableName = "dispensacion_items",
    foreignKeys = [
        ForeignKey(
            entity = DispensacionOptica::class,
            parentColumns = ["id"],
            childColumns = ["dispensacion_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["dispensacion_id"]),
        Index(value = ["optica_id"]),
    ],
)
@Serializable
data class DispensacionItem(
    @PrimaryKey val id: String,
    @SerialName("dispensacion_id")
    @ColumnInfo(name = "dispensacion_id")
    val dispensacionId: String,

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
    @SerialName("filtro_discromatopsia_tipo")
    @ColumnInfo(name = "filtro_discromatopsia_tipo")
    val filtroDiscromatopsiaTipo: String = "",
    @SerialName("notas_diseno")
    @ColumnInfo(name = "notas_diseno")
    val notasDiseno: String = "",

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
    val opticaId: String = "mi_optica_base",

    @SerialName("alto_indice")
    @ColumnInfo(name = "alto_indice")
    val altoIndice: String? = null,
    @SerialName("reduccion_diametro")
    @ColumnInfo(name = "reduccion_diametro")
    val reduccionDiametro: String? = null,
    @SerialName("lenticular")
    @ColumnInfo(name = "lenticular")
    val lenticular: String? = null,
    @SerialName("curva_base")
    @ColumnInfo(name = "curva_base")
    val curvaBase: String? = null,
    @SerialName("costo_real_od")
    @ColumnInfo(name = "costo_real_od")
    val costoRealOd: Double? = null,
    @SerialName("costo_real_oi")
    @ColumnInfo(name = "costo_real_oi")
    val costoRealOi: Double? = null,
    @SerialName("costo_real_montura")
    @ColumnInfo(name = "costo_real_montura")
    val costoRealMontura: Double? = null,
    @SerialName("costo_real_biselado")
    @ColumnInfo(name = "costo_real_biselado")
    val costoRealBiselado: Double? = null,
    @SerialName("costo_real_lc")
    @ColumnInfo(name = "costo_real_lc")
    val costoRealLc: Double? = null,
)
