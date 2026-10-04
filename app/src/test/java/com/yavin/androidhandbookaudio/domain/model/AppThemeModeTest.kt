package com.yavin.androidhandbookaudio.domain.model

import com.yavin.androidhandbookaudio.ui.theme.resolveDarkTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppThemeModeTest {
    @Test
    fun `system cycles to light`() {
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.SYSTEM.next())
    }

    @Test
    fun `light cycles to dark`() {
        assertEquals(AppThemeMode.DARK, AppThemeMode.LIGHT.next())
    }

    @Test
    fun `dark cycles to system`() {
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.DARK.next())
    }

    @Test
    fun `system follows current system theme`() {
        assertTrue(AppThemeMode.SYSTEM.resolveDarkTheme(systemInDarkTheme = true))
        assertFalse(AppThemeMode.SYSTEM.resolveDarkTheme(systemInDarkTheme = false))
    }

    @Test
    fun `light always forces light theme`() {
        assertFalse(AppThemeMode.LIGHT.resolveDarkTheme(systemInDarkTheme = true))
        assertFalse(AppThemeMode.LIGHT.resolveDarkTheme(systemInDarkTheme = false))
    }

    @Test
    fun `dark always forces dark theme`() {
        assertTrue(AppThemeMode.DARK.resolveDarkTheme(systemInDarkTheme = true))
        assertTrue(AppThemeMode.DARK.resolveDarkTheme(systemInDarkTheme = false))
    }
}
