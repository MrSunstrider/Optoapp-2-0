package com.example.optoapp.domain.inventario

/**
 * Distinguishes frame stock from sellable accessories sharing the monturas table.
 * Persisted in [com.example.optoapp.data.Montura.categoria] as [ACCESORIO].
 */
object InventarioItemKind {
    const val MONTURA = "MONTURA"
    const val ACCESORIO = "ACCESORIO"

    fun isAccesorio(categoria: String?): Boolean =
        !categoria.isNullOrBlank() && categoria.equals(ACCESORIO, ignoreCase = true)

    fun isArmazon(categoria: String?): Boolean = !isAccesorio(categoria)

    fun tipoItemFromCategoria(categoria: String?): String =
        if (isAccesorio(categoria)) ACCESORIO else MONTURA

    fun categoriaForSave(tipoItem: String, categoriaCatalogo: String): String =
        if (tipoItem.equals(ACCESORIO, ignoreCase = true)) {
            ACCESORIO
        } else {
            categoriaCatalogo.trim().takeUnless { isAccesorio(it) }.orEmpty()
        }
}
