@file:Suppress("DEPRECATION")

package com.example.optoapp.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val Context.dataStore by preferencesDataStore(name = "settings")

class SecurityManager(private val context: Context) {

    companion object {
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
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        "secure_security_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    val pinHasBeenSet: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[prefPinHasBeenSet] ?: false }

    private val _pinFlow = MutableStateFlow(getSecurePin())

    val userPin: Flow<String> = _pinFlow

    init {
        CoroutineScope(Dispatchers.IO).launch {
            migratePinHasBeenSet()
        }
    }

    private suspend fun migratePinHasBeenSet() {
        if (pinHasBeenSet.first()) return
        val stored = getSecurePin()
        if (stored.isNotEmpty() && stored != "123456") {
            context.dataStore.edit { prefs ->
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

    suspend fun savePin(pin: String) {
        encryptedPrefs.edit { putString("user_pin", pin) }
        _pinFlow.value = pin
        
        context.dataStore.edit { prefs -> 
            prefs[prefPinHasBeenSet] = true
            prefs.remove(stringPreferencesKey("user_pin"))
        }
    }
}
