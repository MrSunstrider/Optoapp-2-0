package com.example.optoapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import java.math.BigDecimal
import java.math.RoundingMode

enum class SaldoEstado { PENDIENTE, SALDADO }

// Judged at the displayed cent, so floating residue printed as "S/ 0.00" never shows as owed.
fun saldoEstado(saldo: Double): SaldoEstado =
    if (BigDecimal.valueOf(saldo).setScale(2, RoundingMode.HALF_UP).signum() > 0) SaldoEstado.PENDIENTE else SaldoEstado.SALDADO

@Composable
fun saldoColor(saldo: Double): Color = when (saldoEstado(saldo)) {
    SaldoEstado.PENDIENTE -> MaterialTheme.colorScheme.error
    SaldoEstado.SALDADO -> MaterialTheme.colorScheme.positiveGreen
}
