package com.example.optoapp.domain

/** Who performs the bevel for an OT product line. */
object OrigenBiselado {
    const val PROPIO = "Propio"
    const val TERCERO = "Tercero"

    val OPTIONS = listOf(PROPIO, TERCERO)

    fun normalize(raw: String?): String = when (raw?.trim()) {
        PROPIO -> PROPIO
        else -> TERCERO
    }

    fun isPropio(raw: String?): Boolean = normalize(raw) == PROPIO

    fun isTercero(raw: String?): Boolean = normalize(raw) == TERCERO
}
