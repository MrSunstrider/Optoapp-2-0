package com.example.optoapp.domain

import android.util.Log
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.Resource
import com.example.optoapp.data.ServicioExtra
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable
import javax.inject.Inject

/**
 * FASE 3 – Paso 3.3
 * Sincronización de Dispensaciones, Pagos y Servicios Extra.
 *
 * Estrategia upload-first:
 *  1. Sube todas las Dispensaciones cuyo opticaId coincida.
 *  2. Sube todos los Pagos asociados (sin filtro de opticaId propio,
 *     se garantiza integridad referencial por la fk dispensacionId/servicioExtraId).
 *  3. Sube todos los ServiciosExtra cuyo opticaId coincida.
 *
 * Orden garantizado: primero padres (Dispensaciones/Servicios) y luego hijos (Pagos),
 * para evitar fallos de clave foránea si Supabase tiene FK habilitadas.
 */
class SyncFinanzasUseCase @Inject constructor(
    private val repository: OptoRepository,
    private val supabase: SupabaseClient
) {

    companion object {
        private const val TAG = "SyncFinanzas"
        private const val TABLE_DISPENSACIONES = "dispensaciones"
        private const val TABLE_PAGOS          = "pagos"
        private const val TABLE_SERVICIOS      = "servicios_extra"
    }

    /**
     * Ejecuta la sincronización completa del módulo financiero (Upload -> Download).
     */
    suspend operator fun invoke(opticaId: String): Resource<FinanzasSyncResult> {
        return try {
            // 1. SUBIDA
            val dispUp = uploadDispensaciones(opticaId)
            val servUp = uploadServicios(opticaId)
            val pagosUp = uploadPagos()

            // 2. BAJADA
            val dispDown = downloadDispensaciones(opticaId)
            val servDown = downloadServicios(opticaId)
            val pagosDown = downloadPagos()

            Resource.Success(
                FinanzasSyncResult(
                    uploadedDispensaciones = dispUp,
                    uploadedServicios = servUp,
                    uploadedPagos = pagosUp,
                    downloadedDispensaciones = dispDown,
                    downloadedServicios = servDown,
                    downloadedPagos = pagosDown
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error en sincronización financiera", e)
            Resource.Error("Error sincronizando finanzas: ${e.localizedMessage}")
        }
    }

    // ─── SUBIDA ──────────────────────────────────────────────────────────────

    private suspend fun uploadDispensaciones(opticaId: String): Int {
        val dispensaciones = repository.getAllDispensacionesSnapshot().filter { it.opticaId == opticaId }
        if (dispensaciones.isEmpty()) return 0
        supabase.postgrest[TABLE_DISPENSACIONES].upsert(dispensaciones.map { it.toRemoteMap() })
        return dispensaciones.size
    }

    private suspend fun uploadServicios(opticaId: String): Int {
        val servicios = repository.getAllServiciosSnapshot().filter { it.opticaId == opticaId }
        if (servicios.isEmpty()) return 0
        supabase.postgrest[TABLE_SERVICIOS].upsert(servicios.map { it.toRemoteMap() })
        return servicios.size
    }

    private suspend fun uploadPagos(): Int {
        val pagos = repository.getAllPagosSnapshot()
        if (pagos.isEmpty()) return 0
        supabase.postgrest[TABLE_PAGOS].upsert(pagos.map { it.toRemoteMap() })
        return pagos.size
    }

    // ─── BAJADA ──────────────────────────────────────────────────────────────

    private suspend fun downloadDispensaciones(opticaId: String): Int {
        val remotos = supabase.postgrest[TABLE_DISPENSACIONES].select { filter { eq("optica_id", opticaId) } }.decodeList<DispensacionRemota>()
        remotos.forEach { repository.insertDispensacion(it.toEntity()) }
        return remotos.size
    }

    private suspend fun downloadServicios(opticaId: String): Int {
        val remotos = supabase.postgrest[TABLE_SERVICIOS].select { filter { eq("optica_id", opticaId) } }.decodeList<ServicioRemoto>()
        remotos.forEach { repository.insertServicio(it.toEntity()) }
        return remotos.size
    }

    private suspend fun downloadPagos(): Int {
        val remotos = supabase.postgrest[TABLE_PAGOS].select().decodeList<PagoRemoto>()
        remotos.forEach { repository.insertPago(it.toEntity()) }
        return remotos.size
    }
}

// ─── DTOs de bajada ──────────────────────────────────────────────────────────

@Serializable
data class DispensacionRemota(
    val id: String, val paciente_id: String, val fecha: Long, val optica_id: String,
    val tipo_montura: String? = null, val material_montura: String? = null,
    val tipo_lente: String? = null, val material_lente: String? = null,
    val tratamientos: String? = null, val color_lente: String? = null,
    val notas_diseno: String? = null, val origen_montura: String? = null,
    val tipo_aro: String? = null, val descripcion_montura: String? = null,
    val monto_total: Double = 0.0, val metodo_pago: String? = null,
    val monto_pagado: Double = 0.0, val estado_entrega: String? = null,
    val fecha_vencimiento_garantia: Long? = null, val distancia_lente: String? = null,
    val sub_tipo_bifocal: String? = null
) {
    fun toEntity() = DispensacionOptica(
        id = id, pacienteId = paciente_id, fecha = fecha, opticaId = optica_id,
        tipoMontura = tipo_montura ?: "", materialMontura = material_montura ?: "",
        tipoLente = tipo_lente ?: "", materialLente = material_lente ?: "",
        tratamientos = tratamientos?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
        colorLente = color_lente ?: "", notasDiseno = notas_diseno ?: "",
        origenMontura = origen_montura ?: "", tipoAro = tipo_aro ?: "",
        descripcionMontura = descripcion_montura ?: "", montoTotal = monto_total,
        metodoPago = metodo_pago ?: "", montoPagado = monto_pagado,
        estadoEntrega = estado_entrega ?: "",
        fechaVencimientoGarantia = fecha_vencimiento_garantia?.toString(),
        distanciaLente = distancia_lente ?: "", subTipoBifocal = sub_tipo_bifocal ?: ""
    )
}

@Serializable
data class ServicioRemoto(
    val id: String, val ot: String = "", val descripcion: String = "",
    val monto_total: Double = 0.0, val a_cuenta: Double = 0.0,
    val estado: String = "", val fecha: Long, val paciente_id: String,
    val metodo_pago: String = "", val optica_id: String
) {
    fun toEntity() = ServicioExtra(
        id = id, ot = ot, descripcion = descripcion, montoTotal = monto_total,
        aCuenta = a_cuenta, estado = estado, fecha = fecha,
        pacienteId = paciente_id, metodoPago = metodo_pago, opticaId = optica_id
    )
}

@Serializable
data class PagoRemoto(
    val id: String, val dispensacion_id: String? = null,
    val servicio_extra_id: String? = null, val fecha: Long,
    val tipo: String = "", val monto: Double = 0.0,
    val metodo_pago: String = "", val nota: String? = null
) {
    fun toEntity() = Pago(
        id = id, dispensacionId = dispensacion_id, servicioExtraId = servicio_extra_id,
        fecha = fecha, tipo = tipo, monto = monto, metodoPago = metodo_pago, nota = nota ?: ""
    )
}

// ─── Resultado de la sincronización financiera ────────────────────────────────

data class FinanzasSyncResult(
    val uploadedDispensaciones: Int,
    val uploadedServicios: Int,
    val uploadedPagos: Int,
    val downloadedDispensaciones: Int,
    val downloadedServicios: Int,
    val downloadedPagos: Int
)

// ─── Extensiones: Entidad → Mapa remoto ──────────────────────────────────────

private fun DispensacionOptica.toRemoteMap(): Map<String, Any?> = mapOf(
    "id"                      to id,
    "paciente_id"             to pacienteId,
    "fecha"                   to fecha,
    "optica_id"               to opticaId,
    "tipo_montura"            to tipoMontura,
    "material_montura"        to materialMontura,
    "tipo_lente"              to tipoLente,
    "material_lente"          to materialLente,
    "tratamientos"            to tratamientos.joinToString(","),
    "color_lente"             to colorLente,
    "notas_diseno"            to notasDiseno,
    "origen_montura"          to origenMontura,
    "tipo_aro"                to tipoAro,
    "descripcion_montura"     to descripcionMontura,
    "monto_total"             to montoTotal,
    "metodo_pago"             to metodoPago,
    "monto_pagado"            to montoPagado,
    "estado_entrega"          to estadoEntrega,
    "fecha_vencimiento_garantia" to fechaVencimientoGarantia,
    "distancia_lente"         to distanciaLente,
    "sub_tipo_bifocal"        to subTipoBifocal
)

private fun Pago.toRemoteMap(): Map<String, Any?> = mapOf(
    "id"                to id,
    "dispensacion_id"   to dispensacionId,
    "servicio_extra_id" to servicioExtraId,
    "fecha"             to fecha,
    "tipo"              to tipo,
    "monto"             to monto,
    "metodo_pago"       to metodoPago,
    "nota"              to nota,
    "optica_id"         to opticaId
)

private fun ServicioExtra.toRemoteMap(): Map<String, Any?> = mapOf(
    "id"          to id,
    "ot"          to ot,
    "descripcion" to descripcion,
    "monto_total" to montoTotal,
    "a_cuenta"    to aCuenta,
    "estado"      to estado,
    "fecha"       to fecha,
    "paciente_id" to pacienteId,
    "metodo_pago" to metodoPago,
    "optica_id"   to opticaId
)
