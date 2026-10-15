package com.example.optoapp.domain

/**
 * Mirrors `app_private.gasto_tipo_efectivo` so a month splits fijo/variable identically online and
 * offline. Legacy gastos carry no explicit type, so the category decides.
 */
object GastoTipoResolver {
    const val FIJO = "fijo"
    const val VARIABLE = "variable"

    private val CATEGORIAS_FIJAS = setOf("alquiler", "servicios", "personal", "impuestos")

    fun resolver(categoria: String, tipoGasto: String?): String =
        tipoGasto?.takeIf { it == FIJO || it == VARIABLE }
            ?: if (categoria in CATEGORIAS_FIJAS) FIJO else VARIABLE
}
