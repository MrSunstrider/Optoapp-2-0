package com.example.optoapp.data.costoproducto

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "costos_productos")
data class CostoProductoEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "optica_id") val opticaId: String,
    val material: String,
    @ColumnInfo(name = "tipo_lente") val tipoLente: String,
    @ColumnInfo(name = "stock_o_fabricacion") val stockOFabricacion: String,
    val tratamiento: String? = null,
    val serie: Int? = null,
    @ColumnInfo(name = "costo_unitario") val costoUnitario: Double,
    @ColumnInfo(name = "laboratorio_id") val laboratorioId: String? = null,
    @ColumnInfo(name = "vigente_desde") val vigenteDesde: String,
    @ColumnInfo(name = "vigente_hasta") val vigenteHasta: String? = null
)
