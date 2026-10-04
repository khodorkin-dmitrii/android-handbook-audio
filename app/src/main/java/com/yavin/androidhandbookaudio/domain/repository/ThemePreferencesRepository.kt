package com.yavin.androidhandbookaudio.domain.repository

import com.yavin.androidhandbookaudio.domain.model.AppThemeMode
import kotlinx.coroutines.flow.Flow

interface ThemePreferencesRepository {
    val themeMode: Flow<AppThemeMode>

    suspend fun saveThemeMode(mode: AppThemeMode)
}
