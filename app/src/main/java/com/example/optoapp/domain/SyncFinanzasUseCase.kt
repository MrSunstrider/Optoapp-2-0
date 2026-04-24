package com.example.optoapp.domain

import android.util.Log
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.FinanzasRemoteDefaults
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
    suspend operator fun invoke(
        opticaId: String,
        downloadAfterUpload: Boolean = true
    ): Resource<FinanzasSyncResult> {
        return try {
            Log.d(TAG, "Finanzas: inicio (opticaId=$opticaId, download=$downloadAfterUpload)")
            val dispUp = uploadDispensaciones(opticaId)
            Log.d(TAG, "Finanzas: upload dispensaciones=$dispUp")
            val servUp = uploadServicios(opticaId)
            Log.d(TAG, "Finanzas: upload servicios_extra=$servUp")
            val pagosUp = uploadPagos(opticaId)
            Log.d(TAG, "Finanzas: upload pagos=$pagosUp")

            val dispDown: Int
            val servDown: Int
            val pagosDown: Int
            if (downloadAfterUpload) {
                dispDown = downloadDispensaciones(opticaId)
                Log.d(TAG, "Finanzas: download dispensaciones=$dispDown")
                servDown = downloadServicios(opticaId)
                Log.d(TAG, "Finanzas: download servicios_extra=$servDown")
                pagosDown = downloadPagos(opticaId)
                Log.d(TAG, "Finanzas: download pagos=$pagosDown; fin OK")
            } else {
                dispDown = 0
                servDown = 0
                pagosDown = 0
                Log.d(TAG, "Finanzas: fin upload-only OK")
            }

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
        val opticaRemota = opticaId.trim().ifBlank { FinanzasRemoteDefaults.OPTICA_ID_FALLBACK }
        val rows = dispensaciones.map { it.toRemoto().copy(opticaId = opticaRemota) }
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
        val opticaRemota = opticaId.trim().ifBlank { FinanzasRemoteDefaults.OPTICA_ID_FALLBACK }
        val remotosExistentes = try {
            supabase.postgrest[TABLE_SERVICIOS]
                .select {
                    filter { eq("optica_id", opticaRemota) }
                }
                .decodeList<ServicioRemotoLookup>()
        } catch (e: Exception) {
            rethrowIfCancellation(e)
            // Si falla esta lectura, seguimos con el flujo normal de upsert para no bloquear sync.
            Log.w(TAG, "No se pudo consultar servicios remotos para reconciliar OT: ${e.message}")
            emptyList()
        }
        val remoteIdByOt = remotosExistentes
            .mapNotNull { r ->
                normalizedOtForUnique(r.ot)?.let { key -> key to r.id }
            }
            .toMap()

        // Evita colisiones de unique (optica_id, upper(trim(ot))) en el mismo batch:
        // - Si OT ya existe remoto, reutilizamos su id para que upsert actualice.
        // - Si hay duplicados locales por OT, nos quedamos con el último visto.
        val uniqueRows = LinkedHashMap<String, ServicioRemoto>()
        servicios.forEach { servicio ->
            val base = servicio.toRemoto().copy(opticaId = opticaRemota)
            val normalizedOt = normalizedOtForUnique(base.ot)
            val reconciled = if (normalizedOt != null) {
                val existingRemoteId = remoteIdByOt[normalizedOt]
                if (existingRemoteId != null && existingRemoteId != base.id) {
                    base.copy(id = existingRemoteId)
                } else {
                    base
                }
            } else {
                base
            }
            val dedupeKey = normalizedOt?.let { "ot:$it" } ?: "id:${reconciled.id}"
            uniqueRows[dedupeKey] = reconciled
        }
        val rows = uniqueRows.values.toList()
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
        val opticaRemota = opticaId.trim().ifBlank { FinanzasRemoteDefaults.OPTICA_ID_FALLBACK }
        val rows = pagos.map { it.toRemoto().copy(opticaId = opticaRemota) }
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
    /** En Postgres suele ser UUID nullable: enviar null, nunca "". */
    @SerialName("paciente_id") val pacienteId: String? = null,
    @SerialName("metodo_pago") val metodoPago: String = "",
    @SerialName("optica_id") val opticaId: String
) {
    fun toEntity() = ServicioExtra(
        // Blindaje adicional ante datos remotos legacy/ensuciados.
        // Si luego se agregan checks numéricos en DB, ya no romperá al descargar/subir.
        // También evita estados o descripciones vacías en UI local.
        // (local puede seguir mostrando OT vacía como opcional).
        // Montos:
        // - montoTotal no negativo
        // - aCuenta en [0, montoTotal]
        // Texto:
        // - descripcion/estado nunca vacíos
        //
        // Nota: método y OT usan mapeo reversible para no ensuciar la UX local.
        id = id,
        ot = ot.trim().remotoOtServicioExtraToLocal(),
        descripcion = descripcion.trim().ifBlank { FinanzasRemoteDefaults.ServicioExtra.DESCRIPCION_VACIA },
        montoTotal = montoTotal.coerceAtLeast(0.0),
        aCuenta = aCuenta.coerceAtLeast(0.0).coerceAtMost(montoTotal.coerceAtLeast(0.0)),
        estado = estado.trim().ifBlank { FinanzasRemoteDefaults.ServicioExtra.ESTADO_VACIO },
        fecha = LocalDate.parse(fecha),
        // Vacío o inválido rompe la FK a pacientes en Room; null es válido (servicio sin paciente).
        pacienteId = pacienteId?.takeIf { it.isNotBlank() },
        metodoPago = metodoPago.remotoServicioExtraMetodoToLocal(),
        opticaId = opticaId.trim().ifBlank { FinanzasRemoteDefaults.OPTICA_ID_FALLBACK }
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
        id = id,
        dispensacionId = dispensacionId.normalizeOptionalFk(),
        servicioExtraId = servicioExtraId.normalizeOptionalFk(),
        fecha = LocalDate.parse(fecha),
        tipo = tipo,
        monto = monto,
        metodoPago = metodoPago,
        nota = nota ?: "",
        opticaId = opticaId.trim().ifBlank { FinanzasRemoteDefaults.OPTICA_ID_FALLBACK }
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
    id = id,
    dispensacionId = dispensacionId.normalizeOptionalFk(),
    servicioExtraId = servicioExtraId.normalizeOptionalFk(),
    fecha = fecha.toString(),
    tipo = tipo.trim().ifBlank { FinanzasRemoteDefaults.Pago.TIPO_VACIO },
    monto = monto,
    metodoPago = metodoPago.trim().ifBlank { FinanzasRemoteDefaults.Pago.METODO_PAGO_VACIO },
    nota = nota.trim(),
    opticaId = opticaId.trim().ifBlank { FinanzasRemoteDefaults.OPTICA_ID_FALLBACK }
)

/** PostgREST: FK UUID opcional no debe enviarse como "". */
private fun String?.normalizeOptionalFk(): String? =
    this?.trim()?.takeIf { it.isNotBlank() }

private fun String.remotoServicioExtraMetodoToLocal(): String =
    if (this == FinanzasRemoteDefaults.ServicioExtra.METODO_PAGO_ROW) "" else this

private fun String.remotoOtServicioExtraToLocal(): String =
    if (this == FinanzasRemoteDefaults.ServicioExtra.OT_VACIA) "" else this

private fun normalizedOtForUnique(ot: String?): String? =
    ot?.trim()?.takeIf { it.isNotBlank() }?.uppercase()

private fun ServicioExtra.toRemoto(): ServicioRemoto = ServicioRemoto(
    id = id,
    // Supabase tiene unicidad por (optica_id, OT normalizada) para OT no vacía.
    // Enviar "-" para OT vacía convertía "sin OT" en un valor real y generaba
    // colisiones de unique constraint al subir más de un servicio sin OT.
    // Se envía vacío para que no entre en el índice único parcial.
    ot = ot.trim(),
    descripcion = descripcion.trim().ifBlank { FinanzasRemoteDefaults.ServicioExtra.DESCRIPCION_VACIA },
    montoTotal = montoTotal.coerceAtLeast(0.0),
    aCuenta = aCuenta.coerceAtLeast(0.0).coerceAtMost(montoTotal.coerceAtLeast(0.0)),
    estado = estado.trim().ifBlank { FinanzasRemoteDefaults.ServicioExtra.ESTADO_VACIO },
    fecha = fecha.toString(),
    pacienteId = pacienteId?.trim()?.takeIf { it.isNotBlank() },
    metodoPago = metodoPago.trim().ifBlank { FinanzasRemoteDefaults.ServicioExtra.METODO_PAGO_ROW },
    opticaId = opticaId.trim().ifBlank { FinanzasRemoteDefaults.OPTICA_ID_FALLBACK }
)

@Serializable
private data class ServicioRemotoLookup(
    val id: String,
    val ot: String = ""
)
