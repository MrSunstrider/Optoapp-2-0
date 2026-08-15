package com.example.optoapp.domain

/**
 * Canonical referenciaId for montura_movimientos writers.
 * The unique index is (referenciaId, tipo, monturaId) — the parent document id is
 * only safe when at most one movement of that tipo exists per montura under it.
 */
fun movimientoReferenciaForManual(movimientoId: String): String {
    require(movimientoId.isNotBlank()) { "manual movement id must not be blank" }
    return movimientoId
}

fun movimientoReferenciaForRegalo(regaloId: String): String {
    require(regaloId.isNotBlank()) { "regalo id must not be blank" }
    return regaloId
}

fun movimientoReferenciaForOrdenCompraItem(itemId: String): String {
    require(itemId.isNotBlank()) { "orden compra item id must not be blank" }
    return itemId
}

fun movimientoReferenciaForInventarioDetalle(detalleId: String): String {
    require(detalleId.isNotBlank()) { "inventario detalle id must not be blank" }
    return detalleId
}
