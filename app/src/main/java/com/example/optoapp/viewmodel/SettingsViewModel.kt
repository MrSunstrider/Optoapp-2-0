package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.ReminderSettingsStore
import com.example.optoapp.notifications.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val reminderSettingsStore: ReminderSettingsStore,
    private val notificationHelper: NotificationHelper,
    private val sessionManager: com.example.optoapp.data.SessionManager
) : ViewModel() {

    val remindersEnabled: StateFlow<Boolean> = reminderSettingsStore.enableRemindersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val userTimeZone: StateFlow<String?> = sessionManager.userTimeZone
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            reminderSettingsStore.setEnableReminders(enabled)
        }
    }

    fun setUserTimeZone(timeZoneId: String?) {
        viewModelScope.launch {
            sessionManager.setUserTimeZone(timeZoneId)
        }
    }

    fun sendTestNotification() {
        notificationHelper.showNotification("Prueba OptoApp")
    }
}
