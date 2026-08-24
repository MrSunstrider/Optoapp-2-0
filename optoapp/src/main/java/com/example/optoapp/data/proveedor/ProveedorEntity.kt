package com.example.optoapp.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "proveedores",
    indices = [
        Index(value = ["opticaId"]),
        Index(value = ["ruc", "opticaId"], unique = true),
    ],
)
data class Proveedor(
    @PrimaryKey val id: String,
    val nombre: String,
    val ruc: String,
    val telefono: String = "",
    val email: String = "",
    val direccion: String = "",
    val contacto: String = "",
    /** monturas | laboratorio | tecnico */
    val tipo: String = "monturas",
    val activo: Boolean = true,
    val opticaId: String,
    val updatedAt: String? = null,
    val updatedBy: String? = null,
)

@Entity(
    tableName = "montura_proveedor",
    indices = [
        Index(value = ["monturaId", "proveedorId"], unique = true),
        Index(value = ["monturaId"]),
        Index(value = ["proveedorId"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = Montura::class,
            parentColumns = ["id"],
            childColumns = ["monturaId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Proveedor::class,
            parentColumns = ["id"],
            childColumns = ["proveedorId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class MonturaProveedor(
    @PrimaryKey val id: String,
    val monturaId: String,
    val proveedorId: String,
    val costoProveedor: Double = 0.0,
    val precioSugerido: Double = 0.0,
    val activo: Boolean = true,
)

@Entity(
    tableName = "categorias_montura",
    indices = [
        Index(value = ["nombre", "opticaId"], unique = true),
    ],
)
data class CategoriaMontura(
    @PrimaryKey val id: String,
    val nombre: String,
    val descripcion: String = "",
    val opticaId: String,
)
