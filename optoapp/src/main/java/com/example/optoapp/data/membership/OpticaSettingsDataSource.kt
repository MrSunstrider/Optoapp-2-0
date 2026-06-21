package com.example.optoapp.data.membership

import android.util.Log
import com.example.optoapp.data.OpticaDto
import com.example.optoapp.data.OpticaFiscalPatch
import com.example.optoapp.data.OpticaFiscalSettings
import com.example.optoapp.data.OpticaHeaderSummary
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import com.example.optoapp.data.OpticaLaboratorioPatch
import com.example.optoapp.data.OpticaMembership


import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class OpticaSettingsDataSource @Inject constructor(
    private val supabase: SupabaseClient
) {
    suspend fun createOpticaForCurrentUser(
        nombreOptica: String,
        fiscalDocTipo: String = "",
        fiscalDocNumero: String = "",
        razonSocial: String = "",
        direccionFiscal: String = "",
        userId: String? = null,
        overrideAccessToken: String? = null
    ): Result<OpticaMembership> {
        // Esperar hasta 6s a que la sesión esté disponible (ej: post-registro)
        var uid = userId
        if (uid == null) {
            repeat(10) {
                uid = supabase.auth.currentUserOrNull()?.id
                if (uid != null) return@repeat
                delay(400)
            }
        }
        if (uid == null) {
            Log.w(TAG, "createOpticaForCurrentUser: no se pudo recuperar sesión tras reintentos")
            return Result.failure(IllegalStateException("Sin sesión"))
        }
        val nombre = nombreOptica.trim()
        if (nombre.isBlank()) return Result.failure(IllegalArgumentException("Nombre de óptica requerido"))
        val opticaId = "opt_" + UUID.randomUUID().toString().replace("-", "").take(16)
        return try {
            supabase.postgrest.rpc(
                "create_optica_for_current_user",
                buildJsonObject {
                    put("p_optica_id", opticaId)
                    put("p_nombre", nombre)
                    put("p_fiscal_doc_tipo", fiscalDocTipo.trim().uppercase())
                    put("p_fiscal_doc_numero", fiscalDocNumero.trim())
                    put("p_razon_social", razonSocial.trim())
                    put("p_direccion_fiscal", direccionFiscal.trim())
                }
            )
            Result.success(OpticaMembership(opticaId = opticaId, nombre = nombre, rol = "admin"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "createOpticaForCurrentUser: nombre=$nombre", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "createOpticaForCurrentUser: nombre=$nombre", e)
            Result.failure(e)
        }
    }

    open suspend fun fetchOpticaPlan(opticaId: String): String? {
        if (supabase.auth.currentUserOrNull() == null) return null
        return try {
            val list = supabase.postgrest[TABLE_OPTICAS]
                .select { filter { eq("id", opticaId) } }
                .decodeList<OpticaDto>()
            val row = list.firstOrNull()
            row?.planCode?.lowercase()?.trim()?.ifBlank { null }
                ?: row?.plan?.lowercase()?.trim()?.ifBlank { null }
                ?: "free"
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "fetchOpticaPlan: opticaId=$opticaId", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "fetchOpticaPlan: opticaId=$opticaId", e)
            null
        }
    }

    suspend fun fetchOpticaLaboratorioSettings(opticaId: String): Pair<String, String>? {
        if (supabase.auth.currentUserOrNull() == null) return null
        return try {
            val list = supabase.postgrest[TABLE_OPTICAS]
                .select { filter { eq("id", opticaId) } }
                .decodeList<OpticaDto>()
            val row = list.firstOrNull() ?: return "" to ""
            row.laboratorioNombre to row.laboratorioContacto
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "fetchOpticaLaboratorioSettings: opticaId=$opticaId", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "fetchOpticaLaboratorioSettings: opticaId=$opticaId", e)
            null
        }
    }

    suspend fun fetchOpticaFiscalSettings(opticaId: String): OpticaFiscalSettings? {
        if (supabase.auth.currentUserOrNull() == null) return null
        return try {
            val list = supabase.postgrest[TABLE_OPTICAS]
                .select { filter { eq("id", opticaId) } }
                .decodeList<OpticaDto>()
            val row = list.firstOrNull() ?: return OpticaFiscalSettings()
            OpticaFiscalSettings(
                nombreComercial = row.nombre,
                docTipo = row.fiscalDocTipo,
                docNumero = row.fiscalDocNumero,
                razonSocial = row.razonSocial,
                direccionFiscal = row.direccionFiscal
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "fetchOpticaFiscalSettings: opticaId=$opticaId", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "fetchOpticaFiscalSettings: opticaId=$opticaId", e)
            null
        }
    }

    suspend fun fetchOpticaHeaderSummary(opticaId: String): OpticaHeaderSummary? {
        if (supabase.auth.currentUserOrNull() == null) return null
        return try {
            val list = supabase.postgrest[TABLE_OPTICAS]
                .select { filter { eq("id", opticaId) } }
                .decodeList<OpticaDto>()
            val row = list.firstOrNull() ?: return OpticaHeaderSummary(
                nombreOptica = "Óptica sin nombre",
                fiscalEtiqueta = "Sin documento fiscal"
            )
            val fiscal = if (row.fiscalDocTipo.isBlank() || row.fiscalDocNumero.isBlank()) {
                "Sin documento fiscal"
            } else {
                "${row.fiscalDocTipo} ${row.fiscalDocNumero}"
            }
            val nombreComercial = row.nombre.trim()
                .ifBlank { row.razonSocial.trim() }
                .ifBlank { "Óptica sin nombre" }
            OpticaHeaderSummary(
                nombreOptica = nombreComercial,
                fiscalEtiqueta = fiscal
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "fetchOpticaHeaderSummary: opticaId=$opticaId", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "fetchOpticaHeaderSummary: opticaId=$opticaId", e)
            null
        }
    }

    suspend fun updateOpticaFiscalSettings(
        opticaId: String,
        nombreComercial: String,
        docTipo: String,
        docNumero: String,
        razonSocial: String,
        direccionFiscal: String
    ): Result<Unit> {
        if (supabase.auth.currentUserOrNull() == null) {
            return Result.failure(IllegalStateException("Sin sesión"))
        }
        return try {
            val patch = OpticaFiscalPatch(
                nombre = nombreComercial.trim(),
                fiscalDocTipo = docTipo.trim().uppercase(),
                fiscalDocNumero = docNumero.trim(),
                razonSocial = razonSocial.trim(),
                direccionFiscal = direccionFiscal.trim()
            )
            supabase.postgrest[TABLE_OPTICAS].update(patch) {
                filter { eq("id", opticaId) }
            }
            val persisted = supabase.postgrest[TABLE_OPTICAS]
                .select { filter { eq("id", opticaId) } }
                .decodeList<OpticaDto>()
                .firstOrNull()
                ?: return Result.failure(
                    IllegalStateException("No se pudo verificar la actualización de datos fiscales para esta óptica.")
                )
            val matches = persisted.fiscalDocTipo.trim().uppercase() == patch.fiscalDocTipo &&
                persisted.nombre.trim() == patch.nombre &&
                persisted.fiscalDocNumero.trim() == patch.fiscalDocNumero &&
                persisted.razonSocial.trim() == patch.razonSocial &&
                persisted.direccionFiscal.trim() == patch.direccionFiscal
            if (!matches) {
                return Result.failure(
                    IllegalStateException("Supabase no confirmó la persistencia de los datos fiscales. Revisa políticas RLS de opticas.")
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "updateOpticaFiscalSettings: opticaId=$opticaId", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "updateOpticaFiscalSettings: opticaId=$opticaId", e)
            Result.failure(e)
        }
    }

    suspend fun updateOpticaLaboratorioSettings(
        opticaId: String,
        laboratorioNombre: String,
        laboratorioContacto: String
    ): Result<Unit> {
        if (supabase.auth.currentUserOrNull() == null) {
            return Result.failure(IllegalStateException("Sin sesión"))
        }
        return try {
            supabase.postgrest[TABLE_OPTICAS].update(
                OpticaLaboratorioPatch(
                    laboratorioNombre = laboratorioNombre.trim(),
                    laboratorioContacto = laboratorioContacto.trim()
                )
            ) {
                filter { eq("id", opticaId) }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "updateOpticaLaboratorioSettings: opticaId=$opticaId", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "updateOpticaLaboratorioSettings: opticaId=$opticaId", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "OpticaSettingsDataSource"
        private const val TABLE_OPTICAS = "opticas"
        private const val TABLE_UO = "usuario_optica"
    }
}
