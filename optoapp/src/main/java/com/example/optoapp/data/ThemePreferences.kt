package com.example.optoapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.optoapp.domain.AppThemeMode
import com.example.optoapp.domain.ThemeModeStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

class ThemePreferences(
    context: Context,
    private val dataStore: DataStore<Preferences> = context.themeDataStore,
) : ThemeModeStore {
    override val mode: Flow<AppThemeMode> = dataStore.data
        .map { prefs -> AppThemeMode.parse(prefs[KEY_THEME_MODE]) }
        .distinctUntilChanged()

    override suspend fun setMode(mode: AppThemeMode) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.storageValue()
        }
    }

    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("pref_app_theme_mode")
    }
}
