package com.example.optoapp.domain

enum class ClaseCobertura { REAL, ESTIMADO, FALTANTE }

/**
 * Kotlin twin of app_private.cobertura_clase_item / cobertura_clase_servicio. Both sides are
 * tested against the same vectors so the app and the server never disagree on coverage.
 */
object CoberturaCostoClassifier {

    private const val ORIGEN_FALTANTE = "faltante"
    private val ORIGENES_REALES = setOf("manual", "matriz", "mixto")

    // faltante is evaluated first so a partial cost can never promote an item the capture flagged as missing.
    fun claseItem(costoOrigen: String?, costoTotal: Double?): ClaseCobertura = when {
        costoOrigen == ORIGEN_FALTANTE -> ClaseCobertura.FALTANTE
        costoOrigen in ORIGENES_REALES -> ClaseCobertura.REAL
        costoOrigen == null && (costoTotal ?: 0.0) > 0.0 -> ClaseCobertura.ESTIMADO
        else -> ClaseCobertura.FALTANTE
    }

    fun claseServicio(realizadoPor: String?, costoReal: Double?, servicioTipoId: String?): ClaseCobertura =
        if (realizadoPor != null && ((costoReal ?: 0.0) > 0.0 || servicioTipoId != null)) {
            ClaseCobertura.REAL
        } else {
            ClaseCobertura.FALTANTE
        }
}
