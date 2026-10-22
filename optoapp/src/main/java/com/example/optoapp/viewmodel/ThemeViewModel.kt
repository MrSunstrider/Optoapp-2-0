package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.domain.AppThemeMode
import com.example.optoapp.domain.ThemeModeStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeModeStore: ThemeModeStore,
) : ViewModel() {

    val mode: StateFlow<AppThemeMode> = themeModeStore.mode.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppThemeMode.SYSTEM,
    )

    fun toggle(systemDark: Boolean) {
        val next = AppThemeMode.afterToggle(mode.value, systemDark)
        viewModelScope.launch {
            themeModeStore.setMode(next)
        }
    }
}
