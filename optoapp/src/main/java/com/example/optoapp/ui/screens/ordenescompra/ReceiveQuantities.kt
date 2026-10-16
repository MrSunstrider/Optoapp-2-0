package com.example.optoapp.ui.screens.ordenescompra

object ReceiveQuantities {

    data class Line(val id: String, val cantidad: Int)

    fun clamp(value: Int, cantidad: Int, minRecibido: Int = 0): Int {
        val max = cantidad.coerceAtLeast(0)
        val min = minRecibido.coerceIn(0, max)
        return value.coerceIn(min, max)
    }

    fun fillPending(items: List<Line>): Map<String, Int> =
        items.associate { it.id to it.cantidad.coerceAtLeast(0) }

    fun parseQuantity(raw: String, cantidad: Int, minRecibido: Int = 0): Int {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return clamp(minRecibido, cantidad, minRecibido)
        val parsed = trimmed.toIntOrNull() ?: return clamp(minRecibido, cantidad, minRecibido)
        return clamp(parsed, cantidad, minRecibido)
    }

    fun toAbsoluteMap(
        textById: Map<String, String>,
        cantidadById: Map<String, Int>,
        minRecibidoById: Map<String, Int> = emptyMap(),
    ): Map<String, Int> =
        cantidadById.mapValues { (id, cantidad) ->
            parseQuantity(textById[id].orEmpty(), cantidad, minRecibidoById[id] ?: 0)
        }
}
