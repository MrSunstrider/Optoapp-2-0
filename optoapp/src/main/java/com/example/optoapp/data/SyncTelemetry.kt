package com.example.optoapp.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.optoapp.util.SyncErrorSanitizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * P0-T3 (operación, DONE): telemetría global de última sync. Estado por fila queda para P0-T5 si hace falta.
 */
@Singleton
class SyncTelemetry @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val keyLastSuccessMs = longPreferencesKey("sync_last_full_success_ms")
    private val keyLastError = stringPreferencesKey("sync_last_full_error")

    val lastSuccessTimestampMs: Flow<Long?> = context.dataStore.data.map { it[keyLastSuccessMs] }
    val lastErrorMessage: Flow<String?> = context.dataStore.data.map { it[keyLastError] }

    suspend fun recordFullSyncSuccess() {
        context.dataStore.edit { prefs ->
            prefs[keyLastSuccessMs] = System.currentTimeMillis()
            prefs.remove(keyLastError)
        }
    }

    suspend fun recordFullSyncError(rawMessage: String?) {
        val safe = SyncErrorSanitizer.forUserMessage(rawMessage).take(500)
        context.dataStore.edit { prefs ->
            prefs[keyLastError] = safe
        }
    }
}
