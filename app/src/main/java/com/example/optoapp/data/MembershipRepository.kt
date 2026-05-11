package com.example.optoapp.data

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.coroutines.CancellationException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MembershipRepository @Inject constructor(
    private val supabase: SupabaseClient
) {

    /**
     * Carga las ópticas a las que tiene acceso el usuario autenticado actual.
     */
    suspend fun fetchMembershipsForCurrentUser(): List<OpticaMembership> {
        val uid = supabase.auth.currentUserOrNull()?.id ?: return emptyList()
        val rows = try {
            supabase.postgrest[TABLE_UO]
                .select { filter { eq("user_id", uid) } }
                .decodeList<UsuarioOpticaDto>()
        } catch (e: Exception) {
            if (e !is CancellationException) {
                Log.w(TAG, "fetchMembershipsForCurrentUser falló (uid=$uid): ${e.message}", e)
            }
            emptyList()
        }
        if (rows.isEmpty()) return emptyList()
        val out = mutableListOf<OpticaMembership>()
        for (row in rows) {
            val nombre = fetchOpticaNombre(row.opticaId)
            out.add(
                OpticaMembership(
                    opticaId = row.opticaId,
                    nombre = nombre.ifBlank { row.opticaId },
                    rol = row.rol.ifBlank { "admin" }
                )
            )
        }
        return out
    }

    suspend fun fetchMembersForOptica(opticaId: String): List<OpticaMemberRow> {
        if (supabase.auth.currentUserOrNull() == null) return emptyList()
        return try {
            supabase.postgrest[TABLE_OPTICA_MEMBERS]
                .select { filter { eq("optica_id", opticaId) } }
                .decodeList<OpticaMemberRow>()
                .sortedWith(compareBy({ it.email.lowercase() }, { it.userId }))
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun assignRoleByEmail(opticaId: String, email: String, rol: String): Result<Unit> {
        if (supabase.auth.currentUserOrNull() == null) {
            return Result.failure(IllegalStateException("Sin sesión"))
        }
        val normalizedEmail = email.trim().lowercase()
        val normalizedRole = rol.trim().lowercase()
        if (normalizedEmail.isBlank()) return Result.failure(IllegalArgumentException("Email requerido"))
        if (normalizedRole.isBlank()) return Result.failure(IllegalArgumentException("Rol requerido"))
        return try {
            val user = supabase.postgrest[TABLE_USER_PROFILES]
                .select { filter { eq("email", normalizedEmail) } }
                .decodeList<UserProfileRow>()
                .firstOrNull()
                ?: return Result.failure(IllegalArgumentException("No existe una cuenta con ese email."))

            supabase.postgrest[TABLE_UO].upsert(
                listOf(
                    UsuarioOpticaUpsertDto(
                    userId = user.userId,
                    opticaId = opticaId,
                    rol = normalizedRole
                    )
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createOpticaForCurrentUser(
        nombreOptica: String,
        fiscalDocTipo: String = "",
        fiscalDocNumero: String = "",
        razonSocial: String = "",
        direccionFiscal: String = ""
    ): Result<OpticaMembership> {
        val uid = supabase.auth.currentUserOrNull()?.id
            ?: return Result.failure(IllegalStateException("Sin sesión"))
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
        } catch (e: Exception) {
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
        } catch (e: Exception) {
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
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchOpticaNombre(opticaId: String): String {
        return try {
            val list = supabase.postgrest[TABLE_OPTICAS]
                .select { filter { eq("id", opticaId) } }
                .decodeList<OpticaDto>()
            list.firstOrNull()?.nombre.orEmpty()
        } catch (_: Exception) {
            ""
        }
    }

    /** Plan de suscripción (`plan_code`) desde Supabase; fallback a columna legacy `plan`. */
    suspend fun fetchOpticaPlan(opticaId: String): String? {
        if (supabase.auth.currentUserOrNull() == null) return null
        return try {
            val list = supabase.postgrest[TABLE_OPTICAS]
                .select { filter { eq("id", opticaId) } }
                .decodeList<OpticaDto>()
            val row = list.firstOrNull()
            row?.planCode?.lowercase()?.trim()?.ifBlank { null }
                ?: row?.plan?.lowercase()?.trim()?.ifBlank { null }
                ?: "free"
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Lee contacto de laboratorio desde Supabase. `null` si no hay sesión, error de red o RLS.
     * Par vacío `"" to ""` es válido cuando la fila existe sin datos aún.
     */
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
        } catch (_: Exception) {
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
        } catch (_: Exception) {
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
        } catch (_: Exception) {
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
            supabase.postgrest[TABLE_OPTICAS].update(
                patch
            ) {
                filter { eq("id", opticaId) }
            }
            // RLS puede dejar updates en 0 filas sin lanzar excepción; verificamos persistencia real.
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
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Persiste en Supabase. Falla si no hay sesión, red o políticas RLS.
     */
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
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "MembershipRepository"
        private const val TABLE_UO = "usuario_optica"
        private const val TABLE_OPTICAS = "opticas"
        private const val TABLE_USER_PROFILES = "user_profiles"
        private const val TABLE_OPTICA_MEMBERS = "optica_members"
    }
}

@Serializable
private data class UsuarioOpticaDto(
    @SerialName("user_id") val userId: String,
    @SerialName("optica_id") val opticaId: String,
    val rol: String = "admin"
)

@Serializable
data class OpticaMemberRow(
    @SerialName("optica_id") val opticaId: String,
    @SerialName("user_id") val userId: String,
    val email: String = "",
    val rol: String = "",
    @SerialName("created_at") val createdAt: String = ""
)

data class PlanSettings(
    val planCode: String,
    val maxOpticas: Int?,
    val maxPacientesPorOptica: Int?,
    val maxUsuariosPorOptica: Int?,
    val planStatus: String
)

data class OpticaFiscalSettings(
    val nombreComercial: String = "",
    val docTipo: String = "",
    val docNumero: String = "",
    val razonSocial: String = "",
    val direccionFiscal: String = "",
    val distritoCiudadDepartamento: String = "",
    val moneda: String = "",
    val pais: String = "",
    val contactoWhatsappTelefono: String = ""
)

data class OpticaHeaderSummary(
    val nombreOptica: String,
    val fiscalEtiqueta: String
)

@Serializable
private data class UserProfileRow(
    @SerialName("user_id") val userId: String,
    val email: String
)

@Serializable
private data class UsuarioOpticaUpsertDto(
    @SerialName("user_id") val userId: String,
    @SerialName("optica_id") val opticaId: String,
    val rol: String
)

@Serializable
private data class OpticaDto(
    val id: String,
    val nombre: String = "",
    val plan: String = "free",
    @SerialName("plan_code") val planCode: String? = null,
    @SerialName("laboratorio_nombre") val laboratorioNombre: String = "",
    @SerialName("laboratorio_contacto") val laboratorioContacto: String = "",
    @SerialName("fiscal_doc_tipo") val fiscalDocTipo: String = "",
    @SerialName("fiscal_doc_numero") val fiscalDocNumero: String = "",
    @SerialName("razon_social") val razonSocial: String = "",
    @SerialName("direccion_fiscal") val direccionFiscal: String = "",
    @SerialName("distrito_ciudad_departamento") val distritoCiudadDepartamento: String = "",
    val moneda: String = "",
    val pais: String = "",
    @SerialName("contacto_whatsapp_telefono") val contactoWhatsappTelefono: String = ""
)

@Serializable
private data class OpticaInsertDto(
    val id: String,
    val nombre: String,
    val plan: String = "free",
    @SerialName("plan_code") val planCode: String = "free",
    @SerialName("max_opticas") val maxOpticas: Int = 1,
    @SerialName("max_pacientes_por_optica") val maxPacientesPorOptica: Int = 20,
    @SerialName("max_usuarios_por_optica") val maxUsuariosPorOptica: Int = 2,
    @SerialName("plan_source") val planSource: String = "manual",
    @SerialName("plan_status") val planStatus: String = "active",
    @SerialName("fiscal_doc_tipo") val fiscalDocTipo: String = "",
    @SerialName("fiscal_doc_numero") val fiscalDocNumero: String = "",
    @SerialName("razon_social") val razonSocial: String = "",
    @SerialName("direccion_fiscal") val direccionFiscal: String = "",
    @SerialName("distrito_ciudad_departamento") val distritoCiudadDepartamento: String = "",
    val moneda: String = "",
    val pais: String = "",
    @SerialName("contacto_whatsapp_telefono") val contactoWhatsappTelefono: String = ""
)

@Serializable
private data class OpticaPlanSettingsDto(
    @SerialName("plan_code") val planCode: String = "free",
    @SerialName("max_opticas") val maxOpticas: Int? = null,
    @SerialName("max_pacientes_por_optica") val maxPacientesPorOptica: Int? = null,
    @SerialName("max_usuarios_por_optica") val maxUsuariosPorOptica: Int? = null,
    @SerialName("plan_status") val planStatus: String = "active"
)

@Serializable
private data class OpticaPlanUpdateDto(
    @SerialName("plan_code") val planCode: String,
    @SerialName("max_opticas") val maxOpticas: Int?,
    @SerialName("max_pacientes_por_optica") val maxPacientesPorOptica: Int?,
    @SerialName("max_usuarios_por_optica") val maxUsuariosPorOptica: Int?,
    @SerialName("plan_status") val planStatus: String
)

@Serializable
private data class OpticaLaboratorioPatch(
    @SerialName("laboratorio_nombre") val laboratorioNombre: String,
    @SerialName("laboratorio_contacto") val laboratorioContacto: String
)

@Serializable
private data class OpticaFiscalPatch(
    val nombre: String,
    @SerialName("fiscal_doc_tipo") val fiscalDocTipo: String,
    @SerialName("fiscal_doc_numero") val fiscalDocNumero: String,
    @SerialName("razon_social") val razonSocial: String,
    @SerialName("direccion_fiscal") val direccionFiscal: String,
    @SerialName("distrito_ciudad_departamento") val distritoCiudadDepartamento: String,
    val moneda: String,
    val pais: String,
    @SerialName("contacto_whatsapp_telefono") val contactoWhatsappTelefono: String
)
