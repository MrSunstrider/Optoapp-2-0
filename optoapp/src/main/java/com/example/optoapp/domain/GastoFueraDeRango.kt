package com.example.optoapp.domain

data class GastoAlerta(
    val activa: Boolean,
    val ratioPct: Double,
    val gastosMes: Double,
    val ventasMes: Double,
)

/** Shared opex/sales gate so the home alert and the action-plan rule cannot drift. */
object GastoFueraDeRango {
    const val RATIO_ALERTA_DEFAULT = 0.4

    fun estaFueraDeRango(ratio: Double, umbral: Double = RATIO_ALERTA_DEFAULT): Boolean = ratio > umbral

    fun evaluar(
        ventasMes: Double,
        gastosMes: Double,
        umbral: Double = RATIO_ALERTA_DEFAULT,
    ): GastoAlerta? {
        if (ventasMes <= 0.0) return null
        val ratio = gastosMes / ventasMes
        if (!estaFueraDeRango(ratio, umbral)) return null
        return GastoAlerta(
            activa = true,
            ratioPct = ratio * 100.0,
            gastosMes = gastosMes,
            ventasMes = ventasMes,
        )
    }
}
