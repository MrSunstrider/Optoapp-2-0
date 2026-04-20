package com.example.optoapp.domain

import android.util.Log
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.Resource
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.util.rethrowIfCancellation
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import javax.inject.Inject

/**
 * FASE 3 – Paso 3.3
 * Sincronización de Dispensaciones, Pagos y Servicios Extra.
 *
 * P0-T2 — Orden de subida obligatorio: **Dispensaciones → servicios_extra → pagos**
 * (padres antes que pagos; pagos referencian dispensación y/o servicio).
 *
 * Orden de bajada: el mismo, para que existan padres antes de insertar pagos locales.
 */
class SyncFinanzasUseCase @Inject constructor(
    private val repository: OptoRepository,
    private val supabase: SupabaseClient,
    private val syncStateTracker: com.example.optoapp.data.SyncStateTracker
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
            Log.d(TAG, "Finanzas: inicio (opticaId=$opticaId)")
            val dispUp = uploadDispensaciones(opticaId)
            Log.d(TAG, "Finanzas: upload dispensaciones=$dispUp")
            val servUp = uploadServicios(opticaId)
            Log.d(TAG, "Finanzas: upload servicios_extra=$servUp")
            val pagosUp = uploadPagos(opticaId)
            Log.d(TAG, "Finanzas: upload pagos=$pagosUp")

            val dispDown = downloadDispensaciones(opticaId)
            Log.d(TAG, "Finanzas: download dispensaciones=$dispDown")
            val servDown = downloadServicios(opticaId)
            Log.d(TAG, "Finanzas: download servicios_extra=$servDown")
            val pagosDown = downloadPagos(opticaId)
            Log.d(TAG, "Finanzas: download pagos=$pagosDown; fin OK")

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
            rethrowIfCancellation(e)
            Log.e(TAG, "Error en sincronización financiera", e)
            Resource.Error("Error sincronizando finanzas: ${e.localizedMessage}")
        }
    }

    // ─── SUBIDA ──────────────────────────────────────────────────────────────

    private suspend fun uploadDispensaciones(opticaId: String): Int {
        val dispensaciones = repository.getDispensacionesSnapshotForOptica(opticaId)
        if (dispensaciones.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_dispensaciones", "batch")
            return 0
        }
        val rows = dispensaciones.map { it.toRemoto().copy(opticaId = opticaId) }
        try {
            supabase.postgrest[TABLE_DISPENSACIONES].upsert(rows)
        } catch (e: Exception) {
            rethrowIfCancellation(e)
            syncStateTracker.markError(opticaId, "upload_dispensaciones", "batch", e.message)
            throw e
        }
        syncStateTracker.markSynced(opticaId, "upload_dispensaciones", "batch")
        dispensaciones.forEach { d ->
            syncStateTracker.markSynced(opticaId, "dispensacion", d.id)
        }
        return dispensaciones.size
    }

    private suspend fun uploadServicios(opticaId: String): Int {
        val servicios = repository.getServiciosSnapshotForOptica(opticaId)
        if (servicios.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_servicios_extra", "batch")
            return 0
        }
        val rows = servicios.map { it.toRemoto().copy(opticaId = opticaId) }
        try {
            supabase.postgrest[TABLE_SERVICIOS].upsert(rows)
        } catch (e: Exception) {
            rethrowIfCancellation(e)
            syncStateTracker.markError(opticaId, "upload_servicios_extra", "batch", e.message)
            throw e
        }
        syncStateTracker.markSynced(opticaId, "upload_servicios_extra", "batch")
        servicios.forEach { s ->
            syncStateTracker.markSynced(opticaId, "servicio_extra", s.id)
        }
        return servicios.size
    }

    private suspend fun uploadPagos(opticaId: String): Int {
        val pagos = repository.getPagosSnapshotForOptica(opticaId)
        if (pagos.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_pagos", "batch")
            return 0
        }
        val rows = pagos.map { it.toRemoto().copy(opticaId = opticaId) }
        try {
            supabase.postgrest[TABLE_PAGOS].upsert(rows)
        } catch (e: Exception) {
            rethrowIfCancellation(e)
            syncStateTracker.markError(opticaId, "upload_pagos", "batch", e.message)
            throw e
        }
        syncStateTracker.markSynced(opticaId, "upload_pagos", "batch")
        pagos.forEach { p ->
            syncStateTracker.markSynced(opticaId, "pago", p.id)
        }
        return pagos.size
    }

    // ─── BAJADA ──────────────────────────────────────────────────────────────

    private suspend fun downloadDispensaciones(opticaId: String): Int {
        val remotos = supabase.postgrest[TABLE_DISPENSACIONES].select { filter { eq("optica_id", opticaId) } }.decodeList<DispensacionRemota>()
        remotos.forEach { r ->
            try {
                val local = r.toEntity()
                repository.insertDispensacion(local)
                syncStateTracker.markSynced(opticaId, "dispensacion", local.id)
            } catch (e: Exception) {
                rethrowIfCancellation(e)
                syncStateTracker.markError(opticaId, "dispensacion", r.id, e.message)
            }
        }
        return remotos.size
    }

    private suspend fun downloadServicios(opticaId: String): Int {
        val remotos = supabase.postgrest[TABLE_SERVICIOS].select { filter { eq("optica_id", opticaId) } }.decodeList<ServicioRemoto>()
        remotos.forEach { r ->
            try {
                val local = r.toEntity()
                repository.insertServicio(local)
                syncStateTracker.markSynced(opticaId, "servicio_extra", local.id)
            } catch (e: Exception) {
                rethrowIfCancellation(e)
                syncStateTracker.markError(opticaId, "servicio_extra", r.id, e.message)
            }
        }
        return remotos.size
    }

    private suspend fun downloadPagos(opticaId: String): Int {
        val remotos = supabase.postgrest[TABLE_PAGOS]
            .select { filter { eq("optica_id", opticaId) } }
            .decodeList<PagoRemoto>()
        remotos.forEach { r ->
            try {
                val local = r.toEntity()
                repository.insertPago(local)
                syncStateTracker.markSynced(opticaId, "pago", local.id)
            } catch (e: Exception) {
                rethrowIfCancellation(e)
                syncStateTracker.markError(opticaId, "pago", r.id, e.message)
            }
        }
        return remotos.size
    }
}

// ─── DTOs de sincronización ──────────────────────────────────────────────────

@Serializable
data class DispensacionRemota(
    val id: String,
    val ot: String? = null,
    @SerialName("montura_id") val monturaId: String? = null,
    @SerialName("paciente_id") val pacienteId: String,
    val fecha: String,
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
    @SerialName("fecha_vencimiento_garantia") val fechaVencimientoGarantia: String? = null,
    @SerialName("distancia_lente") val distanciaLente: String? = null,
    val altura: String? = null,
    @SerialName("sub_tipo_bifocal") val subTipoBifocal: String? = null
) {
    fun toEntity() = DispensacionOptica(
        id = id, ot = ot ?: "", monturaId = monturaId ?: "", pacienteId = pacienteId, fecha = LocalDate.parse(fecha), opticaId = optId(opticaId),
        tipoMontura = tipoMontura ?: "", materialMontura = materialMontura ?: "",
        tipoLente = tipoLente ?: "", materialLente = materialLente ?: "",
        tratamientos = tratamientos?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
        colorLente = colorLente ?: "", notasDiseno = notasDiseno ?: "",
        origenMontura = origenMontura ?: "", tipoAro = tipoAro ?: "",
        descripcionMontura = descripcionMontura ?: "", montoTotal = montoTotal,
        metodoPago = metodoPago ?: "", montoPagado = montoPagado,
        estadoEntrega = estadoEntrega ?: "",
        fechaVencimientoGarantia = fechaVencimientoGarantia?.let(LocalDate::parse),
        distanciaLente = distanciaLente ?: "",
        altura = altura ?: "",
        subTipoBifocal = subTipoBifocal ?: ""
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
    val fecha: String,
    @SerialName("paciente_id") val pacienteId: String,
    @SerialName("metodo_pago") val metodoPago: String = "",
    @SerialName("optica_id") val opticaId: String
) {
    fun toEntity() = ServicioExtra(
        id = id, ot = ot, descripcion = descripcion, montoTotal = montoTotal,
        aCuenta = aCuenta, estado = estado, fecha = LocalDate.parse(fecha),
        // Vacío o inválido rompe la FK a pacientes en Room; null es válido (servicio sin paciente).
        pacienteId = pacienteId.takeIf { it.isNotBlank() },
        metodoPago = metodoPago, opticaId = opticaId
    )
}

@Serializable
data class PagoRemoto(
    val id: String,
    @SerialName("dispensacion_id") val dispensacionId: String? = null,
    @SerialName("servicio_extra_id") val servicioExtraId: String? = null,
    val fecha: String,
    val tipo: String = "",
    val monto: Double = 0.0,
    @SerialName("metodo_pago") val metodoPago: String = "",
    val nota: String? = null,
    @SerialName("optica_id") val opticaId: String = "mi_optica_base"
) {
    fun toEntity() = Pago(
        id = id, dispensacionId = dispensacionId, servicioExtraId = servicioExtraId,
        fecha = LocalDate.parse(fecha), tipo = tipo, monto = monto, metodoPago = metodoPago, nota = nota ?: "",
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
    id = id, ot = ot, monturaId = monturaId, pacienteId = pacienteId, fecha = fecha.toString(), opticaId = opticaId,
    tipoMontura = tipoMontura, materialMontura = materialMontura,
    tipoLente = tipoLente, materialLente = materialLente,
    tratamientos = tratamientos.joinToString(","), colorLente = colorLente,
    notasDiseno = notasDiseno, origenMontura = origenMontura,
    tipoAro = tipoAro, descripcionMontura = descripcionMontura,
    montoTotal = montoTotal, metodoPago = metodoPago,
    montoPagado = montoPagado, estadoEntrega = estadoEntrega,
    fechaVencimientoGarantia = fechaVencimientoGarantia?.toString(),
    distanciaLente = distanciaLente, altura = altura, subTipoBifocal = subTipoBifocal
)

private fun Pago.toRemoto(): PagoRemoto = PagoRemoto(
    id = id, dispensacionId = dispensacionId, servicioExtraId = servicioExtraId,
    fecha = fecha.toString(), tipo = tipo, monto = monto, metodoPago = metodoPago, nota = nota,
    opticaId = opticaId
)

private fun ServicioExtra.toRemoto(): ServicioRemoto = ServicioRemoto(
    id = id, ot = ot, descripcion = descripcion, montoTotal = montoTotal,
    aCuenta = aCuenta, estado = estado, fecha = fecha.toString(),
    pacienteId = pacienteId ?: "", metodoPago = metodoPago, opticaId = opticaId
)
