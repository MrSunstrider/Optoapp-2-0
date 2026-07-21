package com.example.optoapp.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Caché local del contacto de laboratorio por [SessionManager.opticaId].
 * La fuente de verdad es Supabase (`opticas.laboratorio_*`); se actualiza al guardar y al sincronizar desde red.
 */
@Singleton
class OpticaLaboratorioSettings @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private fun keyNombre(opticaId: String) = stringPreferencesKey("lab_nombre_" + sanitize(opticaId))

    private fun keyContacto(opticaId: String) = stringPreferencesKey("lab_contacto_" + sanitize(opticaId))

    private fun sanitize(opticaId: String): String = opticaId.replace(Regex("[^a-zA-Z0-9_]"), "_")

    fun settingsFlow(opticaId: String): Flow<Pair<String, String>> = context.dataStore.data.map { prefs ->
        (prefs[keyNombre(opticaId)] ?: "") to (prefs[keyContacto(opticaId)] ?: "")
    }

    suspend fun save(opticaId: String, laboratorioNombre: String, laboratorioContacto: String) {
        context.dataStore.edit { prefs ->
            prefs[keyNombre(opticaId)] = laboratorioNombre.trim()
            prefs[keyContacto(opticaId)] = laboratorioContacto.trim()
        }
    }
}
