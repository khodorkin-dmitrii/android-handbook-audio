package com.yavin.androidhandbookaudio.domain.model

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    fun next(): AppThemeMode = when (this) {
        SYSTEM -> LIGHT
        LIGHT -> DARK
        DARK -> SYSTEM
    }
}
