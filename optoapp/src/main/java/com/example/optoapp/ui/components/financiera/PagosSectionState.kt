package com.example.optoapp.ui.components.financiera

import com.example.optoapp.data.Pago
import com.example.optoapp.domain.PagoEffect

data class PagosSectionState(
    val montoTotal: Double,
    val pagos: List<Pago>,
) {
    val pagado: Double
        get() = pagos.sumOf { PagoEffect.signedAmount(it.tipo, it.monto) }

    val saldo: Double
        get() = montoTotal - pagado

    fun montoMaximoParaNuevo(): Double = saldo.coerceAtLeast(0.0)

    fun montoMaximoParaEdicion(pagoId: String): Double {
        val otros = pagos
            .filter { it.id != pagoId }
            .sumOf { PagoEffect.signedAmount(it.tipo, it.monto) }
        return (montoTotal - otros).coerceAtLeast(0.0)
    }
}
