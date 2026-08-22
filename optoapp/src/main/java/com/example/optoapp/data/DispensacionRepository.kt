package com.example.optoapp.data

import android.util.Log
import com.example.optoapp.data.pago.PagoDao
import com.example.optoapp.data.servicio.ServicioExtraDao
import com.example.optoapp.util.DateUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import java.io.IOException
import java.time.LocalDate
import java.util.UUID

class DispensacionRepository(
    private val dispensacionDao: DispensacionDao,
    private val dispensacionItemDao: DispensacionItemDao,
    private val pagoDao: PagoDao,
    private val servicioExtraDao: ServicioExtraDao,
) {

    fun getDispensacionesByDateRangeForOptica(start: LocalDate, end: LocalDate, opticaId: String): Flow<List<DispensacionOptica>> = dispensacionDao.getDispensacionesByDateRangeForOptica(start, end, opticaId)

    fun getDispensacionesByPaciente(pacienteId: String, opticaId: String): Flow<List<DispensacionOptica>> = dispensacionDao.getDispensacionesByPaciente(pacienteId, opticaId)

    fun getAllDispensacionesForOptica(opticaId: String): Flow<List<DispensacionOptica>> = dispensacionDao.getAllDispensacionesForOptica(opticaId)

    fun getTotalVendidoForOptica(opticaId: String): Flow<Double?> = dispensacionDao.getTotalVendidoForOptica(opticaId)

    fun getTotalPagadoForOptica(opticaId: String): Flow<Double?> = dispensacionDao.getTotalPagadoForOptica(opticaId)

    suspend fun getDispensacionById(id: String, opticaId: String): Resource<DispensacionOptica> = try {
        val dispensacion = dispensacionDao.getDispensacionById(id, opticaId)
        if (dispensacion != null) {
            Resource.Success(dispensacion)
        } else {
            Resource.Error("Dispensación no encontrada")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Log.e(TAG, "getDispensacionById: id=$id", e)
        Resource.Error("Error de red al obtener dispensación")
    } catch (e: Exception) {
        Log.e(TAG, "getDispensacionById: id=$id", e)
        Resource.Error(e.message ?: "Error al obtener dispensación")
    }

    suspend fun getLastDispensacionByPacienteId(pacienteId: String, opticaId: String): Resource<DispensacionOptica> = try {
        val disp = dispensacionDao.getLastDispensacionByPacienteId(pacienteId, opticaId)
        if (disp != null) {
            Resource.Success(disp)
        } else {
            Resource.Error("No hay dispensaciones")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Log.e(TAG, "getLastDispensacionByPacienteId: pacienteId=$pacienteId", e)
        Resource.Error("Error de red al obtener dispensación")
    } catch (e: Exception) {
        Log.e(TAG, "getLastDispensacionByPacienteId: pacienteId=$pacienteId", e)
        Resource.Error(e.message ?: "Error al obtener dispensación")
    }

    suspend fun insertDispensacion(dispensacion: DispensacionOptica) {
        dispensacionDao.insertDispensacion(dispensacion)
    }

    suspend fun updateDispensacion(dispensacion: DispensacionOptica) {
        dispensacionDao.updateDispensacion(dispensacion)
    }

    suspend fun deleteDispensacionById(id: String, opticaId: String): Int = dispensacionDao.deleteById(id, opticaId)

    suspend fun getDispensacionesSnapshotForOptica(opticaId: String): List<DispensacionOptica> = dispensacionDao.getDispensacionesListByOptica(opticaId)

    suspend fun suggestNextOt(opticaId: String, fecha: LocalDate): String {
        val year = fecha.year.toString()
        val ots = dispensacionDao.getOtsWithYearPrefix(opticaId, year)
        val regex = Regex("^OT-$year-(\\d+)$", RegexOption.IGNORE_CASE)
        var max = 0
        for (ot in ots) {
            regex.find(ot.trim())?.groupValues?.get(1)?.toIntOrNull()?.let { if (it > max) max = it }
        }
        val next = max + 1
        return "OT-$year-" + next.toString().padStart(4, '0')
    }

    suspend fun reassignFromLegacyMiOpticaBase(currentOpticaId: String): Int {
        val d = dispensacionDao.reassignFromLegacyMiOpticaBase(currentOpticaId)
        val s = servicioExtraDao.reassignFromLegacyMiOpticaBase(currentOpticaId)
        val pg = pagoDao.reassignFromLegacyMiOpticaBase(currentOpticaId)
        return d + s + pg
    }

    fun getItemsByDispensacion(dispensacionId: String): Flow<List<DispensacionItem>> = dispensacionItemDao.getItemsByDispensacion(dispensacionId)

    suspend fun getItemsListByDispensacion(dispensacionId: String): List<DispensacionItem> = dispensacionItemDao.getItemsListByDispensacion(dispensacionId)

    suspend fun getItemsListByOptica(opticaId: String): List<DispensacionItem> = dispensacionItemDao.getItemsListByOptica(opticaId)

    suspend fun getDispensacionItemById(id: String, opticaId: String): DispensacionItem? =
        dispensacionItemDao.getById(id, opticaId)

    suspend fun insertDispensacionItem(item: DispensacionItem) {
        dispensacionItemDao.insertItem(item)
    }

    suspend fun deleteDispensacionItemById(id: String, opticaId: String): Int = dispensacionItemDao.deleteById(id, opticaId)

    suspend fun deleteItemsByDispensacionId(dispensacionId: String, opticaId: String): Int = dispensacionItemDao.deleteByDispensacionId(dispensacionId, opticaId)

    @Suppress("DEPRECATION")
    @Deprecated(
        message = "Use getItemsListByOptica to enforce multi-tenant isolation",
        replaceWith = ReplaceWith("getItemsListByOptica(opticaId)"),
    )
    suspend fun getAllDispensacionItems(): List<DispensacionItem> = dispensacionItemDao.getAllItems()

    fun getPagosByDispensacion(dispensacionId: String): Flow<List<Pago>> = pagoDao.getPagosByDispensacion(dispensacionId)

    suspend fun insertPago(pago: Pago) {
        pagoDao.insertPago(pago)
    }

    suspend fun updatePago(pago: Pago) {
        pagoDao.updatePago(
            id = pago.id, opticaId = pago.opticaId,
            dispensacionId = pago.dispensacionId,
            servicioExtraId = pago.servicioExtraId, fecha = pago.fecha,
            tipo = pago.tipo, monto = pago.monto,
            metodoPago = pago.metodoPago, nota = pago.nota,
            updatedAt = pago.updatedAt, updatedBy = pago.updatedBy,
            ventaId = pago.ventaId,
            reversaPagoId = pago.reversaPagoId,
        )
    }

    suspend fun getPagoById(id: String, opticaId: String): Pago? = pagoDao.getPagoByIdForOptica(id, opticaId)

    suspend fun reassignPagosDispensacion(oldDispensacionId: String, newDispensacionId: String, opticaId: String): Int = pagoDao.reassignDispensacionIdForOptica(oldDispensacionId, newDispensacionId, opticaId)

    suspend fun reassignItemsDispensacion(sourceId: String, targetId: String, opticaId: String): Int = dispensacionItemDao.reassignItemsDispensacion(sourceId, targetId, opticaId)

    suspend fun deletePagoRegistrandoAnulacionEnCaja(
        pago: Pago,
        opticaId: String,
    ) {
        val existing = pagoDao.getPagoByIdForOptica(pago.id, opticaId) ?: return
        if (existing.monto == 0.0) return
        if (existing.tipo !in setOf("Abono", "Pago completo")) return
        if (pagoDao.getReversoByOriginalId(existing.id) != null) return
        val reversal = Pago(
            id = UUID.randomUUID().toString(),
            dispensacionId = existing.dispensacionId,
            servicioExtraId = existing.servicioExtraId,
            ventaId = existing.ventaId,
            fecha = existing.fecha,
            tipo = "Reverso",
            monto = existing.monto,
            metodoPago = existing.metodoPago,
            nota = "Reverso abono ${existing.id.take(8)}… (${DateUtils.formatLocalized(existing.fecha)})",
            opticaId = opticaId,
            reversaPagoId = existing.id,
            updatedAt = java.time.Instant.now().toString(),
        )
        pagoDao.insertPago(reversal)
    }

    suspend fun deletePago(pago: Pago) = pagoDao.deletePago(pago.id, pago.opticaId)

    fun getPagosByServicioExtra(servicioExtraId: String): Flow<List<Pago>> = pagoDao.getPagosByServicioExtra(servicioExtraId)

    @Suppress("DEPRECATION")
    @Deprecated(
        message = "Use getPagosByDateRangeForOptica to enforce multi-tenant isolation",
        replaceWith = ReplaceWith("getPagosByDateRangeForOptica(start, end, opticaId)"),
    )
    fun getPagosByDateRange(start: LocalDate, end: LocalDate): Flow<List<Pago>> = pagoDao.getPagosByDateRange(start, end)

    fun getPagosByDateRangeForOptica(start: LocalDate, end: LocalDate, opticaId: String): Flow<List<Pago>> = pagoDao.getPagosByDateRangeForOptica(start, end, opticaId)

    suspend fun getPagosSnapshotForOptica(opticaId: String): List<Pago> = pagoDao.getPagosListByOptica(opticaId)

    fun getPagosFlowForOptica(opticaId: String): Flow<List<Pago>> = pagoDao.getPagosFlowByOptica(opticaId)

    fun getAllServiciosForOptica(opticaId: String): Flow<List<ServicioExtra>> = servicioExtraDao.getAllServiciosForOptica(opticaId)

    fun getServiciosByDateRangeForOptica(start: LocalDate, end: LocalDate, opticaId: String): Flow<List<ServicioExtra>> = servicioExtraDao.getServiciosByDateRangeForOptica(start, end, opticaId)

    suspend fun getServiciosByIds(ids: List<String>, opticaId: String): List<ServicioExtra> = servicioExtraDao.getServiciosByIds(ids, opticaId)

    suspend fun getDispensacionesByIds(ids: List<String>, opticaId: String): List<DispensacionOptica> = dispensacionDao.getDispensacionesByIds(ids, opticaId)

    suspend fun getServicioById(id: String, opticaId: String): Resource<ServicioExtra> = try {
        val servicio = servicioExtraDao.getServicioById(id, opticaId)
        if (servicio != null) {
            Resource.Success(servicio)
        } else {
            Resource.Error("Servicio no encontrado")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Log.e(TAG, "getServicioById: id=$id", e)
        Resource.Error("Error de red al obtener servicio")
    } catch (e: Exception) {
        Log.e(TAG, "getServicioById: id=$id", e)
        Resource.Error(e.message ?: "Error al obtener servicio")
    }

    suspend fun insertServicio(servicio: ServicioExtra) {
        servicioExtraDao.insertServicio(servicio)
    }

    suspend fun updateServicio(servicio: ServicioExtra) {
        servicioExtraDao.updateServicio(
            id = servicio.id, opticaId = servicio.opticaId,
            ot = servicio.ot, descripcion = servicio.descripcion,
            montoTotal = servicio.montoTotal, aCuenta = servicio.aCuenta,
            estado = servicio.estado, fecha = servicio.fecha,
            pacienteId = servicio.pacienteId, metodoPago = servicio.metodoPago,
            fechaEntrega = servicio.fechaEntrega,
            updatedAt = servicio.updatedAt, updatedBy = servicio.updatedBy,
        )
    }

    suspend fun deleteServicio(servicio: ServicioExtra) {
        servicioExtraDao.deleteServicio(servicio.id, servicio.opticaId)
    }

    suspend fun getServiciosSnapshotForOptica(opticaId: String): List<ServicioExtra> = servicioExtraDao.getServiciosListByOptica(opticaId)

    companion object {
        private const val TAG = "DispensacionRepository"
    }
}
