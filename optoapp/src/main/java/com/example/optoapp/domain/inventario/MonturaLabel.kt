package com.example.optoapp.domain.inventario

import com.example.optoapp.data.Montura

fun monturaLabel(montura: Montura): String =
    "${montura.marca} ${montura.modelo} (${montura.sku})"

fun monturaMatchesDescripcion(montura: Montura, descripcion: String): Boolean {
    if (descripcion.isBlank()) return false
    return descripcion.equals(monturaLabel(montura), ignoreCase = true) ||
        descripcion.contains("(${montura.sku})", ignoreCase = true)
}
