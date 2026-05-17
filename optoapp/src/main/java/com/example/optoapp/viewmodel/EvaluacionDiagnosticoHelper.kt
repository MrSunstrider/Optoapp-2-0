package com.example.optoapp.viewmodel

import java.util.Locale
import kotlin.math.abs

/**
 * Umbral de diferencia de equivalente esférico para diagnosticar anisometropía:
 * solo se considera si |EE(OD) − EE(OI)| ≥ 2.0 D
 */
const val ANISOMETROPIA_UMBRAL_DIOPTRIAS = 2.0

/** Pure logic: parse Snellen "20/XX" to logMAR. */
fun parseSnellenToLogMar(snellen: String): Double? {
    return try {
        val clean = snellen.trim()
        if (!clean.contains("/")) return null
        val parts = clean.split("/")
        if (parts.size != 2) return null
        val denominator = parts[1].trim().toDoubleOrNull() ?: return null
        if (denominator <= 0) return null
        val decimalAV = 20.0 / denominator
        -Math.log10(decimalAV)
    } catch (e: NumberFormatException) { return null }
}

fun parseRefraction(v: String): Double? {
    val clean = v.lowercase().trim()
    if (clean in listOf("plano", "neutro", "pl", "nt")) return 0.0
    return clean.replace(",", ".").toDoubleOrNull()
}

fun calcularDiagnostico(esferaStr: String, cilindroStr: String): String {
    val esf = parseRefraction(esferaStr)
    val cil = parseRefraction(cilindroStr)

    if (esf == null && cil == null) return ""

    var e = esf ?: 0.0
    var c = cil ?: 0.0

    if (c > 0) {
        e += c
        c = -c
    }

    val meridian1 = e
    val meridian2 = e + c

    return when {
        meridian1 == 0.0 && meridian2 == 0.0 -> "Emetropía"
        meridian1 < 0.0 && c == 0.0 -> "Miopía"
        meridian1 > 0.0 && c == 0.0 -> "Hipermetropía"
        (meridian1 == 0.0 && meridian2 < 0.0) || (meridian1 < 0.0 && meridian2 == 0.0) -> "Astigmatismo miópico simple"
        (meridian1 == 0.0 && meridian2 > 0.0) || (meridian1 > 0.0 && meridian2 == 0.0) -> "Astigmatismo hipermetrópico simple"
        meridian1 < 0.0 && meridian2 < 0.0 -> "Astigmatismo miópico compuesto"
        meridian1 > 0.0 && meridian2 > 0.0 -> "Astigmatismo hipermetrópico compuesto"
        (meridian1 > 0.0 && meridian2 < 0.0) || (meridian1 < 0.0 && meridian2 > 0.0) -> "Astigmatismo mixto"
        else -> "Astigmatismo mixto"
    }
}

fun computeDiagnosticoAuto(state: EvaluacionUiState): EvaluacionUiState {
    val isTextBalOd = sequenceOf(state.recetaOdEsf, state.recetaOdCil, state.recetaOdEje).any { it.trim().lowercase().contains("bal") }
    val isTextBalOi = sequenceOf(state.recetaOiEsf, state.recetaOiCil, state.recetaOiEje).any { it.trim().lowercase().contains("bal") }

    val effBalanceOd = state.balanceOd || isTextBalOd
    val effBalanceOi = state.balanceOi || isTextBalOi

    val hasDataOd = state.recetaOdEsf.trim().isNotEmpty() || state.recetaOdCil.trim().isNotEmpty()
    val hasDataOi = state.recetaOiEsf.trim().isNotEmpty() || state.recetaOiCil.trim().isNotEmpty()

    val diagOd = if (effBalanceOd) "Balance"
    else if (hasDataOd) calcularDiagnostico(state.recetaOdEsf, state.recetaOdCil)
    else ""

    val diagOi = if (effBalanceOi) "Balance"
    else if (hasDataOi) calcularDiagnostico(state.recetaOiEsf, state.recetaOiCil)
    else ""

    return state.copy(
        diagnosticoOd = if (diagOd.isNotEmpty()) listOf(diagOd) else emptyList(),
        diagnosticoOi = if (diagOi.isNotEmpty()) listOf(diagOi) else emptyList()
    )
}

fun computeOtrosAuto(state: EvaluacionUiState): EvaluacionUiState {
    val isTextBalOd = sequenceOf(state.recetaOdEsf, state.recetaOdCil, state.recetaOdEje).any { it.trim().lowercase().contains("bal") }
    val isTextBalOi = sequenceOf(state.recetaOiEsf, state.recetaOiCil, state.recetaOiEje).any { it.trim().lowercase().contains("bal") }

    val effBalanceOd = state.balanceOd || isTextBalOd
    val effBalanceOi = state.balanceOi || isTextBalOi

    val newAddOi = if (state.isAddAo) state.addCercaOd else state.addCercaOi

    val addValStr = if (state.addCercaOd.isNotEmpty()) state.addCercaOd else newAddOi
    val addVal = parseRefraction(addValStr) ?: 0.0
    val presbiciaVal = addVal > 0

    val eeOd = (parseRefraction(state.recetaOdEsf) ?: 0.0) + ((parseRefraction(state.recetaOdCil) ?: 0.0) / 2.0)
    val eeOi = (parseRefraction(state.recetaOiEsf) ?: 0.0) + ((parseRefraction(state.recetaOiCil) ?: 0.0) / 2.0)

    val anisometropiaVal = if (!effBalanceOd && !effBalanceOi &&
        state.recetaOdEsf.isNotEmpty() && state.recetaOiEsf.isNotEmpty()
    ) {
        abs(eeOd - eeOi) >= ANISOMETROPIA_UMBRAL_DIOPTRIAS
    } else {
        false
    }

    val logMarOd = parseSnellenToLogMar(state.avCcOdLejos)
    val logMarOi = parseSnellenToLogMar(state.avCcOiLejos)
    val ambliopiaVal = if (logMarOd != null && logMarOi != null) {
        abs(logMarOd - logMarOi) >= 0.19
    } else state.otrosAmbliopia

    return state.copy(
        addCercaOi = newAddOi,
        otrosPresbicia = if (state.autoPresbicia) presbiciaVal else state.otrosPresbicia,
        otrosAnisometropia = if (state.autoAnisometropia) anisometropiaVal else state.otrosAnisometropia,
        otrosAmbliopia = if (state.autoAmbliopia) ambliopiaVal else state.otrosAmbliopia
    )
}

fun normalizeAndTranspose(state: EvaluacionUiState, ojo: String): EvaluacionUiState {
    val esfStr = if (ojo == "OD") state.recetaOdEsf else state.recetaOiEsf
    val cilStr = if (ojo == "OD") state.recetaOdCil else state.recetaOiCil
    val ejeStr = if (ojo == "OD") state.recetaOdEje else state.recetaOiEje

    val eVal = parseRefraction(esfStr) ?: return state
    val cVal = parseRefraction(cilStr) ?: return state
    val ejeVal = ejeStr.toIntOrNull() ?: 0

    if (cVal > 0) {
        val newEsfNum = eVal + cVal
        val newCilNum = -cVal
        var newEjeNum = ejeVal + 90
        if (newEjeNum > 180) newEjeNum -= 180

        val newEsf = if (newEsfNum == 0.0) "plano" else "%.2f".format(Locale.US, newEsfNum)
        val newCil = "%.2f".format(Locale.US, newCilNum)
        val newEje = newEjeNum.toString()

        if (ojo == "OD") {
            return state.copy(recetaOdEsf = newEsf, recetaOdCil = newCil, recetaOdEje = newEje)
        } else {
            return state.copy(recetaOiEsf = newEsf, recetaOiCil = newCil, recetaOiEje = newEje)
        }
    }
    return state
}
