package com.example.optoapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SecurityManager(private val context: Context) {
    private val USER_PIN = stringPreferencesKey("user_pin")

    val userPin: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_PIN] ?: "1234"
    }

    suspend fun savePin(pin: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_PIN] = pin
        }
    }
}
