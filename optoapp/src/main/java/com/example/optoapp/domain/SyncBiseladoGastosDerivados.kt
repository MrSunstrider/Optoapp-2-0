package com.example.optoapp.domain

import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.gastooperativo.GastoOperativoDao
import com.example.optoapp.data.gastooperativo.GastoOperativoEntity
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Upserts linked gastos for Tercero bevel COGS; removes them for Propio / zero / deleted items.
 * Linked rows stay visible in Gastos UI but are filtered out of analisis neto.
 *
 * Callers must pass [deleteLinked] / [upsertLinked] that go through OptoRepository
 * (tombstone + sync schedule). Raw DAO delete leaves remote rows that resurrect on download.
 */
object SyncBiseladoGastosDerivados {

    suspend fun sync(
        gastoOperativoDao: GastoOperativoDao,
        opticaId: String,
        ot: String,
        fecha: LocalDate,
        items: List<DispensacionItem>,
        deletedItemIds: List<String>,
        deleteLinked: suspend (GastoOperativoEntity) -> Unit,
        upsertLinked: suspend (GastoOperativoEntity) -> Unit,
    ) {
        deletedItemIds.forEach { itemId ->
            gastoOperativoDao.getByOrigenDispensacionItemId(opticaId, itemId)?.let { deleteLinked(it) }
        }
        items.forEach { item ->
            val existing = gastoOperativoDao.getByOrigenDispensacionItemId(opticaId, item.id)
            val monto = item.costoRealBiselado ?: 0.0
            val debeRegistrar = OrigenBiselado.isTercero(item.origenBiselado) && monto > 0.0
            if (!debeRegistrar) {
                existing?.let { deleteLinked(it) }
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
            upsertLinked(gasto)
        }
    }
}
