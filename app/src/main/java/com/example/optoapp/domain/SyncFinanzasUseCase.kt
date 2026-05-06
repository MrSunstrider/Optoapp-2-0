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
import kotlinx.coroutines.delay
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
        private const val UPSERT_BATCH_SIZE = 80
        private const val NETWORK_RETRY_ATTEMPTS = 3
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

            // ── 0. Propagar eliminaciones pendientes a Supabase ────────────────
            pushPendingDeletions(opticaId)

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

    // ─── PROPAGAR ELIMINACIONES ───────────────────────────────────────────────

    private suspend fun pushPendingDeletions(opticaId: String) {
        val pending = repository.getPendingDeletions(opticaId)
        if (pending.isEmpty()) return
        Log.d(TAG, "Finanzas: propagando ${pending.size} eliminaciones a Supabase")
        pending.forEach { tombstone ->
            val table = when (tombstone.entityType) {
                "servicio_extra" -> TABLE_SERVICIOS
                "dispensacion"   -> TABLE_DISPENSACIONES
                "pago"           -> TABLE_PAGOS
                else             -> null
            }
            if (table == null) {
                repository.clearDeletionState(opticaId, tombstone.entityType, tombstone.entityId)
                return@forEach
            }
            try {
                supabase.postgrest[table].delete {
                    filter {
                        eq("id", tombstone.entityId)
                        eq("optica_id", opticaId)
                    }
                }
                repository.clearDeletionState(opticaId, tombstone.entityType, tombstone.entityId)
                Log.d(TAG, "Eliminado remoto ${tombstone.entityType}/${tombstone.entityId}")
            } catch (e: Exception) {
                rethrowIfCancellation(e)
                Log.w(TAG, "No se pudo eliminar remoto ${tombstone.entityType}/${tombstone.entityId}: ${e.message}")
                // No lanzamos: un fallo aislado no debe abortar todo el sync
            }
        }
    }

    // ─── SUBIDA ──────────────────────────────────────────────────────────────

    private suspend fun uploadDispensaciones(opticaId: String): Int {
        resolveLocalDuplicateDispensaciones(opticaId)
        val dispensaciones = repository.getDispensacionesSnapshotForOptica(opticaId)
        if (dispensaciones.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_dispensaciones", "batch")
            return 0
        }
        val localById = dispensaciones.associateBy { it.id }
        val opticaRemota = opticaId.trim().ifBlank { FinanzasRemoteDefaults.OPTICA_ID_FALLBACK }
        val remotosExistentes = try {
            supabase.postgrest[TABLE_DISPENSACIONES]
                .select {
                    filter { eq("optica_id", opticaRemota) }
                }
                .decodeList<DispensacionRemotaLookup>()
        } catch (e: Exception) {
            rethrowIfCancellation(e)
            Log.w(TAG, "No se pudo consultar dispensaciones remotas para reconciliar OT: ${e.message}")
            emptyList()
        }
        val remoteIdByOt = remotosExistentes
            .mapNotNull { r ->
                normalizedOtForUnique(r.ot)?.let { key -> key to r.id }
            }
            .toMap()
        val uniqueRows = LinkedHashMap<String, Pair<String, DispensacionRemota>>()
        dispensaciones.forEach { dispensacion ->
            val base = dispensacion.toRemoto().copy(opticaId = opticaRemota)
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
            if (uniqueRows.containsKey(dedupeKey)) {
                val canonicalLocalId = uniqueRows[dedupeKey]?.first.orEmpty()
                val canonicalLocal = localById[canonicalLocalId]
                val duplicateLocal = localById[dispensacion.id]
                if (canonicalLocal != null && duplicateLocal != null) {
                    mergeLocalDispensacionConflict(
                        opticaId = opticaId,
                        canonical = canonicalLocal,
                        duplicate = duplicateLocal
                    )
                } else {
                    Log.w(TAG, "OT duplicada en lote sin datos locales para fusión (dedupeKey=$dedupeKey, localId=${dispensacion.id})")
                }
                return@forEach
            }
            uniqueRows[dedupeKey] = dispensacion.id to reconciled
        }
        // Segundo blindaje: evita que dos filas distintas terminen apuntando al mismo id remoto.
        val uniqueById = LinkedHashMap<String, Pair<String, DispensacionRemota>>()
        uniqueRows.values.forEach { (localId, row) ->
            if (uniqueById.containsKey(row.id)) {
                val firstLocalId = uniqueById[row.id]?.first.orEmpty()
                val canonicalLocal = localById[firstLocalId]
                val duplicateLocal = localById[localId]
                if (canonicalLocal != null && duplicateLocal != null) {
                    mergeLocalDispensacionConflict(
                        opticaId = opticaId,
                        canonical = canonicalLocal,
                        duplicate = duplicateLocal
                    )
                } else {
                    Log.w(TAG, "Conflicto de reconciliación sin datos locales para fusión ($localId -> $firstLocalId)")
                }
                return@forEach
            }
            uniqueById[row.id] = localId to row
        }
        val rows = uniqueById.values.map { it.second }
        try {
            rows.chunked(UPSERT_BATCH_SIZE).forEachIndexed { index, chunk ->
                retryNetwork("upsert:$TABLE_DISPENSACIONES:chunk${index + 1}") {
                    supabase.postgrest[TABLE_DISPENSACIONES].upsert(chunk)
                }
            }
        } catch (e: Exception) {
            rethrowIfCancellation(e)
            syncStateTracker.markError(opticaId, "upload_dispensaciones", "batch", e.message)
            throw e
        }
        syncStateTracker.markSynced(opticaId, "upload_dispensaciones", "batch")
        uniqueById.values.forEach { (localId, _) ->
            syncStateTracker.markSynced(opticaId, "dispensacion", localId)
        }
        return rows.size
    }

    private suspend fun mergeLocalDispensacionConflict(
        opticaId: String,
        canonical: DispensacionOptica,
        duplicate: DispensacionOptica
    ) {
        val merged = canonical.copy(
            ot = canonical.ot.ifBlank { duplicate.ot },
            monturaId = canonical.monturaId.ifBlank { duplicate.monturaId },
            pacienteId = canonical.pacienteId.ifBlank { duplicate.pacienteId },
            fecha = if (canonical.fecha >= duplicate.fecha) canonical.fecha else duplicate.fecha,
            tipoMontura = canonical.tipoMontura.ifBlank { duplicate.tipoMontura },
            materialMontura = canonical.materialMontura.ifBlank { duplicate.materialMontura },
            tipoLente = canonical.tipoLente.ifBlank { duplicate.tipoLente },
            materialLente = canonical.materialLente.ifBlank { duplicate.materialLente },
            tratamientos = (canonical.tratamientos + duplicate.tratamientos).distinct(),
            colorLente = canonical.colorLente.ifBlank { duplicate.colorLente },
            notasDiseno = canonical.notasDiseno.ifBlank { duplicate.notasDiseno },
            origenMontura = canonical.origenMontura.ifBlank { duplicate.origenMontura },
            tipoAro = canonical.tipoAro.ifBlank { duplicate.tipoAro },
            descripcionMontura = canonical.descripcionMontura.ifBlank { duplicate.descripcionMontura },
            montoTotal = maxOf(canonical.montoTotal, duplicate.montoTotal),
            metodoPago = canonical.metodoPago.ifBlank { duplicate.metodoPago },
            montoPagado = maxOf(canonical.montoPagado, duplicate.montoPagado),
            estadoEntrega = canonical.estadoEntrega.ifBlank { duplicate.estadoEntrega },
            fechaVencimientoGarantia = canonical.fechaVencimientoGarantia ?: duplicate.fechaVencimientoGarantia,
            distanciaLente = canonical.distanciaLente.ifBlank { duplicate.distanciaLente },
            altura = canonical.altura.ifBlank { duplicate.altura },
            subTipoBifocal = canonical.subTipoBifocal.ifBlank { duplicate.subTipoBifocal }
        )
        repository.updateDispensacion(merged)
        val moved = repository.reassignPagosDispensacion(duplicate.id, canonical.id)
        repository.deleteDispensacionById(duplicate.id)
        syncStateTracker.markSynced(opticaId, "dispensacion", duplicate.id)
        syncStateTracker.markError(
            opticaId,
            "dispensacion",
            canonical.id,
            "Conflicto de reconciliación resuelto: fusionada ${duplicate.id} en ${canonical.id}; " +
                "ot=${merged.ot.ifBlank { "(sin OT)" }}, paciente_id=${merged.pacienteId}, pagos_movidos=$moved."
        )
        Log.w(TAG, "Dispensacion fusionada por conflicto remoto ${duplicate.id} -> ${canonical.id} (pagos movidos=$moved)")
    }

    private suspend fun resolveLocalDuplicateDispensaciones(opticaId: String) {
        val local = repository.getDispensacionesSnapshotForOptica(opticaId)
        if (local.isEmpty()) return
        val groups = local
            .mapNotNull { d ->
                val key = normalizedOtForUnique(d.ot) ?: return@mapNotNull null
                key to d
            }
            .groupBy({ it.first }, { it.second })
            .filterValues { it.size > 1 }
        if (groups.isEmpty()) return

        groups.forEach { (otKey, rows) ->
            val canonical = rows.maxByOrNull { it.fecha } ?: return@forEach
            rows.forEach { duplicate ->
                if (duplicate.id == canonical.id) return@forEach
                val moved = repository.reassignPagosDispensacion(duplicate.id, canonical.id)
                repository.deleteDispensacionById(duplicate.id)
                syncStateTracker.markError(
                    opticaId,
                    "dispensacion",
                    duplicate.id,
                    "OT duplicada local ($otKey) resuelta automáticamente. Fusionada en ${canonical.id}; pagos movidos=$moved."
                )
                Log.w(TAG, "Dispensacion duplicada OT=$otKey fusionada ${duplicate.id} -> ${canonical.id} (pagos movidos=$moved)")
            }
        }
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
        val rows = uniqueRows.values.toList().distinctBy { it.id }
        try {
            rows.chunked(UPSERT_BATCH_SIZE).forEachIndexed { index, chunk ->
                retryNetwork("upsert:$TABLE_SERVICIOS:chunk${index + 1}") {
                    supabase.postgrest[TABLE_SERVICIOS].upsert(chunk)
                }
            }
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
        val rows = pagos.map { it.toRemoto().copy(opticaId = opticaRemota) }.distinctBy { it.id }
        try {
            rows.chunked(UPSERT_BATCH_SIZE).forEachIndexed { index, chunk ->
                retryNetwork("upsert:$TABLE_PAGOS:chunk${index + 1}") {
                    supabase.postgrest[TABLE_PAGOS].upsert(chunk)
                }
            }
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

    private suspend fun retryNetwork(
        opName: String,
        block: suspend () -> Unit
    ) {
        var lastError: Exception? = null
        repeat(NETWORK_RETRY_ATTEMPTS) { attempt ->
            try {
                block()
                return
            } catch (e: Exception) {
                rethrowIfCancellation(e)
                lastError = e
                val shouldRetry = isTransientNetworkError(e)
                if (!shouldRetry || attempt == NETWORK_RETRY_ATTEMPTS - 1) throw e
                val backoffMs = 400L * (attempt + 1)
                Log.w(TAG, "$opName fallo de red (intento ${attempt + 1}/$NETWORK_RETRY_ATTEMPTS). Reintentando en ${backoffMs}ms")
                delay(backoffMs)
            }
        }
        lastError?.let { throw it }
    }

    private fun isTransientNetworkError(e: Exception): Boolean {
        val msg = e.message?.lowercase().orEmpty()
        return msg.contains("timeout") ||
            msg.contains("timed out") ||
            msg.contains("connect") && msg.contains("failed") ||
            msg.contains("unable to resolve host") ||
            msg.contains("network is unreachable") ||
            msg.contains("connection reset")
    }

    // ─── BAJADA ──────────────────────────────────────────────────────────────

    /** IDs marcados para eliminación que NO deben reinsertarse al bajar de la nube. */
    private suspend fun deletedIds(opticaId: String): Set<String> {
        return repository.getPendingDeletions(opticaId).map { it.entityId }.toSet()
    }

    private suspend fun downloadDispensaciones(opticaId: String): Int {
        val skipIds = deletedIds(opticaId)
        val remotos = supabase.postgrest[TABLE_DISPENSACIONES].select { filter { eq("optica_id", opticaId) } }.decodeList<DispensacionRemota>()
        remotos.forEach { r ->
            if (r.id in skipIds) return@forEach
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
        val skipIds = deletedIds(opticaId)
        val remotos = supabase.postgrest[TABLE_SERVICIOS].select { filter { eq("optica_id", opticaId) } }.decodeList<ServicioRemoto>()
        remotos.forEach { r ->
            if (r.id in skipIds) return@forEach
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
        val skipIds = deletedIds(opticaId)
        val remotos = supabase.postgrest[TABLE_PAGOS]
            .select { filter { eq("optica_id", opticaId) } }
            .decodeList<PagoRemoto>()
        remotos.forEach { r ->
            if (r.id in skipIds) return@forEach
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

@Serializable
private data class DispensacionRemotaLookup(
    val id: String,
    val ot: String? = null
)
