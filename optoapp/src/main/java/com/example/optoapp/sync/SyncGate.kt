package com.example.optoapp.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Las cancelaciones de corrutina no son fallos de red/datos.
 */
fun rethrowIfCancellation(e: Throwable) {
    if (e is CancellationException) throw e
}

/**
 * Un solo [Mutex] para toda la sincronización con Supabase.
 */
@Singleton
class SyncGate @Inject constructor() {
    val mutex = Mutex()
}
