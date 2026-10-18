package com.example.optoapp.domain

/** Which eyes are dispensed on an OT product line (monocular orders are common). */
object OjosLente {
    const val AMBOS = "Ambos"
    const val OD = "OD"
    const val OI = "OI"

    val OPTIONS = listOf(AMBOS, OD, OI)

    fun includesOd(ojosLente: String): Boolean = ojosLente != OI

    fun includesOi(ojosLente: String): Boolean = ojosLente != OD

    fun normalize(raw: String?): String = when (raw?.trim()) {
        OD -> OD
        OI -> OI
        else -> AMBOS
    }
}
