package com.example.optoapp.util

import com.example.optoapp.data.BackgroundError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Colector singleton de errores de background (sync, auth, refresh).
 *
 * Cualquier componente puede loguear errores silenciosos que normalmente se
 * tragan con catch-and-log. La UI de diagnóstico los expone para que el
 * usuario (o el desarrollador en debug) pueda ver qué está fallando.
 *
 * Máximo 50 eventos; los más viejos se descartan.
 */
@Singleton
class BackgroundErrorCollector @Inject constructor() {

    companion object {
        private const val MAX_EVENTS = 50
        private const val TAG = "BgErrCollector"
    }

    private val _errors = MutableStateFlow<List<BackgroundError>>(emptyList())
    val errors: StateFlow<List<BackgroundError>> = _errors.asStateFlow()

    /**
     * Registra un error silencioso. Thread-safe (StateFlow es thread-safe en Kotlin).
     */
    fun record(source: String, message: String) {
        val entry = BackgroundError(source = source, message = message)
        _errors.value = (_errors.value + entry).takeLast(MAX_EVENTS)
        android.util.Log.w(TAG, "[$source] $message")
    }

    /** Limpia todo el historial. */
    fun clear() {
        _errors.value = emptyList()
    }
}
