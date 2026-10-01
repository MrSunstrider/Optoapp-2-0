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

fun movimientoReferenciaForServicioExtraReverso(servicioId: String, monturaId: String): String {
    require(servicioId.isNotBlank()) { "servicio id must not be blank" }
    require(monturaId.isNotBlank()) { "montura id must not be blank" }
    return "$servicioId:rev:$monturaId"
}

/**
 * WHY: cancel restocks reuse the montura of the sale/edit rows, so the `:anul` segment keeps the
 * reversal outside their (referenciaId, tipo, monturaId) slot and makes a repeated cancel hit
 * the same key instead of inserting a second restock.
 */
fun movimientoReferenciaForDispensacionItemAnulacion(dispensacionId: String, itemId: String): String {
    require(dispensacionId.isNotBlank()) { "dispensacion id must not be blank" }
    require(itemId.isNotBlank()) { "dispensacion item id must not be blank" }
    return "$dispensacionId:anul:$itemId"
}

fun movimientoReferenciaForDispensacionHeaderAnulacion(dispensacionId: String, monturaId: String): String {
    require(dispensacionId.isNotBlank()) { "dispensacion id must not be blank" }
    require(monturaId.isNotBlank()) { "montura id must not be blank" }
    return "$dispensacionId:anul:h:$monturaId"
}

fun movimientoReferenciaForRegaloAnulacion(regaloId: String): String {
    require(regaloId.isNotBlank()) { "regalo id must not be blank" }
    return "$regaloId:anul"
}

fun movimientoReferenciaForServicioItemAnulacion(itemId: String): String {
    require(itemId.isNotBlank()) { "servicio item id must not be blank" }
    return "$itemId:anul"
}
