package com.example.optoapp.domain

import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.gastooperativo.GastoOperativoDao
import com.example.optoapp.data.gastooperativo.GastoOperativoEntity
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Upserts linked gastos for Tercero bevel COGS; removes them for Propio / zero / deleted items.
 * Linked rows stay visible in Gastos UI but are filtered out of analisis neto.
 */
object SyncBiseladoGastosDerivados {

    suspend fun sync(
        gastoOperativoDao: GastoOperativoDao,
        opticaId: String,
        ot: String,
        fecha: LocalDate,
        items: List<DispensacionItem>,
        deletedItemIds: List<String>,
    ) {
        deletedItemIds.forEach { itemId ->
            gastoOperativoDao.getByOrigenDispensacionItemId(opticaId, itemId)?.let {
                gastoOperativoDao.delete(it.id, opticaId)
            }
        }
        items.forEach { item ->
            val existing = gastoOperativoDao.getByOrigenDispensacionItemId(opticaId, item.id)
            val monto = item.costoRealBiselado ?: 0.0
            val debeRegistrar = OrigenBiselado.isTercero(item.origenBiselado) && monto > 0.0
            if (!debeRegistrar) {
                existing?.let { gastoOperativoDao.delete(it.id, opticaId) }
                return@forEach
            }
            val gasto = GastoOperativoEntity(
                id = existing?.id ?: "gb_${item.id}",
                opticaId = opticaId,
                categoria = "Biselado",
                descripcion = "Biselado OT $ot",
                monto = BigDecimal.valueOf(monto),
                fecha = fecha,
                tipoGasto = GastoTipoResolver.VARIABLE,
                origenDispensacionItemId = item.id,
                createdAt = existing?.createdAt,
            )
            gastoOperativoDao.upsert(gasto)
        }
    }
}
