package com.example.optoapp.data.membership

import android.util.Log
import com.example.optoapp.data.OpticaDto
import com.example.optoapp.data.OpticaFiscalPatch
import com.example.optoapp.data.OpticaFiscalSettings
import com.example.optoapp.data.OpticaHeaderSummary
import com.example.optoapp.data.OpticaInsertDto
import com.example.optoapp.data.OpticaLaboratorioPatch
import com.example.optoapp.data.OpticaMembership
import com.example.optoapp.data.OpticaPlanSettingsDto
import com.example.optoapp.data.OpticaPlanUpdateDto
import com.example.optoapp.data.PlanSettings
import com.example.optoapp.data.UsuarioOpticaUpsertDto
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
        direccionFiscal: String = ""
    ): Result<OpticaMembership> {
        var uid = supabase.auth.currentUserOrNull()?.id
        if (uid == null) {
            // Reintentar: refrescar sesión y esperar antes de rendirse
            runCatching {
                supabase.auth.refreshCurrentSession()
                delay(500)
                uid = supabase.auth.currentUserOrNull()?.id
            }
        }
        if (uid == null) return Result.failure(IllegalStateException("Sin sesión"))
        val nombre = nombreOptica.trim()
        if (nombre.isBlank()) return Result.failure(IllegalArgumentException("Nombre de óptica requerido"))
        val opticaId = "opt_" + UUID.randomUUID().toString().replace("-", "").take(16)
        return try {
            supabase.postgrest[TABLE_OPTICAS].insert(
                listOf(
                    OpticaInsertDto(
                        id = opticaId,
                        nombre = nombre,
                        plan = "free",
                        planCode = "free",
                        maxOpticas = 1,
                        maxPacientesPorOptica = 20,
                        maxUsuariosPorOptica = 2,
                        planSource = "manual",
                        planStatus = "active",
                        fiscalDocTipo = fiscalDocTipo.trim().uppercase(),
                        fiscalDocNumero = fiscalDocNumero.trim(),
                        razonSocial = razonSocial.trim(),
                        direccionFiscal = direccionFiscal.trim()
                    )
                )
            )
            supabase.postgrest[TABLE_UO].insert(
                listOf(UsuarioOpticaUpsertDto(userId = uid, opticaId = opticaId, rol = "admin"))
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

    suspend fun fetchPlanSettings(opticaId: String): Result<PlanSettings> {
        if (supabase.auth.currentUserOrNull() == null) return Result.failure(IllegalStateException("Sin sesión"))
        return try {
            val row = supabase.postgrest[TABLE_OPTICAS]
                .select { filter { eq("id", opticaId) } }
                .decodeList<OpticaPlanSettingsDto>()
                .firstOrNull()
                ?: return Result.failure(IllegalStateException("No se encontró la óptica"))
            Result.success(
                PlanSettings(
                    planCode = row.planCode.ifBlank { "free" },
                    maxOpticas = row.maxOpticas,
                    maxPacientesPorOptica = row.maxPacientesPorOptica,
                    maxUsuariosPorOptica = row.maxUsuariosPorOptica,
                    planStatus = row.planStatus.ifBlank { "active" }
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "fetchPlanSettings: opticaId=$opticaId", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "fetchPlanSettings: opticaId=$opticaId", e)
            Result.failure(e)
        }
    }

    suspend fun updatePlanSettings(opticaId: String, settings: PlanSettings): Result<Unit> {
        if (supabase.auth.currentUserOrNull() == null) return Result.failure(IllegalStateException("Sin sesión"))
        return try {
            supabase.postgrest[TABLE_OPTICAS].update(
                OpticaPlanUpdateDto(
                    planCode = settings.planCode.trim().lowercase(),
                    maxOpticas = settings.maxOpticas,
                    maxPacientesPorOptica = settings.maxPacientesPorOptica,
                    maxUsuariosPorOptica = settings.maxUsuariosPorOptica,
                    planStatus = settings.planStatus.trim().lowercase()
                )
            ) {
                filter { eq("id", opticaId) }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "updatePlanSettings: opticaId=$opticaId", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "updatePlanSettings: opticaId=$opticaId", e)
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
                direccionFiscal = row.direccionFiscal,
                distritoCiudadDepartamento = row.distritoCiudadDepartamento,
                moneda = row.moneda,
                pais = row.pais,
                contactoWhatsappTelefono = row.contactoWhatsappTelefono
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
        direccionFiscal: String,
        distritoCiudadDepartamento: String,
        moneda: String,
        pais: String,
        contactoWhatsappTelefono: String
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
                direccionFiscal = direccionFiscal.trim(),
                distritoCiudadDepartamento = distritoCiudadDepartamento.trim(),
                moneda = moneda.trim(),
                pais = pais.trim(),
                contactoWhatsappTelefono = contactoWhatsappTelefono.trim()
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
                persisted.direccionFiscal.trim() == patch.direccionFiscal &&
                persisted.distritoCiudadDepartamento.trim() == patch.distritoCiudadDepartamento &&
                persisted.moneda.trim() == patch.moneda &&
                persisted.pais.trim() == patch.pais &&
                persisted.contactoWhatsappTelefono.trim() == patch.contactoWhatsappTelefono
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
