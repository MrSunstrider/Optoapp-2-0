package com.example.optoapp.domain

/**
 * Marks operating result as incomplete when cost coverage is weak,
 * so the owner does not treat an under-costed month as a hard fact.
 */
object ResultadoOperativoConfiabilidad {
    const val UMBRAL_PCT_REAL = 80.0

    fun esIncompleto(cobertura: CoberturaCosto): Boolean =
        cobertura.sinDatos || cobertura.pctReal < UMBRAL_PCT_REAL
}
