package com.example.optoapp.sync

import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Un solo [Mutex] para toda la sincronización con Supabase: evita carreras y upserts concurrentes.
 */
@Singleton
class SyncGate @Inject constructor() {
    val mutex = Mutex()
}
