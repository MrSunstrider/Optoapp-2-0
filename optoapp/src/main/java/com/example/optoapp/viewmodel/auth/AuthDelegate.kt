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
import com.example.optoapp.data.OpticaFiscalSettings
import com.example.optoapp.data.OpticaFiscalSettingsStore
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
    private val fiscalStore: OpticaFiscalSettingsStore,
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

        /** Pure logic: check if a timestamp is within the session window (3h) of now. */
        fun isTimestampWithinSessionWindow(lastLoginTimestamp: Long): Boolean {
            if (lastLoginTimestamp == 0L) return false
            val diffHours = (System.currentTimeMillis() - lastLoginTimestamp) / (1000 * 60 * 60)
            return diffHours < 3
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
        isTimestampWithinSessionWindow(sessionManager.lastLoginTimestamp.first())

    //── Login ─────────────────────────────────────────────────────────────────

    suspend fun login(email: String, password: String) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        // Guardar el access token inmediatamente después del login
        pendingAccessToken = runCatching {
            supabase.auth.currentSessionOrNull()?.accessToken
        }.getOrNull().orEmpty()
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
        repeat(20) {
            val session = runCatching { supabase.auth.currentSessionOrNull() }.getOrNull()
            if (session != null) {
                hasSession = true
                return@repeat
            }
            delay(300)
        }

        if (hasSession) {
            // No refrescar: la sesión es nueva y el refresh token puede no estar listo
            return null // success — no error message
        }

        return handleResult.exceptionOrNull()?.localizedMessage
            ?: "No se pudo recuperar la sesión de Google. Reintenta el acceso."
    }

    //── Register ──────────────────────────────────────────────────────────────

    /**
     * Registra con email y espera la sesión. Si hay confirmación de email,
     * la sesión no se establece inmediatamente y retorna un mensaje indicando
     * que revise su correo.
     *
     * @return null si ok (sesión lista), o un mensaje de error/información.
     */
    suspend fun register(email: String, password: String): String? {
        return try {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            // Esperar hasta 6s a que la sesión se establezca
            repeat(20) {
                val session = runCatching { supabase.auth.currentSessionOrNull() }.getOrNull()
                if (session != null) {
                    val user = supabase.auth.currentUserOrNull()
                    if (user != null) return null // success
                }
                delay(300)
            }
            // No se pudo establecer sesión después del registro
            "No se pudo crear la cuenta. Reintenta con otro correo o verifica tu conexión."
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Registro: error de red", e)
            "No se pudo crear la cuenta: ${e.localizedMessage ?: "error desconocido"}"
        } catch (e: Exception) {
            Log.e(TAG, "Registro", e)
            val msg = e.localizedMessage ?: "error desconocido"
            when {
                msg.contains("password", ignoreCase = true) ->
                    "La contraseña debe tener al menos: minúsculas, MAYÚSCULAS, números y símbolos especiales."
                else -> "No se pudo crear la cuenta: $msg"
            }
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
    private var pendingUserId: String = ""
    private var pendingAccessToken: String = ""

    suspend fun resolvePostLogin(
        emailFallback: String? = null,
        nameFallback: String? = null
    ): PostLoginResult {
        var user = supabase.auth.currentUserOrNull()
        if (user == null) {
            // Reintentar con espera progresiva (sin refrescar, la sesión ya debería estar)
            for (attempt in 1..5) {
                delay(attempt * 400L)
                user = supabase.auth.currentUserOrNull()
                if (user != null) break
            }
        }
        val finalUser = user ?: throw IllegalStateException("No se encontró usuario autenticado")
        val email = finalUser.email?.trim().orEmpty().ifBlank { emailFallback.orEmpty() }
        val nombre = Companion.extractDisplayName(finalUser, emailFallback, nameFallback)

        pendingLoginEmail = email
        pendingLoginName = nombre
        pendingUserId = finalUser.id
        if (pendingAccessToken.isBlank()) {
            pendingAccessToken = runCatching {
                supabase.auth.currentSessionOrNull()?.accessToken
            }.getOrNull().orEmpty()
        }

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

    /**
     * Valida la sesión al iniciar la app.
     *
     * 1. Si hay sesión cachead a local, intenta refrescar el JWT contra Supabase.
     *    - Éxito → sesión válida, restaura estado.
     *    - Error de red → fallback al timestamp local (ventana de 3h).
     *    - Otro error → sesión inválida, logout.
     * 2. Si no hay sesión local → logout directo.
     *
     * Nota: cuando el refresh token expiró, Supabase devuelve una sesión anónima
     * (role: "anon") en vez de lanzar error. Por eso NO basta con verificar
     * accessToken — también se verifica currentUserOrNull() post-refresh.
     */
    suspend fun checkExistingSession(): Boolean {
        val session = supabase.auth.currentSessionOrNull()
        if (session == null) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Sin sesión local. Limpiando...")
            logout()
            return false
        }

        return try {
            supabase.auth.refreshCurrentSession()
            val refreshed = supabase.auth.currentSessionOrNull()
            if (refreshed?.accessToken.isNullOrBlank()) {
                if (BuildConfig.DEBUG) Log.d(TAG, "JWT vacío tras refresh. Limpiando...")
                logout()
                false
            } else if (supabase.auth.currentUserOrNull() == null) {
                // Refresh devolvió sesión anónima (refresh token inválido/expirado)
                if (BuildConfig.DEBUG) Log.d(TAG, "Sesión anónima tras refresh. Limpiando...")
                logout()
                false
            } else {
                if (BuildConfig.DEBUG) Log.d(TAG, "JWT validado contra Supabase. Sesión activa.")
                val oid = sessionManager.opticaId.first()
                repository.reassignLegacyMiOpticaBaseTo(oid)
                rescheduleFutureRemindersIfEnabled(oid)
                true
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            // Sin conexión: fallback al timestamp local (3h)
            if (BuildConfig.DEBUG) Log.d(TAG, "Sin red al validar JWT, fallback a timestamp local.")
            if (!isSessionTimeValid()) {
                logout()
                false
            } else {
                val oid = sessionManager.opticaId.first()
                repository.reassignLegacyMiOpticaBaseTo(oid)
                rescheduleFutureRemindersIfEnabled(oid)
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error validando sesión contra Supabase: ${e.message}", e)
            logout()
            false
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
            direccionFiscal = direccionFiscal,
            userId = pendingUserId.ifBlank { null },
            overrideAccessToken = pendingAccessToken.ifBlank { null }
        )
        if (result.isSuccess) {
            val m = result.getOrNull()!!
            sessionManager.saveSession(
                opticaId = m.opticaId,
                email = email,
                name = name,
                rol = "admin"
            )
            // Guardar datos fiscales localmente para que aparezcan en Configuración
            runCatching {
                fiscalStore.save(m.opticaId, OpticaFiscalSettings(
                    nombreComercial = nombreOptica.trim(),
                    docTipo = fiscalDocTipo.trim().uppercase(),
                    docNumero = fiscalDocNumero.trim(),
                    razonSocial = razonSocial.trim(),
                    direccionFiscal = direccionFiscal.trim()
                ))
            }
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

    //── Recordar Cuenta ────────────────────────────────────────────────────

    suspend fun saveRememberedEmail(email: String) {
        sessionManager.saveRememberedEmail(email)
    }

    suspend fun getRememberedEmail(): String = sessionManager.getRememberedEmail()

    suspend fun clearRememberedEmail() {
        sessionManager.clearRememberedEmail()
    }

    suspend fun saveRememberedPassword(password: String) {
        sessionManager.saveRememberedPassword(password)
    }

    suspend fun getRememberedPassword(): String = sessionManager.getRememberedPassword()

    suspend fun clearRememberedPassword() {
        sessionManager.clearRememberedPassword()
    }

    //── Resolver duplicados (admin/gerente) ────────────────────────────────

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
