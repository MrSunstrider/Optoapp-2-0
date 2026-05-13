package com.example.optoapp.viewmodel.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.edit
import java.io.IOException
import kotlinx.coroutines.CancellationException
import com.example.optoapp.BuildConfig
import com.example.optoapp.data.ISecurityManager
import com.example.optoapp.data.ISessionManager
import com.example.optoapp.data.MembershipRepository
import com.example.optoapp.data.OpticaMembership
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.domain.SyncSessionHelper
import com.example.optoapp.notifications.NotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/**
 * Encapsula toda la lógica de autenticación (login, register, logout, sesión).
 *
 * Los métodos que originalmente usaban [androidx.lifecycle.viewModelScope] se exponen como
 * funciones suspend. AuthViewModel se encarga de lanzarlas en su scope y de manejar el estado.
 *
 * ADR-2: Delegados separados inyectados → SRP, testeable con fakes.
 */
open class AuthDelegate @Inject constructor(
    private val securityManager: ISecurityManager,
    private val sessionManager: ISessionManager,
    private val repository: OptoRepository,
    private val membershipRepository: MembershipRepository,
    private val supabase: SupabaseClient,
    @ApplicationContext private val appContext: Context
) {
    companion object {
        private const val TAG = "AuthDelegate"

        /** Pure logic: extract display name from UserInfo metadata. */
        fun extractDisplayName(
            user: UserInfo,
            emailFallback: String?,
            nameFallback: String?
        ): String {
            val meta = user.userMetadata
            val candidates = listOf(
                meta?.get("nombre")?.toString(),
                meta?.get("full_name")?.toString(),
                meta?.get("name")?.toString(),
                nameFallback,
                emailFallback?.substringBefore("@"),
                user.email?.substringBefore("@")
            )
            return candidates.firstOrNull { !it.isNullOrBlank() }
                ?.removePrefix("\"")
                ?.removeSuffix("\"")
                .orEmpty()
                .ifBlank { "Usuario" }
        }

        /** Pure logic: check if a timestamp is within 24h of now. */
        fun isTimestampWithin24h(lastLoginTimestamp: Long): Boolean {
            if (lastLoginTimestamp == 0L) return false
            val diffHours = (System.currentTimeMillis() - lastLoginTimestamp) / (1000 * 60 * 60)
            return diffHours < 24
        }
    }

    //── Flujos reactivos de sesión (delegados de SessionManager) ──────────────

    val isLoggedIn: Flow<Boolean> = sessionManager.isLoggedIn
    val opticaId: Flow<String> = sessionManager.opticaId
    val opticaRol: Flow<String> = sessionManager.opticaRol
    val userEmail: Flow<String> = sessionManager.userEmail
    val userName: Flow<String> = sessionManager.userName
    val userTimeZone: Flow<String?> = sessionManager.userTimeZone

    //── Sesión ────────────────────────────────────────────────────────────────

    suspend fun isSessionTimeValid(): Boolean =
        isTimestampWithin24h(sessionManager.lastLoginTimestamp.first())

    //── Login ─────────────────────────────────────────────────────────────────

    suspend fun login(email: String, password: String) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun loginWithGoogle() {
        supabase.auth.signInWith(Google)
        Log.d(TAG, "Inicio de OAuth Google lanzado")
    }

    suspend fun handleAuthDeepLinkIntent(intent: Intent?): String? {
        val deepLink = intent?.data ?: return null
        Log.d(TAG, "Recibido deeplink OAuth: $deepLink")

        val handleResult = runCatching { supabase.handleDeeplinks(intent) }
        handleResult.onFailure { e ->
            Log.w(TAG, "No se pudo procesar deeplink OAuth: ${e.localizedMessage}", e)
        }

        var hasSession = false
        repeat(10) {
            val session = runCatching { supabase.auth.currentSessionOrNull() }.getOrNull()
            if (session != null) {
                hasSession = true
                return@repeat
            }
            delay(200)
        }

        if (hasSession) {
            SyncSessionHelper.refreshSessionBeforeSync(supabase)
            return null // success — no error message
        }

        return handleResult.exceptionOrNull()?.localizedMessage
            ?: "No se pudo recuperar la sesión de Google. Reintenta el acceso."
    }

    //── Register ──────────────────────────────────────────────────────────────

    suspend fun register(email: String, password: String): String {
        return try {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            runCatching { supabase.auth.signOut() }
            "Cuenta creada. Ahora un admin/gerente debe asignarte rol en la óptica."
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Registro: error de red", e)
            "No se pudo crear la cuenta: ${e.localizedMessage ?: "error desconocido"}"
        } catch (e: Exception) {
            Log.e(TAG, "Registro", e)
            "No se pudo crear la cuenta: ${e.localizedMessage ?: "error desconocido"}"
        }
    }

    //── Selección de óptica ───────────────────────────────────────────────────

    suspend fun selectOptica(membership: OpticaMembership) {
        sessionManager.saveSession(
            opticaId = membership.opticaId,
            email = pendingLoginEmail,
            name = pendingLoginName,
            rol = membership.rol
        )
        repository.reassignLegacyMiOpticaBaseTo(membership.opticaId)
        if (BuildConfig.DEBUG) {
            val uid = try { supabase.auth.currentUserOrNull()?.id } catch (_: Exception) { null }
            Log.d(TAG, "Óptica seleccionada: ${membership.opticaId} rol=${membership.rol} uid=$uid")
        }
    }

    suspend fun prepareOpticaSelection(): List<OpticaMembership> {
        return membershipRepository.fetchMembershipsForCurrentUser()
    }

    //── Post-login (private en ViewModel original) ────────────────────────────

    data class PostLoginResult(
        val email: String,
        val name: String,
        val memberships: List<OpticaMembership>,
        val requiresSelection: Boolean,
        val requiresOnboarding: Boolean
    )

    private var pendingLoginEmail: String = ""
    private var pendingLoginName: String = ""

    suspend fun resolvePostLogin(
        emailFallback: String? = null,
        nameFallback: String? = null
    ): PostLoginResult {
        val user = supabase.auth.currentUserOrNull()
            ?: throw IllegalStateException("No se encontró usuario autenticado")
        val email = user.email?.trim().orEmpty().ifBlank { emailFallback.orEmpty() }
        val nombre = Companion.extractDisplayName(user, emailFallback, nameFallback)

        pendingLoginEmail = email
        pendingLoginName = nombre

        val memberships = membershipRepository.fetchMembershipsForCurrentUser()

        return when {
            memberships.size > 1 -> PostLoginResult(
                email = email,
                name = nombre,
                memberships = memberships,
                requiresSelection = true,
                requiresOnboarding = false
            )
            memberships.size == 1 -> {
                val m = memberships.first()
                sessionManager.saveSession(
                    opticaId = m.opticaId,
                    email = email,
                    name = nombre,
                    rol = m.rol
                )
                repository.reassignLegacyMiOpticaBaseTo(m.opticaId)
                PostLoginResult(
                    email = email,
                    name = nombre,
                    memberships = memberships,
                    requiresSelection = false,
                    requiresOnboarding = false
                )
            }
            else -> {
                sessionManager.clearSession()
                PostLoginResult(
                    email = email,
                    name = nombre,
                    memberships = emptyList(),
                    requiresSelection = false,
                    requiresOnboarding = true
                )
            }
        }
    }

    //── Logout ────────────────────────────────────────────────────────────────

    suspend fun logout() {
        try {
            supabase.auth.signOut()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.w(TAG, "Error en signOut (ignorado): ${e.localizedMessage}", e)
        } catch (e: Exception) {
            Log.w(TAG, "Error en signOut (ignorado): ${e.localizedMessage}", e)
        }
        sessionManager.clearSession()
    }

    //── Check session al inicio ───────────────────────────────────────────────

    suspend fun checkExistingSession(): Boolean {
        val session = supabase.auth.currentSessionOrNull()
        return if (session == null || !isSessionTimeValid()) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Sesión inexistente o expirada por tiempo (24h). Limpiando...")
            }
            logout()
            false
        } else {
            val oid = sessionManager.opticaId.first()
            repository.reassignLegacyMiOpticaBaseTo(oid)
            rescheduleFutureRemindersIfEnabled(oid)
            true
        }
    }

    private suspend fun rescheduleFutureRemindersIfEnabled(opticaId: String) {
        val enabled = appContext
            .getSharedPreferences("optoapp_prefs", Context.MODE_PRIVATE)
            .getBoolean("pref_enable_reminders", true)
        if (!enabled) return

        val today = LocalDate.now()
        val helper = NotificationHelper(appContext)
        val evaluaciones = repository.getEvaluacionesSnapshotForOptica(opticaId)
        var scheduled = 0
        var cancelled = 0

        for (ev in evaluaciones) {
            val cita = ev.proximaCita
            val estado = ev.citaEstado.trim().lowercase()
            val shouldNotify = cita != null &&
                !cita.isBefore(today) &&
                estado !in setOf("asistio", "no_asistio")

            if (shouldNotify) {
                val paciente = repository.getPacienteById(ev.pacienteId)
                val nombre = if (paciente is com.example.optoapp.data.Resource.Success) {
                    paciente.data?.nombreCompleto ?: "Paciente"
                } else {
                    "Paciente"
                }
                helper.scheduleWorkManagerReminder(nombre, cita!!, ev.id)
                scheduled++
            } else {
                helper.cancelReminder(ev.id)
                cancelled++
            }
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Reprogramación de recordatorios: programados=$scheduled cancelados=$cancelled")
        }
    }

    //── Onboarding ────────────────────────────────────────────────────────────

    suspend fun completeOnboardingOptica(
        nombreOptica: String,
        fiscalDocTipo: String,
        fiscalDocNumero: String,
        razonSocial: String,
        direccionFiscal: String
    ): Result<OpticaMembership> {
        val email = pendingLoginEmail.ifBlank { sessionManager.userEmail.first() }
        val name = pendingLoginName.ifBlank { sessionManager.userName.first() }
        val result = membershipRepository.createOpticaForCurrentUser(
            nombreOptica = nombreOptica,
            fiscalDocTipo = fiscalDocTipo,
            fiscalDocNumero = fiscalDocNumero,
            razonSocial = razonSocial,
            direccionFiscal = direccionFiscal
        )
        if (result.isSuccess) {
            val m = result.getOrNull()!!
            sessionManager.saveSession(
                opticaId = m.opticaId,
                email = email,
                name = name,
                rol = "admin"
            )
        }
        return result
    }

    suspend fun createAdditionalOptica(nombreOptica: String): Result<OpticaMembership> {
        val role = sessionManager.opticaRol.first().trim().lowercase()
        if (role !in setOf("admin", "gerente")) {
            return Result.failure(IllegalStateException("Solo admin o gerente pueden crear sucursales."))
        }
        return membershipRepository.createOpticaForCurrentUser(nombreOptica)
    }

    suspend fun resolveDuplicateHistorias(): String {
        val rol = sessionManager.opticaRol.first().trim().lowercase()
        if (rol !in setOf("admin", "gerente")) {
            return "Solo admin o gerente pueden resolver duplicados de historia optométrica."
        }
        return runCatching {
            val oid = sessionManager.opticaId.first()
            val result = repository.resolveDuplicatePacientesByHistoria(oid)
            if (result.mergedPacientes == 0) {
                "No se encontraron pacientes duplicados por Historia Optométrica."
            } else {
                "Duplicados resueltos: pacientes fusionados=${result.mergedPacientes}, " +
                    "evaluaciones movidas=${result.movedEvaluaciones}, " +
                    "dispensaciones movidas=${result.movedDispensaciones}, " +
                    "servicios movidos=${result.movedServicios}."
            }
        }.getOrElse { e ->
            "No se pudieron resolver duplicados HO: ${e.localizedMessage ?: "error desconocido"}"
        }
    }
}
