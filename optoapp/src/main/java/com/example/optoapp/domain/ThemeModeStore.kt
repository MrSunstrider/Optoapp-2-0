package com.example.optoapp.domain

import kotlinx.coroutines.flow.Flow

interface ThemeModeStore {
    val mode: Flow<AppThemeMode>
    suspend fun setMode(mode: AppThemeMode)
}
