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
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    val remindersEnabled: StateFlow<Boolean> = reminderSettingsStore.enableRemindersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            reminderSettingsStore.setEnableReminders(enabled)
        }
    }

    fun sendTestNotification() {
        notificationHelper.showNotification("Prueba OptoApp")
    }
}
