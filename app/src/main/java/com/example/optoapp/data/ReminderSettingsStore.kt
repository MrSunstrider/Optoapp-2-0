package com.example.optoapp.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ReminderSettingsStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val keyEnableReminders = booleanPreferencesKey("pref_enable_reminders")

    val enableRemindersFlow: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[keyEnableReminders] ?: true }

    suspend fun setEnableReminders(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[keyEnableReminders] = enabled
        }
    }
}
