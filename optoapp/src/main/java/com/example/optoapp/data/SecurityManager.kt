package com.example.optoapp.data

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.util.concurrent.atomic.AtomicBoolean

val Context.dataStore by preferencesDataStore(name = "settings")

interface ISecurityManager {
    val userPin: Flow<String>
    val pinHasBeenSet: Flow<Boolean>
    suspend fun savePin(pin: String)
}

class SecurityManager(
    private val context: Context,
    private val dataStore: DataStore<Preferences> = context.dataStore,
    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_security_prefs",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
) : ISecurityManager {

    companion object {
        /**
         * PIN de desarrollo usado SOLO cuando no hay PIN almacenado.
         * NUNCA se asigna automáticamente — el usuario crea su PIN en CreatePinScreen.
         * Este valor existe únicamente para evitar nulls en flujos de desarrollo/test.
         */
        const val DEV_FALLBACK_PIN = "999999"
        const val PIN_LENGTH = 6

        fun isValidPin(pin: String): Boolean {
            if (pin.length != PIN_LENGTH || pin.any { !it.isDigit() }) return false
            return pin !in weakPinPatterns
        }

        private val weakPinPatterns = setOf(
            "000000", "111111", "222222", "333333", "444444",
            "555555", "666666", "777777", "888888", "999999",
            "123456", "234567", "345678", "456789",
            "654321", "543210"
        )
    }

    private val prefPinHasBeenSet = booleanPreferencesKey("pref_pin_has_been_set")
    private val migrationRan = AtomicBoolean(false)

    override val pinHasBeenSet: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[prefPinHasBeenSet] ?: false }
        .onStart {
            if (migrationRan.compareAndSet(false, true)) {
                migratePinHasBeenSet()
            }
        }
        .distinctUntilChanged()

    private val _pinFlow = MutableStateFlow(getSecurePin())

    override val userPin: Flow<String> = _pinFlow

    private suspend fun migratePinHasBeenSet() {
        val alreadySet = dataStore.data.first()[prefPinHasBeenSet] ?: false
        if (alreadySet) return
        val stored = getSecurePin()
        // El PIN de desarrollo no cuenta como "seteado por el usuario".
        if (stored.isNotEmpty() && stored != DEV_FALLBACK_PIN) {
            dataStore.edit { prefs ->
                prefs[prefPinHasBeenSet] = true
            }
        }
    }

    private fun getSecurePin(): String {
        return encryptedPrefs.getString("user_pin", "") ?: ""
    }

    suspend fun getStoredPin(): String {
        if (!pinHasBeenSet.first()) return ""
        return getSecurePin()
    }

    override suspend fun savePin(pin: String) {
        encryptedPrefs.edit { putString("user_pin", pin) }
        _pinFlow.value = pin
        
        dataStore.edit { prefs -> 
            prefs[prefPinHasBeenSet] = true
            prefs.remove(stringPreferencesKey("user_pin"))
        }
    }
}
