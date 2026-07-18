package com.example.optoapp.data.costolc

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "costos_lc")
data class CostoLcEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "optica_id") val opticaId: String,
    @ColumnInfo(name = "tipo_lc") val tipoLc: String,
    @ColumnInfo(name = "material_lc") val materialLc: String,
    val modalidad: String,
    @ColumnInfo(name = "radio_base") val radioBase: String? = null,
    val diametro: String? = null,
    @ColumnInfo(name = "laboratorio_id") val laboratorioId: String? = null,
    @ColumnInfo(name = "costo_unitario") val costoUnitario: Double,
    @ColumnInfo(name = "vigente_desde") val vigenteDesde: String,
    @ColumnInfo(name = "vigente_hasta") val vigenteHasta: String? = null
)
