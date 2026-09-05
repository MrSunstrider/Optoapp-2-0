package com.example.optoapp.domain.inventario

import com.example.optoapp.data.Montura

fun monturaLabel(montura: Montura): String {
    val base = "${montura.marca} ${montura.modelo} (${montura.sku})"
    return if (montura.tipoAro.isNotBlank()) "$base · ${montura.tipoAro}" else base
}

fun monturaMatchesDescripcion(montura: Montura, descripcion: String): Boolean {
    if (descripcion.isBlank()) return false
    if (descripcion.equals(monturaLabel(montura), ignoreCase = true)) return true
    val skuToken = "(${montura.sku})"
    if (!descripcion.contains(skuToken, ignoreCase = true)) return false
    // Same-SKU rim variants: only match when tipoAro is present in the description.
    if (montura.tipoAro.isNotBlank()) {
        return descripcion.contains(montura.tipoAro, ignoreCase = true)
    }
    // Accesorios / legacy rows without tipoAro.
    return true
}
