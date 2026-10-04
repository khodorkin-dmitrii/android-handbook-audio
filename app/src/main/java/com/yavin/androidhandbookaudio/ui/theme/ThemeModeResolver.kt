package com.yavin.androidhandbookaudio.ui.theme

import com.yavin.androidhandbookaudio.domain.model.AppThemeMode

fun AppThemeMode.resolveDarkTheme(systemInDarkTheme: Boolean): Boolean = when (this) {
    AppThemeMode.SYSTEM -> systemInDarkTheme
    AppThemeMode.LIGHT -> false
    AppThemeMode.DARK -> true
}
