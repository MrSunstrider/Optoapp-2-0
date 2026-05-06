package com.example.optoapp.domain.builder

import com.example.optoapp.domain.model.EvaluacionModel
import java.time.LocalDate
import java.util.UUID

class EvaluacionBuilder {
    private var evaluacion = EvaluacionModel(
        id = UUID.randomUUID().toString(),
        pacienteId = "",
        fecha = LocalDate.now(),
        opticaId = ""
    )

    fun id(id: String) = apply { evaluacion = evaluacion.copy(id = id) }
    fun pacienteId(id: String) = apply { evaluacion = evaluacion.copy(pacienteId = id) }
    fun fecha(fecha: LocalDate) = apply { evaluacion = evaluacion.copy(fecha = fecha) }
    fun opticaId(id: String) = apply { evaluacion = evaluacion.copy(opticaId = id) }

    fun refraccionOd(esf: String, cil: String, eje: String) = apply {
        val normalizedEsf = normalizeSphere(esf)
        val (nEsf, nCil, nEje) = transposeIfNeeded(normalizedEsf, cil, eje)
        evaluacion = evaluacion.copy(
            recetaOdEsf = nEsf,
            recetaOdCil = nCil,
            recetaOdEje = nEje
        )
        updateAutoDiagnosis()
    }

    fun refraccionOi(esf: String, cil: String, eje: String) = apply {
        val normalizedEsf = normalizeSphere(esf)
        val (nEsf, nCil, nEje) = transposeIfNeeded(normalizedEsf, cil, eje)
        evaluacion = evaluacion.copy(
            recetaOiEsf = nEsf,
            recetaOiCil = nCil,
            recetaOiEje = nEje
        )
        updateAutoDiagnosis()
    }

    fun add(addOd: String, addOi: String) = apply {
        evaluacion = evaluacion.copy(
            addCercaOd = addOd,
            addCercaOi = addOi
        )
        updateAutoDiagnosis()
    }

    fun agudezaVisual(od: String, oi: String) = apply {
        evaluacion = evaluacion.copy(
            recetaOdAv = od,
            recetaOiAv = oi
        )
        updateAutoDiagnosis()
    }

    fun build(): EvaluacionModel {
        validate()
        return evaluacion
    }

    private fun normalizeSphere(value: String): String {
        val v = value.trim().lowercase()
        return if (v == "plano" || v == "neutro" || v == "pl") "0.00" else value
    }

    private fun updateAutoDiagnosis() {
        // Solo automatizar si no están en modo manual
        if (evaluacion.autoPresbicia) {
            val hasAdd = evaluacion.addCercaOd.toDoubleOrNull() ?: 0.0 > 0.0 || 
                         evaluacion.addCercaOi.toDoubleOrNull() ?: 0.0 > 0.0
            evaluacion = evaluacion.copy(otrosPresbicia = hasAdd)
        }

        if (evaluacion.autoAnisometropia) {
            val eeOd = calculateEE(evaluacion.recetaOdEsf, evaluacion.recetaOdCil)
            val eeOi = calculateEE(evaluacion.recetaOiEsf, evaluacion.recetaOiCil)
            val diff = kotlin.math.abs(eeOd - eeOi)
            evaluacion = evaluacion.copy(otrosAnisometropia = diff >= 2.00)
        }

        // Ambliopía: Simplificado para el ejemplo (diferencia de líneas)
        // En una implementación real se usaría un mapa de valores LogMAR o líneas Snellen
    }

    private fun calculateEE(esf: String, cil: String): Double {
        val e = esf.toDoubleOrNull() ?: 0.0
        val c = cil.toDoubleOrNull() ?: 0.0
        return e + (c / 2.0)
    }

    private fun validate() {
        if (evaluacion.pacienteId.isBlank()) throw IllegalStateException("Paciente ID es obligatorio")
        if (evaluacion.opticaId.isBlank()) throw IllegalStateException("Optica ID es obligatorio")
    }

    /**
     * Lógica de transposición: convierte cilindro positivo a negativo.
     * Nueva Esfera = Esfera + Cilindro
     * Nuevo Cilindro = -Cilindro
     * Nuevo Eje = Eje + 90 (si > 180, restar 180)
     */
    private fun transposeIfNeeded(esf: String, cil: String, eje: String): Triple<String, String, String> {
        val c = cil.toDoubleOrNull() ?: 0.0
        if (c <= 0) return Triple(esf, cil, eje)

        val e = esf.toDoubleOrNull() ?: 0.0
        val a = eje.toIntOrNull() ?: 0

        val newEsf = e + c
        val newCil = -c
        var newEje = a + 90
        if (newEje > 180) newEje -= 180

        return Triple(
            String.format("%.2f", newEsf),
            String.format("%.2f", newCil),
            newEje.toString()
        )
    }
}
