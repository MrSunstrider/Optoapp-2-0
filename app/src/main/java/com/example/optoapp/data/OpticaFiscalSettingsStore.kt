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
    private fun keyDocTipo(opticaId: String) = stringPreferencesKey("fiscal_doc_tipo_" + sanitize(opticaId))
    private fun keyDocNumero(opticaId: String) = stringPreferencesKey("fiscal_doc_numero_" + sanitize(opticaId))
    private fun keyRazonSocial(opticaId: String) = stringPreferencesKey("fiscal_razon_social_" + sanitize(opticaId))
    private fun keyDireccionFiscal(opticaId: String) = stringPreferencesKey("fiscal_direccion_" + sanitize(opticaId))
    private fun keyDistritoCiudadDepartamento(opticaId: String) =
        stringPreferencesKey("fiscal_distrito_ciudad_departamento_" + sanitize(opticaId))
    private fun keyMoneda(opticaId: String) = stringPreferencesKey("fiscal_moneda_" + sanitize(opticaId))
    private fun keyPais(opticaId: String) = stringPreferencesKey("fiscal_pais_" + sanitize(opticaId))
    private fun keyContactoWhatsappTelefono(opticaId: String) =
        stringPreferencesKey("fiscal_contacto_whatsapp_telefono_" + sanitize(opticaId))

    private fun sanitize(opticaId: String): String = opticaId.replace(Regex("[^a-zA-Z0-9_]"), "_")

    fun settingsFlow(opticaId: String): Flow<OpticaFiscalSettings> =
        context.dataStore.data.map { prefs ->
            OpticaFiscalSettings(
                docTipo = prefs[keyDocTipo(opticaId)] ?: "",
                docNumero = prefs[keyDocNumero(opticaId)] ?: "",
                razonSocial = prefs[keyRazonSocial(opticaId)] ?: "",
                direccionFiscal = prefs[keyDireccionFiscal(opticaId)] ?: "",
                distritoCiudadDepartamento = prefs[keyDistritoCiudadDepartamento(opticaId)] ?: "",
                moneda = prefs[keyMoneda(opticaId)] ?: "",
                pais = prefs[keyPais(opticaId)] ?: "",
                contactoWhatsappTelefono = prefs[keyContactoWhatsappTelefono(opticaId)] ?: ""
            )
        }

    suspend fun save(opticaId: String, settings: OpticaFiscalSettings) {
        context.dataStore.edit { prefs ->
            prefs[keyDocTipo(opticaId)] = settings.docTipo.trim().uppercase()
            prefs[keyDocNumero(opticaId)] = settings.docNumero.trim()
            prefs[keyRazonSocial(opticaId)] = settings.razonSocial.trim()
            prefs[keyDireccionFiscal(opticaId)] = settings.direccionFiscal.trim()
            prefs[keyDistritoCiudadDepartamento(opticaId)] = settings.distritoCiudadDepartamento.trim()
            prefs[keyMoneda(opticaId)] = settings.moneda.trim()
            prefs[keyPais(opticaId)] = settings.pais.trim()
            prefs[keyContactoWhatsappTelefono(opticaId)] = settings.contactoWhatsappTelefono.trim()
        }
    }
}
