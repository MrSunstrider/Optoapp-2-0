package com.example.optoapp.domain

import com.example.optoapp.data.DatabaseTransactionRunner
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.data.costobiselado.CostoBiseladoDao
import com.example.optoapp.data.costobiselado.CostoBiseladoEntity
import com.example.optoapp.data.costolc.CostoLcDao
import com.example.optoapp.data.costolc.CostoLcEntity
import com.example.optoapp.data.costoproducto.CostoProductoDao
import com.example.optoapp.data.costoproducto.CostoProductoEntity
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

sealed interface ResultadoVigencia {
    data class Aplicado(val nuevoId: String) : ResultadoVigencia
    data class Rechazado(val fechaMinima: LocalDate) : ResultadoVigencia
    data object YaNoVigente : ResultadoVigencia
}

/**
 * A price edit never rewrites a row: OTs already priced with it must keep resolving the old price by
 * date, so the open row is closed the day before the new one starts. Both rows are marked for upload
 * because the closing itself is an edit other devices must receive.
 */
class MatrizVigenciaUseCase @Inject constructor(
    private val costoProductoDao: CostoProductoDao,
    private val costoBiseladoDao: CostoBiseladoDao,
    private val costoLcDao: CostoLcDao,
    private val syncStateTracker: SyncStateTracker,
    private val transactionRunner: DatabaseTransactionRunner,
) {

    suspend fun editarProducto(previo: CostoProductoEntity, costo: Double, desde: LocalDate, hoy: LocalDate) =
        versionar(
            Version(previo.opticaId, previo.id, previo.vigenteDesde, TIPO_PRODUCTO), desde, hoy,
            leerVigente = { costoProductoDao.getById(previo.id)?.takeIf { it.vigenteHasta == null } },
            cerrar = { actual, hasta, ts -> costoProductoDao.upsertAll(listOf(actual.copy(vigenteHasta = hasta, updatedAt = ts))) },
            abrir = { actual, id, ts ->
                costoProductoDao.upsertAll(listOf(actual.copy(id = id, costoUnitario = costo, vigenteDesde = desde.toString(), updatedAt = ts)))
            },
        )

    suspend fun editarBiselado(previo: CostoBiseladoEntity, costoPorPar: Double, desde: LocalDate, hoy: LocalDate) =
        versionar(
            Version(previo.opticaId, previo.id, previo.vigenteDesde, TIPO_BISELADO), desde, hoy,
            leerVigente = { costoBiseladoDao.getById(previo.id)?.takeIf { it.vigenteHasta == null } },
            cerrar = { actual, hasta, ts -> costoBiseladoDao.upsertAll(listOf(actual.copy(vigenteHasta = hasta, updatedAt = ts))) },
            abrir = { actual, id, ts ->
                costoBiseladoDao.upsertAll(listOf(actual.copy(id = id, costoPorPar = costoPorPar, vigenteDesde = desde.toString(), updatedAt = ts)))
            },
        )

    suspend fun editarLc(previo: CostoLcEntity, costo: Double, desde: LocalDate, hoy: LocalDate) =
        versionar(
            Version(previo.opticaId, previo.id, previo.vigenteDesde, TIPO_LC), desde, hoy,
            leerVigente = { costoLcDao.getById(previo.id)?.takeIf { it.vigenteHasta == null } },
            cerrar = { actual, hasta, ts -> costoLcDao.upsertAll(listOf(actual.copy(vigenteHasta = hasta, updatedAt = ts))) },
            abrir = { actual, id, ts ->
                costoLcDao.upsertAll(listOf(actual.copy(id = id, costoUnitario = costo, vigenteDesde = desde.toString(), updatedAt = ts)))
            },
        )

    private data class Version(val opticaId: String, val id: String, val vigenteDesde: String, val tipo: String)

    private suspend fun <T> versionar(
        previo: Version,
        desde: LocalDate,
        hoy: LocalDate,
        leerVigente: suspend () -> T?,
        cerrar: suspend (actual: T, hasta: String, updatedAt: String) -> Unit,
        abrir: suspend (actual: T, id: String, updatedAt: String) -> Unit,
    ): ResultadoVigencia {
        val minima = fechaMinima(previo.vigenteDesde, hoy)
        if (desde < minima) return ResultadoVigencia.Rechazado(minima)
        return transactionRunner.inTransaction {
            val actual = leerVigente() ?: return@inTransaction ResultadoVigencia.YaNoVigente
            val nuevoId = UUID.randomUUID().toString()
            val ahora = Instant.now().toString()
            cerrar(actual, desde.minusDays(1).toString(), ahora)
            abrir(actual, nuevoId, ahora)
            syncStateTracker.markLocallyModified(previo.opticaId, previo.tipo, previo.id)
            syncStateTracker.markLocallyModified(previo.opticaId, previo.tipo, nuevoId)
            ResultadoVigencia.Aplicado(nuevoId)
        }
    }

    companion object {
        private const val TIPO_PRODUCTO = "costo_producto"
        private const val TIPO_BISELADO = "costo_biselado"
        private const val TIPO_LC = "costo_lc"

        // Starting on the previous start day would leave a closed row that ends before it begins.
        fun fechaMinima(previoDesde: String, hoy: LocalDate): LocalDate =
            maxOf(hoy, LocalDate.parse(previoDesde).plusDays(1))
    }
}
