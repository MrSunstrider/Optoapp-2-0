package com.example.optoapp.domain

import android.util.Log
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.Resource
import com.example.optoapp.data.ServicioExtra
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
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
            val pagosUp = uploadPagos(opticaId)

            // 2. BAJADA
            val dispDown = downloadDispensaciones(opticaId)
            val servDown = downloadServicios(opticaId)
            val pagosDown = downloadPagos(opticaId)

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
        val dispensaciones = repository.getDispensacionesSnapshotForOptica(opticaId)
        if (dispensaciones.isEmpty()) return 0
        val rows = dispensaciones.map { it.toRemoto().copy(opticaId = opticaId) }
        supabase.postgrest[TABLE_DISPENSACIONES].upsert(rows)
        return dispensaciones.size
    }

    private suspend fun uploadServicios(opticaId: String): Int {
        val servicios = repository.getServiciosSnapshotForOptica(opticaId)
        if (servicios.isEmpty()) return 0
        val rows = servicios.map { it.toRemoto().copy(opticaId = opticaId) }
        supabase.postgrest[TABLE_SERVICIOS].upsert(rows)
        return servicios.size
    }

    private suspend fun uploadPagos(opticaId: String): Int {
        val pagos = repository.getPagosSnapshotForOptica(opticaId)
        if (pagos.isEmpty()) return 0
        val rows = pagos.map { it.toRemoto().copy(opticaId = opticaId) }
        supabase.postgrest[TABLE_PAGOS].upsert(rows)
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

    private suspend fun downloadPagos(opticaId: String): Int {
        val remotos = supabase.postgrest[TABLE_PAGOS]
            .select { filter { eq("optica_id", opticaId) } }
            .decodeList<PagoRemoto>()
        remotos.forEach { repository.insertPago(it.toEntity()) }
        return remotos.size
    }
}

// ─── DTOs de sincronización ──────────────────────────────────────────────────

@Serializable
data class DispensacionRemota(
    val id: String,
    @SerialName("paciente_id") val pacienteId: String,
    val fecha: Long,
    @SerialName("optica_id") val opticaId: String,
    @SerialName("tipo_montura") val tipoMontura: String? = null,
    @SerialName("material_montura") val materialMontura: String? = null,
    @SerialName("tipo_lente") val tipoLente: String? = null,
    @SerialName("material_lente") val materialLente: String? = null,
    val tratamientos: String? = null,
    @SerialName("color_lente") val colorLente: String? = null,
    @SerialName("notas_diseno") val notasDiseno: String? = null,
    @SerialName("origen_montura") val origenMontura: String? = null,
    @SerialName("tipo_aro") val tipoAro: String? = null,
    @SerialName("descripcion_montura") val descripcionMontura: String? = null,
    @SerialName("monto_total") val montoTotal: Double = 0.0,
    @SerialName("metodo_pago") val metodoPago: String? = null,
    @SerialName("monto_pagado") val montoPagado: Double = 0.0,
    @SerialName("estado_entrega") val estadoEntrega: String? = null,
    @SerialName("fecha_vencimiento_garantia") val fechaVencimientoGarantia: Long? = null,
    @SerialName("distancia_lente") val distanciaLente: String? = null,
    @SerialName("sub_tipo_bifocal") val subTipoBifocal: String? = null
) {
    fun toEntity() = DispensacionOptica(
        id = id, pacienteId = pacienteId, fecha = fecha, opticaId = optId(opticaId),
        tipoMontura = tipoMontura ?: "", materialMontura = materialMontura ?: "",
        tipoLente = tipoLente ?: "", materialLente = materialLente ?: "",
        tratamientos = tratamientos?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
        colorLente = colorLente ?: "", notasDiseno = notasDiseno ?: "",
        origenMontura = origenMontura ?: "", tipoAro = tipoAro ?: "",
        descripcionMontura = descripcionMontura ?: "", montoTotal = montoTotal,
        metodoPago = metodoPago ?: "", montoPagado = montoPagado,
        estadoEntrega = estadoEntrega ?: "",
        fechaVencimientoGarantia = fechaVencimientoGarantia?.toString(),
        distanciaLente = distanciaLente ?: "", subTipoBifocal = subTipoBifocal ?: ""
    )

    private fun optId(remoteId: String) = remoteId.ifBlank { "mi_optica_base" }
}

@Serializable
data class ServicioRemoto(
    val id: String,
    val ot: String = "",
    val descripcion: String = "",
    @SerialName("monto_total") val montoTotal: Double = 0.0,
    @SerialName("a_cuenta") val aCuenta: Double = 0.0,
    val estado: String = "",
    val fecha: Long,
    @SerialName("paciente_id") val pacienteId: String,
    @SerialName("metodo_pago") val metodoPago: String = "",
    @SerialName("optica_id") val opticaId: String
) {
    fun toEntity() = ServicioExtra(
        id = id, ot = ot, descripcion = descripcion, montoTotal = montoTotal,
        aCuenta = aCuenta, estado = estado, fecha = fecha,
        pacienteId = pacienteId, metodoPago = metodoPago, opticaId = opticaId
    )
}

@Serializable
data class PagoRemoto(
    val id: String,
    @SerialName("dispensacion_id") val dispensacionId: String? = null,
    @SerialName("servicio_extra_id") val servicioExtraId: String? = null,
    val fecha: Long,
    val tipo: String = "",
    val monto: Double = 0.0,
    @SerialName("metodo_pago") val metodoPago: String = "",
    val nota: String? = null,
    @SerialName("optica_id") val opticaId: String = "mi_optica_base"
) {
    fun toEntity() = Pago(
        id = id, dispensacionId = dispensacionId, servicioExtraId = servicioExtraId,
        fecha = fecha, tipo = tipo, monto = monto, metodoPago = metodoPago, nota = nota ?: "",
        opticaId = opticaId
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

// ─── Extensiones: Entidad → DTO para Supabase ────────────────────────────────

private fun DispensacionOptica.toRemoto(): DispensacionRemota = DispensacionRemota(
    id = id, pacienteId = pacienteId, fecha = fecha, opticaId = opticaId,
    tipoMontura = tipoMontura, materialMontura = materialMontura,
    tipoLente = tipoLente, materialLente = materialLente,
    tratamientos = tratamientos.joinToString(","), colorLente = colorLente,
    notasDiseno = notasDiseno, origenMontura = origenMontura,
    tipoAro = tipoAro, descripcionMontura = descripcionMontura,
    montoTotal = montoTotal, metodoPago = metodoPago,
    montoPagado = montoPagado, estadoEntrega = estadoEntrega,
    fechaVencimientoGarantia = fechaVencimientoGarantia?.toLongOrNull(),
    distanciaLente = distanciaLente, subTipoBifocal = subTipoBifocal
)

private fun Pago.toRemoto(): PagoRemoto = PagoRemoto(
    id = id, dispensacionId = dispensacionId, servicioExtraId = servicioExtraId,
    fecha = fecha, tipo = tipo, monto = monto, metodoPago = metodoPago, nota = nota,
    opticaId = opticaId
)

private fun ServicioExtra.toRemoto(): ServicioRemoto = ServicioRemoto(
    id = id, ot = ot, descripcion = descripcion, montoTotal = montoTotal,
    aCuenta = aCuenta, estado = estado, fecha = fecha,
    pacienteId = pacienteId ?: "", metodoPago = metodoPago, opticaId = opticaId
)
