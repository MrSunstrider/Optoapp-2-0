package com.example.optoapp.domain

import com.example.optoapp.data.SyncStateTracker
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.abs

data class CostoFechado(val fecha: LocalDate, val costo: Double)

// Must run inside the save transaction: a rolled-back save must not leave a marker behind.
class ResumenRecalcMarker @Inject constructor(
    private val syncStateTracker: SyncStateTracker,
) {
    suspend fun registrar(
        opticaId: String,
        causeType: String,
        causeId: String,
        anterior: CostoFechado?,
        nuevo: CostoFechado,
        hoy: LocalDate = LocalDate.now(),
    ) {
        fechasAfectadas(anterior, nuevo, hoy).forEach { fecha ->
            syncStateTracker.markPendingRecalc(opticaId, fecha, causeType, causeId)
        }
    }

    companion object {
        // Items and gifts are always saved through their parent, so only parent tracking types can be causes.
        const val CAUSA_DISPENSACION = "dispensacion"
        const val CAUSA_SERVICIO_EXTRA = "servicio_extra"
        private const val TOLERANCIA_COSTO = 0.005

        // Today is excluded: every finanzas sync already recalculates the current day.
        fun fechasAfectadas(anterior: CostoFechado?, nuevo: CostoFechado, hoy: LocalDate): List<LocalDate> {
            val cambio = anterior == null ||
                anterior.fecha != nuevo.fecha ||
                abs(anterior.costo - nuevo.costo) > TOLERANCIA_COSTO
            if (!cambio) return emptyList()
            return listOfNotNull(anterior?.fecha, nuevo.fecha).distinct().filter { it < hoy }
        }
    }
}
