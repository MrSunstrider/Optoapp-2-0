package com.example.optoapp.data.costobiselado

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "costos_biselado")
data class CostoBiseladoEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "optica_id") val opticaId: String,
    val material: String,
    @ColumnInfo(name = "tipo_aro") val tipoAro: String,
    @ColumnInfo(name = "stock_o_fabricacion") val stockOFabricacion: String,
    val serie: Int? = null,
    @ColumnInfo(name = "alto_indice") val altoIndice: String? = null,
    @ColumnInfo(name = "costo_por_par") val costoPorPar: Double,
    val proveedor: String? = null,
    @ColumnInfo(name = "vigente_desde") val vigenteDesde: String,
    @ColumnInfo(name = "vigente_hasta") val vigenteHasta: String? = null,
)
