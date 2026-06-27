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

/**
 * Repositorio especializado en operaciones de DispensacionOptica, Pago y ServicioExtra.
 * Extraído de [OptoRepository] para reducir el God class.
 */
class DispensacionRepository(
    private val dispensacionDao: DispensacionDao,
    private val dispensacionItemDao: DispensacionItemDao,
    private val pagoDao: PagoDao,
    private val servicioExtraDao: ServicioExtraDao
) {

    // ── Dispensaciones ───────────────────────────────────────────────────────

    fun getDispensacionesByDateRange(start: LocalDate, end: LocalDate): Flow<List<DispensacionOptica>> =
        dispensacionDao.getDispensacionesByDateRange(start, end)

    fun getDispensacionesByDateRangeForOptica(start: LocalDate, end: LocalDate, opticaId: String): Flow<List<DispensacionOptica>> =
        dispensacionDao.getDispensacionesByDateRangeForOptica(start, end, opticaId)

    fun getDispensacionesByPaciente(pacienteId: String): Flow<List<DispensacionOptica>> =
        dispensacionDao.getDispensacionesByPaciente(pacienteId)

    fun getAllDispensaciones(): Flow<List<DispensacionOptica>> = dispensacionDao.getAllDispensaciones()

    fun getAllDispensacionesForOptica(opticaId: String): Flow<List<DispensacionOptica>> =
        dispensacionDao.getAllDispensacionesForOptica(opticaId)

    fun getTotalVendido(): Flow<Double?> = dispensacionDao.getTotalVendido()

    fun getTotalPagado(): Flow<Double?> = dispensacionDao.getTotalPagado()

    fun getTotalVendidoForOptica(opticaId: String): Flow<Double?> =
        dispensacionDao.getTotalVendidoForOptica(opticaId)

    fun getTotalPagadoForOptica(opticaId: String): Flow<Double?> =
        dispensacionDao.getTotalPagadoForOptica(opticaId)

    suspend fun getDispensacionById(id: String): Resource<DispensacionOptica> {
        return try {
            val dispensacion = dispensacionDao.getDispensacionById(id)
            if (dispensacion != null) Resource.Success(dispensacion)
            else Resource.Error("Dispensación no encontrada")
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "getDispensacionById: id=$id", e)
            Resource.Error("Error de red al obtener dispensación")
        } catch (e: Exception) {
            Log.e(TAG, "getDispensacionById: id=$id", e)
            Resource.Error(e.message ?: "Error al obtener dispensación")
        }
    }

    suspend fun getLastDispensacionByPacienteId(pacienteId: String): Resource<DispensacionOptica> {
        return try {
            val disp = dispensacionDao.getLastDispensacionByPacienteId(pacienteId)
            if (disp != null) Resource.Success(disp)
            else Resource.Error("No hay dispensaciones")
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "getLastDispensacionByPacienteId: pacienteId=$pacienteId", e)
            Resource.Error("Error de red al obtener dispensación")
        } catch (e: Exception) {
            Log.e(TAG, "getLastDispensacionByPacienteId: pacienteId=$pacienteId", e)
            Resource.Error(e.message ?: "Error al obtener dispensación")
        }
    }

    suspend fun insertDispensacion(dispensacion: DispensacionOptica) {
        dispensacionDao.insertDispensacion(dispensacion)
    }

    suspend fun updateDispensacion(dispensacion: DispensacionOptica) {
        dispensacionDao.updateDispensacion(dispensacion)
    }

    suspend fun deleteDispensacionById(id: String): Int = dispensacionDao.deleteById(id)

    suspend fun getDispensacionesSnapshotForOptica(opticaId: String): List<DispensacionOptica> =
        dispensacionDao.getDispensacionesListByOptica(opticaId)

    /** Siguiente correlativo `OT-<año>-####` según OT existentes de la óptica para ese año. */
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

    suspend fun deleteAll() {
        dispensacionItemDao.deleteAll()
        servicioExtraDao.deleteAll()
        pagoDao.deleteAll()
        dispensacionDao.deleteAll()
    }

    // ── Items de Dispensación ─────────────────────────────────────────────────

    fun getItemsByDispensacion(dispensacionId: String): Flow<List<DispensacionItem>> =
        dispensacionItemDao.getItemsByDispensacion(dispensacionId)

    suspend fun getItemsListByDispensacion(dispensacionId: String): List<DispensacionItem> =
        dispensacionItemDao.getItemsListByDispensacion(dispensacionId)

    suspend fun getItemsListByOptica(opticaId: String): List<DispensacionItem> =
        dispensacionItemDao.getItemsListByOptica(opticaId)

    suspend fun getDispensacionItemById(id: String): DispensacionItem? =
        dispensacionItemDao.getById(id)

    suspend fun insertDispensacionItem(item: DispensacionItem) {
        dispensacionItemDao.insertItem(item)
    }

    suspend fun deleteDispensacionItemById(id: String): Int =
        dispensacionItemDao.deleteById(id)

    suspend fun deleteItemsByDispensacionId(dispensacionId: String): Int =
        dispensacionItemDao.deleteByDispensacionId(dispensacionId)

    suspend fun getAllDispensacionItems(): List<DispensacionItem> =
        dispensacionItemDao.getAllItems()

    // ── Pagos ────────────────────────────────────────────────────────────────

    fun getPagosByDispensacion(dispensacionId: String): Flow<List<Pago>> = pagoDao.getPagosByDispensacion(dispensacionId)

    suspend fun insertPago(pago: Pago) {
        pagoDao.insertPago(pago)
    }

    suspend fun updatePago(pago: Pago) {
        pagoDao.updatePago(pago)
    }

    suspend fun getPagoById(id: String): Pago? = pagoDao.getPagoById(id)

    suspend fun reassignPagosDispensacion(oldDispensacionId: String, newDispensacionId: String): Int =
        pagoDao.reassignDispensacionId(oldDispensacionId, newDispensacionId)

    /**
     * Borra un abono. Si ya existía en BD, registra un movimiento de anulación el día [fechaAnulacion]
     * (importe negativo) para que cierre de caja refleje la salida o reversión en la fecha correcta.
     */
    suspend fun deletePagoRegistrandoAnulacionEnCaja(
        pago: Pago,
        opticaId: String,
        fechaAnulacion: LocalDate = DateUtils.today()
    ) {
        val existing = pagoDao.getPagoById(pago.id)
        if (existing != null && existing.monto != 0.0) {
            val reversal = Pago(
                id = UUID.randomUUID().toString(),
                dispensacionId = existing.dispensacionId,
                servicioExtraId = existing.servicioExtraId,
                fecha = fechaAnulacion,
                tipo = "Anulación",
                monto = -existing.monto,
                metodoPago = existing.metodoPago,
                nota = "Anula abono ${existing.id.take(8)}… (${DateUtils.formatLocalized(existing.fecha)})",
                opticaId = opticaId
            )
            pagoDao.insertPago(reversal)
        }
        pagoDao.deletePago(pago)
    }

    suspend fun deletePago(pago: Pago) = pagoDao.deletePago(pago)

    fun getPagosByServicioExtra(servicioExtraId: String): Flow<List<Pago>> =
        pagoDao.getPagosByServicioExtra(servicioExtraId)

    fun getPagosByDateRange(start: LocalDate, end: LocalDate): Flow<List<Pago>> =
        pagoDao.getPagosByDateRange(start, end)

    fun getPagosByDateRangeForOptica(start: LocalDate, end: LocalDate, opticaId: String): Flow<List<Pago>> =
        pagoDao.getPagosByDateRangeForOptica(start, end, opticaId)

    suspend fun getPagosSnapshotForOptica(opticaId: String): List<Pago> =
        pagoDao.getPagosListByOptica(opticaId)

    // ── Servicios Extra ──────────────────────────────────────────────────────

    fun getAllServicios(): Flow<List<ServicioExtra>> = servicioExtraDao.getAllServicios()

    fun getAllServiciosForOptica(opticaId: String): Flow<List<ServicioExtra>> =
        servicioExtraDao.getAllServiciosForOptica(opticaId)

    fun getServiciosByPaciente(pacienteId: String): Flow<List<ServicioExtra>> = servicioExtraDao.getServiciosByPaciente(pacienteId)

    suspend fun getServicioById(id: String): Resource<ServicioExtra> {
        return try {
            val servicio = servicioExtraDao.getServicioById(id)
            if (servicio != null) Resource.Success(servicio)
            else Resource.Error("Servicio no encontrado")
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "getServicioById: id=$id", e)
            Resource.Error("Error de red al obtener servicio")
        } catch (e: Exception) {
            Log.e(TAG, "getServicioById: id=$id", e)
            Resource.Error(e.message ?: "Error al obtener servicio")
        }
    }

    suspend fun insertServicio(servicio: ServicioExtra) {
        servicioExtraDao.insertServicio(servicio)
    }

    suspend fun updateServicio(servicio: ServicioExtra) {
        servicioExtraDao.updateServicio(servicio)
    }

    suspend fun deleteServicio(servicio: ServicioExtra) {
        servicioExtraDao.deleteServicio(servicio)
    }

    suspend fun getServiciosSnapshotForOptica(opticaId: String): List<ServicioExtra> =
        servicioExtraDao.getServiciosListByOptica(opticaId)

    companion object {
        private const val TAG = "DispensacionRepository"
    }
}
