package com.example.optoapp.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "ordenes_compra",
    indices = [
        Index(value = ["opticaId"]),
        Index(value = ["numero"]),
        Index(value = ["estado"]),
        Index(value = ["proveedorId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Proveedor::class,
            parentColumns = ["id"],
            childColumns = ["proveedorId"]
        )
    ]
)
data class OrdenCompra(
    @PrimaryKey val id: String,
    val numero: String,
    val proveedorId: String,
    val fecha: LocalDate,
    val estado: String = "PENDIENTE",
    val total: Double = 0.0,
    val opticaId: String,
    val updatedAt: String? = null,
    val updatedBy: String? = null
)

@Entity(
    tableName = "orden_compra_items",
    indices = [
        Index(value = ["ordenId"]),
        Index(value = ["monturaId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = OrdenCompra::class,
            parentColumns = ["id"],
            childColumns = ["ordenId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Montura::class,
            parentColumns = ["id"],
            childColumns = ["monturaId"]
        )
    ]
)
data class OrdenCompraItem(
    @PrimaryKey val id: String,
    val ordenId: String,
    val monturaId: String,
    val cantidad: Int,
    val costoUnitario: Double = 0.0,
    val recibido: Int = 0
)
