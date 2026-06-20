package com.example.optoapp.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpticaFiscalSettingsStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private fun keyNombreComercial(opticaId: String) = stringPreferencesKey("fiscal_nombre_comercial_" + sanitize(opticaId))
    private fun keyDocTipo(opticaId: String) = stringPreferencesKey("fiscal_doc_tipo_" + sanitize(opticaId))
    private fun keyDocNumero(opticaId: String) = stringPreferencesKey("fiscal_doc_numero_" + sanitize(opticaId))
    private fun keyRazonSocial(opticaId: String) = stringPreferencesKey("fiscal_razon_social_" + sanitize(opticaId))
    private fun keyDireccionFiscal(opticaId: String) = stringPreferencesKey("fiscal_direccion_" + sanitize(opticaId))

    private fun sanitize(opticaId: String): String = opticaId.replace(Regex("[^a-zA-Z0-9_]"), "_")

    fun settingsFlow(opticaId: String): Flow<OpticaFiscalSettings> =
        context.dataStore.data.map { prefs ->
            OpticaFiscalSettings(
                nombreComercial = prefs[keyNombreComercial(opticaId)] ?: "",
                docTipo = prefs[keyDocTipo(opticaId)] ?: "",
                docNumero = prefs[keyDocNumero(opticaId)] ?: "",
                razonSocial = prefs[keyRazonSocial(opticaId)] ?: "",
                direccionFiscal = prefs[keyDireccionFiscal(opticaId)] ?: ""
            )
        }

    suspend fun save(opticaId: String, settings: OpticaFiscalSettings) {
        context.dataStore.edit { prefs ->
            prefs[keyNombreComercial(opticaId)] = settings.nombreComercial.trim()
            prefs[keyDocTipo(opticaId)] = settings.docTipo.trim().uppercase()
            prefs[keyDocNumero(opticaId)] = settings.docNumero.trim()
            prefs[keyRazonSocial(opticaId)] = settings.razonSocial.trim()
            prefs[keyDireccionFiscal(opticaId)] = settings.direccionFiscal.trim()
        }
    }
}
