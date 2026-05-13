package com.example.optoapp.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory [DataStore] backed by [MutablePreferences] for unit tests.
 *
 * Implements [updateData] so that the `edit {}` extension function works transparently.
 * All operations happen synchronously on the calling thread — no I/O, no delays.
 */
internal class FakeDataStore : DataStore<Preferences> {

    private val prefs = mutablePreferencesOf()
    private val _data = MutableStateFlow(prefs.toPreferences())

    override val data: Flow<Preferences> = _data.asStateFlow()

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        val result = transform(prefs.toPreferences())
        prefs.clear()
        result.asMap().forEach { (key, value) ->
            @Suppress("UNCHECKED_CAST")
            prefs[key as Preferences.Key<Any>] = value
        }
        val updated = prefs.toPreferences()
        _data.value = updated
        return updated
    }
}
