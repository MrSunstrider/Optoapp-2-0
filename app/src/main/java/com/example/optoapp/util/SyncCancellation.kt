package com.example.optoapp.util

import kotlinx.coroutines.CancellationException

/**
 * Las cancelaciones de corrutina no son fallos de red/datos. La sync pesada debe ejecutarse fuera del
 * [androidx.lifecycle.viewmodel.compose.viewModelScope] de pantallas (ver [com.example.optoapp.sync.PostSaveSyncScheduler]).
 * Este helper evita registrar cancelaciones residuales (p. ej. cierre del proceso) como error de sync.
 */
fun rethrowIfCancellation(e: Throwable) {
    if (e is CancellationException) throw e
}
