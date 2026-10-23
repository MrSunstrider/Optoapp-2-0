package com.example.optoapp.domain

data class CumplimientoPlan(
    val hechas: Int,
    val enCurso: Int,
    val noAplica: Int,
    val abiertas: Int,
    val total: Int,
) {
    companion object {
        fun from(seguimientos: Map<String, SeguimientoEstado>): CumplimientoPlan {
            var hechas = 0
            var enCurso = 0
            var noAplica = 0
            for (estado in seguimientos.values) {
                when (estado) {
                    SeguimientoEstado.HECHO -> hechas++
                    SeguimientoEstado.EN_CURSO -> enCurso++
                    SeguimientoEstado.NO_APLICA -> noAplica++
                }
            }
            val total = hechas + enCurso + noAplica
            return CumplimientoPlan(
                hechas = hechas,
                enCurso = enCurso,
                noAplica = noAplica,
                abiertas = enCurso,
                total = total,
            )
        }
    }
}
