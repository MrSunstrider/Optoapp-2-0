package com.example.optoapp.domain

import com.example.optoapp.data.pago.PagoDao
import javax.inject.Inject

class CalcularMontoPagadoUseCase @Inject constructor(
    private val pagoDao: PagoDao
) {
    suspend operator fun invoke(dispensacionId: String): Double {
        return pagoDao.sumMontoByDispensacion(dispensacionId, "Anulación")
    }
}
