package com.example.optoapp.viewmodel

import android.content.Intent
import android.util.Log
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.BuildConfig
import com.example.optoapp.domain.SyncSessionHelper
import com.example.optoapp.data.MembershipRepository
import com.example.optoapp.data.OpticaMembership
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SecurityManager
import com.example.optoapp.data.SessionManager
import com.example.optoapp.util.BackupImportValidator
import com.example.optoapp.notifications.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

sealed class AuthState {
    object Idle       : AuthState()
    object Loading    : AuthState()
    object Success    : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val securityManager: SecurityManager,
    private val sessionManager: SessionManager,
    private val repository: OptoRepository,
    private val membershipRepository: MembershipRepository,
    private val supabase: SupabaseClient,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    companion object { private const val TAG = "AuthViewModel" }

    // ─── Estado PIN (sin cambios) ─────────────────────────────────────────────

    private val userPin = securityManager.userPin
    private val _pinInput = MutableStateFlow("")
    val pinInput: StateFlow<String> = _pinInput

    fun onPinDigit(digit: String) {
        if (_pinInput.value.length < SecurityManager.PIN_LENGTH) _pinInput.value += digit
    }

    fun clearPin() { _pinInput.value = "" }

    suspend fun validatePin(): Boolean = _pinInput.value == userPin.first()

    fun updatePin(oldPin: String, newPin: String) = viewModelScope.launch {
        if (oldPin != userPin.first()) return@launch
        if (newPin.length != SecurityManager.PIN_LENGTH) return@launch
        securityManager.savePin(newPin)
    }

    // ─── Estado Supabase Auth ─────────────────────────────────────────────────

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /** Flujo reactivo: ¿hay sesión SaaS activa? */
    val isLoggedIn = sessionManager.isLoggedIn

    /** opticaId activo, usado por los Use Cases de sincronización. */
    val opticaId = sessionManager.opticaId

    /** Rol en la óptica activa (`usuario_optica`), para ocultar secciones en el drawer. */
    val opticaRol = sessionManager.opticaRol

    private val _isAuthChecked = MutableStateFlow(false)
    val isAuthChecked = _isAuthChecked.asStateFlow()
    private val _needsOnboarding = MutableStateFlow(false)
    val needsOnboarding = _needsOnboarding.asStateFlow()

    val userEmail = sessionManager.userEmail
    val userName  = sessionManager.userName

    /** Flujo para la pantalla de configuración: ¿el PIN es obligatorio? */
    val isPinRequired = sessionManager.isPinRequired

    /** Zona horaria preferida por el usuario (Manual Override) */
    val userTimeZone = sessionManager.userTimeZone

    /** Si hay más de una óptica, el usuario debe elegir en [SeleccionOpticaScreen]. */
    private val _pendingMemberships = MutableStateFlow<List<OpticaMembership>>(emptyList())
    val pendingMemberships: StateFlow<List<OpticaMembership>> = _pendingMemberships.asStateFlow()

    private var pendingLoginEmail: String = ""
    private var pendingLoginName: String = ""

    fun togglePinRequired(enabled: Boolean) = viewModelScope.launch {
        sessionManager.setPinRequired(enabled)
    }

    /** 
     * Verifica si la sesión SaaS ha expirado basándose en tiempo (24h).
     * Retorna TRUE si la sesión SIGUE siendo válida.
     */
    suspend fun isSessionTimeValid(): Boolean {
        val lastTs = sessionManager.lastLoginTimestamp.first()
        if (lastTs == 0L) return false
        
        val now = System.currentTimeMillis()
        val diffHours = (now - lastTs) / (1000 * 60 * 60)
        
        return diffHours < 24
    }

    // ─── Login ────────────────────────────────────────────────────────────────

    /**
     * Autentica con email/contraseña en Supabase.
     * Óptica activa: tabla `usuario_optica` (multi-óptica). Si no hay filas, fallback a metadata/uid (legado).
     */
    fun login(email: String, password: String) = viewModelScope.launch {
        _authState.value = AuthState.Loading
        _pendingMemberships.value = emptyList()
        try {
            supabase.auth.signInWith(Email) {
                this.email    = email
                this.password = password
            }

            resolvePostLogin(
                emailFallback = email,
                nameFallback = email.substringBefore("@")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error de login", e)
            _authState.value = AuthState.Error(
                when {
                    e.message?.contains("Invalid login credentials", ignoreCase = true) == true ->
                        "Email o contraseña incorrectos"
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "Sin conexión a internet"
                    else -> "Error: ${e.localizedMessage}"
                }
            )
        }
    }

    fun loginWithGoogle() = viewModelScope.launch {
        _authState.value = AuthState.Loading
        _pendingMemberships.value = emptyList()
        try {
            supabase.auth.signInWith(Google)
            Log.d(TAG, "Inicio de OAuth Google lanzado")
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando login con Google", e)
            _authState.value = AuthState.Error("No se pudo iniciar Google: ${e.localizedMessage}")
        }
    }

    fun handleAuthDeepLinkIntent(intent: Intent?) = viewModelScope.launch {
        val deepLink = intent?.data ?: return@launch
        _authState.value = AuthState.Loading
        Log.d(TAG, "Recibido deeplink OAuth: $deepLink")

        val handleResult = runCatching { supabase.handleDeeplinks(intent) }
        handleResult.onFailure { e ->
            Log.w(TAG, "No se pudo procesar deeplink OAuth: ${e.localizedMessage}", e)
        }

        // En algunos dispositivos/custom tabs la sesión tarda unos milisegundos en persistirse.
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
            // Fuerza JWT actualizado en el cliente antes de PostgREST (evita peticiones como `anon` tras Custom Tabs).
            SyncSessionHelper.refreshSessionBeforeSync(supabase)
            runCatching { resolvePostLogin() }.onFailure { e ->
                Log.e(TAG, "Error cerrando OAuth Google", e)
                _authState.value = AuthState.Error("No se pudo completar Google: ${e.localizedMessage}")
            }
            return@launch
        }

        val message = handleResult.exceptionOrNull()?.localizedMessage
            ?: "No se pudo recuperar la sesión de Google. Reintenta el acceso."
        _authState.value = AuthState.Error(message)
        Log.w(TAG, "OAuth completado sin sesión activa. deeplink=$deepLink")
    }

    /**
     * Registro básico de cuenta.
     * El rol NO se define aquí: lo asigna un admin/gerente desde gestión de usuarios.
     */
    fun register(email: String, password: String, onFinished: (String) -> Unit) = viewModelScope.launch {
        try {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            // Evitar que la sesión de registro pise la sesión operativa actual.
            runCatching { supabase.auth.signOut() }
            onFinished("Cuenta creada. Ahora un admin/gerente debe asignarte rol en la óptica.")
        } catch (e: Exception) {
            onFinished("No se pudo crear la cuenta: ${e.localizedMessage ?: "error desconocido"}")
        }
    }

    /** Tras elegir óptica en pantalla de selección (solo si hay varias membresías). */
    suspend fun selectOptica(membership: OpticaMembership) {
        _needsOnboarding.value = false
        sessionManager.saveSession(
            opticaId = membership.opticaId,
            email = pendingLoginEmail,
            name = pendingLoginName,
            rol = membership.rol
        )
        repository.reassignLegacyMiOpticaBaseTo(membership.opticaId)
        _pendingMemberships.value = emptyList()
        if (BuildConfig.DEBUG) {
            val uid = try { supabase.auth.currentUserOrNull()?.id } catch (_: Exception) { null }
            Log.d(TAG, "Óptica seleccionada: ${membership.opticaId} rol=${membership.rol} uid=$uid")
        }
        _authState.value = AuthState.Success
    }

    /**
     * Prepara la pantalla de selección de óptica en sesión activa.
     * Retorna true si hay más de una membresía para elegir.
     */
    suspend fun prepareOpticaSelection(): Boolean {
        val memberships = membershipRepository.fetchMembershipsForCurrentUser()
        _pendingMemberships.value = memberships
        return memberships.size > 1
    }

    // ─── Logout ───────────────────────────────────────────────────────────────

    /** Cierra sesión en Supabase y borra datos locales; debe llamarse desde una corrutina. */
    suspend fun logout() {
        try {
            supabase.auth.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Error en signOut (ignorado): ${e.localizedMessage}")
        } finally {
            _pendingMemberships.value = emptyList()
            sessionManager.clearSession()
            _authState.value = AuthState.Idle
        }
    }

    // ─── Restaurar sesión al inicio ───────────────────────────────────────────

    /**
     * Verifica si el token guardado en Supabase SDK sigue siendo válido.
     * Si expiró, limpia la sesión local para forzar nuevo login.
     */
    fun checkExistingSession() = viewModelScope.launch {
        try {
            val session = supabase.auth.currentSessionOrNull()
            // Nueva validación: si la sesión no existe en Supabase O el tiempo de 24h expiró
            if (session == null || !isSessionTimeValid()) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Sesión inexistente o expirada por tiempo (24h). Limpiando...")
                }
                logout()
            } else {
                val oid = sessionManager.opticaId.first()
                repository.reassignLegacyMiOpticaBaseTo(oid)
                rescheduleFutureRemindersIfEnabled(oid)
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo verificar sesión existente: ${e.localizedMessage}")
            sessionManager.clearSession()
        } finally {
            _isAuthChecked.value = true
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
            Log.d(TAG, "Reprogramación de recordatorios al inicio: programados=$scheduled cancelados=$cancelled")
        }
    }

    fun completeOnboardingOptica(
        nombreOptica: String,
        fiscalDocTipo: String,
        fiscalDocNumero: String,
        razonSocial: String,
        direccionFiscal: String,
        onFinished: (Boolean, String) -> Unit
    ) = viewModelScope.launch {
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
            _needsOnboarding.value = false
            onFinished(true, "Óptica creada en plan gratuito.")
        } else {
            val raw = result.exceptionOrNull()?.localizedMessage.orEmpty()
            val friendly = when {
                raw.contains("límite de ópticas", ignoreCase = true) ||
                    raw.contains("max_opticas", ignoreCase = true) ->
                    "Has alcanzado el límite de ópticas de tu plan. Actualiza tu plan para crear más sedes."
                raw.contains("permission", ignoreCase = true) ||
                    raw.contains("policy", ignoreCase = true) ||
                    raw.contains("RLS", ignoreCase = true) ->
                    "No tienes permisos para crear una nueva óptica con esta cuenta."
                raw.isBlank() -> "No se pudo crear la óptica."
                else -> raw
            }
            onFinished(false, friendly)
        }
    }

    fun createAdditionalOptica(nombreOptica: String, onFinished: (Boolean, String) -> Unit) = viewModelScope.launch {
        val role = sessionManager.opticaRol.first().trim().lowercase()
        if (role !in setOf("admin", "gerente")) {
            onFinished(false, "Solo admin o gerente pueden crear sucursales.")
            return@launch
        }
        val result = membershipRepository.createOpticaForCurrentUser(nombreOptica)
        if (result.isSuccess) {
            val created = result.getOrNull()
            onFinished(
                true,
                "Sucursal creada: ${created?.nombre ?: nombreOptica.trim()}. Cierra y vuelve a iniciar sesión para elegirla."
            )
        } else {
            val raw = result.exceptionOrNull()?.localizedMessage.orEmpty()
            val friendly = when {
                raw.contains("límite de ópticas", ignoreCase = true) ||
                    raw.contains("max_opticas", ignoreCase = true) ->
                    "Has alcanzado el límite de sucursales de tu plan."
                raw.contains("permission", ignoreCase = true) ||
                    raw.contains("policy", ignoreCase = true) ||
                    raw.contains("RLS", ignoreCase = true) ->
                    "No tienes permisos para crear una nueva sucursal."
                raw.isBlank() -> "No se pudo crear la sucursal."
                else -> raw
            }
            onFinished(false, friendly)
        }
    }

    fun resolveDuplicateHistorias(onFinished: (String) -> Unit) = viewModelScope.launch {
        val rol = sessionManager.opticaRol.first().trim().lowercase()
        if (rol !in setOf("admin", "gerente")) {
            onFinished("Solo admin o gerente pueden resolver duplicados de historia optométrica.")
            return@launch
        }
        runCatching {
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
        }.onSuccess { msg ->
            onFinished(msg)
        }.onFailure { e ->
            onFinished("No se pudieron resolver duplicados HO: ${e.localizedMessage ?: "error desconocido"}")
        }
    }

    // ─── Backup local (sin cambios) ───────────────────────────────────────────

    suspend fun getBackupJson(): String {
        val oid = sessionManager.opticaId.first()
        val rol = sessionManager.opticaRol.first().trim().lowercase()
        if (rol != "admin") {
            throw IllegalStateException("Solo admin puede descargar respaldo total.")
        }
        assertBackupOperationAllowed("export", oid, oid)
        return com.google.gson.Gson().toJson(repository.getBackupDataForOptica(oid))
    }

    fun restoreBackup(json: String, onFinished: (String) -> Unit) = viewModelScope.launch {
        val rol = sessionManager.opticaRol.first().trim().lowercase()
        if (rol != "admin") {
            onFinished("Solo admin puede restaurar respaldos.")
            return@launch
        }
        val data = BackupImportValidator.parse(json).getOrElse { e ->
            onFinished(e.message ?: "No se pudo validar el respaldo.")
            return@launch
        }
        try {
            val currentOpticaId = sessionManager.opticaId.first()
            val sourceOpticaId = data.sourceOpticaId?.trim().orEmpty()
            if (sourceOpticaId.isBlank()) {
                onFinished("Respaldo sin origen de óptica. Exporta uno nuevo con la versión actual.")
                return@launch
            }
            if (!sourceOpticaId.equals(currentOpticaId, ignoreCase = false)) {
                onFinished(
                    "Este respaldo pertenece a otra óptica y no puede restaurarse aquí."
                )
                return@launch
            }
            assertBackupOperationAllowed("restore", sourceOpticaId, currentOpticaId)
            repository.restoreBackup(data, currentOpticaId)
            onFinished("Base de datos restaurada correctamente.")
        } catch (e: Exception) {
            onFinished("Error al restaurar: ${e.message ?: "desconocido"}")
        }
    }

    private suspend fun resolvePostLogin(
        emailFallback: String? = null,
        nameFallback: String? = null
    ) {
        val user = supabase.auth.currentUserOrNull()
            ?: throw IllegalStateException("No se encontró usuario autenticado")
        val uid = user.id
        val email = user.email?.trim().orEmpty().ifBlank { emailFallback.orEmpty() }
        val nombre = extractDisplayName(user, emailFallback, nameFallback)

        pendingLoginEmail = email
        pendingLoginName = nombre

        val memberships = membershipRepository.fetchMembershipsForCurrentUser()
        when {
            memberships.size > 1 -> {
                _pendingMemberships.value = memberships
                _authState.value = AuthState.Success
                Log.d(TAG, "Login: ${memberships.size} ópticas; se requiere selección")
            }
            memberships.size == 1 -> {
                val m = memberships.first()
                sessionManager.saveSession(
                    opticaId = m.opticaId,
                    email = email,
                    name = nombre,
                    rol = m.rol
                )
                repository.reassignLegacyMiOpticaBaseTo(m.opticaId)
                _authState.value = AuthState.Success
                Log.d(TAG, "Login exitoso. opticaId=${m.opticaId} uid=$uid (única membresía)")
            }
            else -> {
                // Evita reutilizar una óptica previa de otra cuenta (estado legacy en dispositivo compartido).
                // Sin membresías, el usuario debe pasar por onboarding antes de sincronizar.
                sessionManager.clearSession()
                _needsOnboarding.value = true
                _authState.value = AuthState.Success
                Log.d(TAG, "Login sin membresías: requiere onboarding de óptica. uid=$uid")
            }
        }
    }

    private fun extractDisplayName(
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

    private suspend fun assertBackupOperationAllowed(
        action: String,
        sourceOpticaId: String,
        targetOpticaId: String
    ) {
        supabase.postgrest.rpc(
            "assert_backup_operation_allowed",
            buildJsonObject {
                put("p_action", action)
                put("p_source_optica_id", sourceOpticaId)
                put("p_target_optica_id", targetOpticaId)
            }
        )
    }
}

