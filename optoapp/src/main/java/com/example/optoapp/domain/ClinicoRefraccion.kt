package com.example.optoapp.domain

import com.example.optoapp.data.EvaluacionClinica

object ClinicoRefraccion {
    const val EMETROPIA = "Emetropía"
    const val MIOPIA = "Miopía"
    const val HIPERMETROPIA = "Hipermetropía"
    const val ASTIGMATISMO = "Astigmatismo"
    const val SIN_DATO = "Sin dato"

    fun grupo(evaluacion: EvaluacionClinica): String {
        val pairs = listOf(
            evaluacion.recetaOdEsf to evaluacion.recetaOdCil,
            evaluacion.recetaOiEsf to evaluacion.recetaOiCil,
            evaluacion.subjOdEsf to evaluacion.subjOdCil,
            evaluacion.subjOiEsf to evaluacion.subjOiCil,
            evaluacion.objOdEsf to evaluacion.objOdCil,
            evaluacion.objOiEsf to evaluacion.objOiCil,
        )
        for ((esf, cil) in pairs) {
            val fine = DiagnosticoCalculator.calcularDiagnostico(esf.orEmpty(), cil.orEmpty())
            if (fine.isNotBlank()) return coarsen(fine)
        }
        return SIN_DATO
    }

    fun coarsen(fineLabel: String): String {
        val d = fineLabel.trim()
        if (d.isBlank()) return SIN_DATO
        val folded = foldAccents(d)
        if (folded.equals("emetropia", ignoreCase = true)) return EMETROPIA
        // WHY: keep "Astigmatismo miópico*" under Miopía so filters/charts match prior behavior.
        if (folded.equals("miopia", ignoreCase = true) || folded.contains("miop", ignoreCase = true)) return MIOPIA
        if (folded.equals("hipermetropia", ignoreCase = true) || folded.contains("hipermet", ignoreCase = true)) {
            return HIPERMETROPIA
        }
        if (folded.startsWith("astigmatismo", ignoreCase = true)) return ASTIGMATISMO
        return d
    }

    // WHY: OD/OI spelling variants were splitting Miopía into multiple ranking rows.
    fun normalizeDiagnosticoLabel(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return trimmed
        val withoutEye = trimmed
            .replace(Regex("""\s*\((?i:od|oi|ao)\)\s*$"""), "")
            .replace(Regex("""\s+(?i:od|oi|ao)$"""), "")
            .trim()
        val coarse = coarsen(withoutEye)
        return if (coarse == SIN_DATO) withoutEye.ifBlank { trimmed } else coarse
    }

    private fun foldAccents(value: String): String {
        val normalized = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{M}+"), "")
    }
}

enum class ClinicoSexoFiltro { TODOS, HOMBRES, MUJERES, SIN_DATO }

enum class ClinicoEdadFiltro { TODOS, PEDIATRICO, ADULTO, ADULTO_MAYOR }

enum class ClinicoEmetropiaFiltro { TODOS, EMETROPIA, MIOPIA, HIPERMETROPIA, ASTIGMATISMO, SIN_DATO }

data class ClinicoFiltros(
    val sexo: ClinicoSexoFiltro = ClinicoSexoFiltro.TODOS,
    val edad: ClinicoEdadFiltro = ClinicoEdadFiltro.TODOS,
    val emetropia: ClinicoEmetropiaFiltro = ClinicoEmetropiaFiltro.TODOS,
)
