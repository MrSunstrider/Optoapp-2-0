package com.example.optoapp.domain

object OrigenMontura {
    const val TIENDA = "Tienda"

    /** Label written by older app versions; rows with it still hold store stock. */
    const val TIENDA_LEGACY = "Nueva de Tienda"

    fun isTienda(origen: String): Boolean = origen.trim().let { it == TIENDA || it == TIENDA_LEGACY }
}
