package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.ReminderSettingsStore
import com.example.optoapp.data.SecurityManager
import com.example.optoapp.data.SessionManager
import com.example.optoapp.notifications.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val reminderSettingsStore: ReminderSettingsStore,
    private val notificationHelper: NotificationHelper,
    private val sessionManager: SessionManager,
    private val securityManager: SecurityManager,
) : ViewModel() {

    // ─── Notificaciones ──────────────────────────────────────────────────────

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

    // ─── PIN ─────────────────────────────────────────────────────────────────

    val pinHasBeenSet: StateFlow<Boolean> = sessionManager.pinHasBeenSet
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val isPinRequired: StateFlow<Boolean> = sessionManager.isPinRequired
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun togglePinRequired(enabled: Boolean) = viewModelScope.launch {
        sessionManager.setPinRequired(enabled)
    }

    fun updatePin(oldPin: String, newPin: String) = viewModelScope.launch {
        val currentPin = securityManager.userPin.first()
        if (oldPin != currentPin) return@launch
        if (!SecurityManager.isValidPin(newPin)) return@launch
        securityManager.savePin(newPin)
    }
}
