package com.example.optoapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SecurityManager(private val context: Context) {

    companion object {
        /** Longitud fija del PIN en la app. */
        const val PIN_LENGTH = 6
        private const val DEFAULT_PIN = "123456"
    }
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        "secure_security_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _pinFlow = MutableStateFlow(getSecurePin())

    val userPin: Flow<String> = _pinFlow

    private fun getSecurePin(): String {
        return encryptedPrefs.getString("user_pin", DEFAULT_PIN) ?: DEFAULT_PIN
    }

    suspend fun savePin(pin: String) {
        encryptedPrefs.edit().putString("user_pin", pin).apply()
        _pinFlow.value = pin
        
        // Limpiar el antiguo si existe
        context.dataStore.edit { prefs -> 
            prefs.remove(stringPreferencesKey("user_pin"))
        }
    }
}
