package com.example.optoapp.data.categoriaproducto

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categorias_producto")
data class CategoriaProductoEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val familia: String,
    val orden: Int = 0,
)
