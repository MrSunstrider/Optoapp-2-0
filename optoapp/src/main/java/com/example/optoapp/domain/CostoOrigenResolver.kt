package com.example.optoapp.domain

enum class OrigenComponente { MANUAL, MATRIZ, FALTANTE, PRESERVADA }

object CostoOrigenResolver {
    const val MANUAL = "manual"
    const val MATRIZ = "matriz"
    const val MIXTO = "mixto"
    const val FALTANTE = "faltante"

    fun resolver(componentes: Collection<OrigenComponente>, origenPrevio: String?): String {
        val origenes = componentes.map { origen(it, origenPrevio) }
        return when {
            origenes.isEmpty() -> FALTANTE
            // Partial gaps stay mixto so a successful OD/OI matrix hit is not wiped by a missing bevel/montura.
            origenes.all { it == FALTANTE } -> FALTANTE
            origenes.all { it == MANUAL } -> MANUAL
            origenes.all { it == MATRIZ } -> MATRIZ
            else -> MIXTO
        }
    }

    // A preserved value of a mixto, faltante or legacy item cannot be attributed to one source.
    private fun origen(componente: OrigenComponente, origenPrevio: String?): String = when (componente) {
        OrigenComponente.MANUAL -> MANUAL
        OrigenComponente.MATRIZ -> MATRIZ
        OrigenComponente.FALTANTE -> FALTANTE
        OrigenComponente.PRESERVADA -> when (origenPrevio) {
            MANUAL -> MANUAL
            MATRIZ -> MATRIZ
            else -> MIXTO
        }
    }
}
