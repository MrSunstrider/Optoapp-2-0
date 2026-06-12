package com.example.optoapp.viewmodel

import com.example.optoapp.viewmodel.diagnostico.DiagnosticoCalculator
import java.util.Locale
import kotlin.math.abs

/**
 * Umbral de diferencia de equivalente esférico para diagnosticar anisometropía:
 * solo se considera si |EE(OD) − EE(OI)| ≥ 2.0 D
 */
const val ANISOMETROPIA_UMBRAL_DIOPTRIAS = 2.0

fun computeDiagnosticoAuto(state: EvaluacionUiState): EvaluacionUiState {
    val isTextBalOd = sequenceOf(state.recetaOdEsf, state.recetaOdCil, state.recetaOdEje).any { it.trim().lowercase().contains("bal") }
    val isTextBalOi = sequenceOf(state.recetaOiEsf, state.recetaOiCil, state.recetaOiEje).any { it.trim().lowercase().contains("bal") }

    val effBalanceOd = state.balanceOd || isTextBalOd
    val effBalanceOi = state.balanceOi || isTextBalOi

    val hasDataOd = state.recetaOdEsf.trim().isNotEmpty() || state.recetaOdCil.trim().isNotEmpty()
    val hasDataOi = state.recetaOiEsf.trim().isNotEmpty() || state.recetaOiCil.trim().isNotEmpty()

    val diagOd = if (effBalanceOd) "Balance"
    else if (hasDataOd) DiagnosticoCalculator.calcularDiagnostico(state.recetaOdEsf, state.recetaOdCil)
    else ""

    val diagOi = if (effBalanceOi) "Balance"
    else if (hasDataOi) DiagnosticoCalculator.calcularDiagnostico(state.recetaOiEsf, state.recetaOiCil)
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

    val newAddCercaOi = if (state.isAddAo) state.addCercaOd else state.addCercaOi
    val newAddIntermediaOi = if (state.isAddAo) state.addIntermediaOd else state.addIntermediaOi

    val presbiciaVal = sequenceOf(
        state.addCercaOd, newAddCercaOi,
        state.addIntermediaOd, newAddIntermediaOi
    ).any { (DiagnosticoCalculator.parseRefraction(it) ?: 0.0) > 0 }

    val eeOd = (DiagnosticoCalculator.parseRefraction(state.recetaOdEsf) ?: 0.0) + ((DiagnosticoCalculator.parseRefraction(state.recetaOdCil) ?: 0.0) / 2.0)
    val eeOi = (DiagnosticoCalculator.parseRefraction(state.recetaOiEsf) ?: 0.0) + ((DiagnosticoCalculator.parseRefraction(state.recetaOiCil) ?: 0.0) / 2.0)

    val anisometropiaVal = if (!effBalanceOd && !effBalanceOi &&
        state.recetaOdEsf.isNotEmpty() && state.recetaOiEsf.isNotEmpty()
    ) {
        abs(eeOd - eeOi) >= ANISOMETROPIA_UMBRAL_DIOPTRIAS
    } else {
        false
    }

    val logMarOd = DiagnosticoCalculator.parseSnellenToLogMar(state.avCcOdLejos)
    val logMarOi = DiagnosticoCalculator.parseSnellenToLogMar(state.avCcOiLejos)
    val ambliopiaVal = if (logMarOd != null && logMarOi != null) {
        abs(logMarOd - logMarOi) >= 0.19
    } else state.otrosAmbliopia

    return state.copy(
        addCercaOi = newAddCercaOi,
        addIntermediaOi = newAddIntermediaOi,
        otrosPresbicia = if (state.autoPresbicia) presbiciaVal else state.otrosPresbicia,
        otrosAnisometropia = if (state.autoAnisometropia) anisometropiaVal else state.otrosAnisometropia,
        otrosAmbliopia = if (state.autoAmbliopia) ambliopiaVal else state.otrosAmbliopia
    )
}

fun normalizeAndTranspose(state: EvaluacionUiState, ojo: String): EvaluacionUiState {
    val esfStr = if (ojo == "OD") state.recetaOdEsf else state.recetaOiEsf
    val cilStr = if (ojo == "OD") state.recetaOdCil else state.recetaOiCil
    val ejeStr = if (ojo == "OD") state.recetaOdEje else state.recetaOiEje

    val eVal = DiagnosticoCalculator.parseRefraction(esfStr) ?: return state
    val cVal = DiagnosticoCalculator.parseRefraction(cilStr) ?: return state
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
