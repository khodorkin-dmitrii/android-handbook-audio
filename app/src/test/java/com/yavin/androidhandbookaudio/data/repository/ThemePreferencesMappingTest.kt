package com.yavin.androidhandbookaudio.data.repository

import androidx.datastore.preferences.core.preferencesOf
import com.yavin.androidhandbookaudio.domain.model.AppThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemePreferencesMappingTest {
    @Test
    fun `missing theme mode defaults to system`() {
        assertEquals(AppThemeMode.SYSTEM, preferencesOf().toAppThemeMode())
    }

    @Test
    fun `invalid stored theme mode defaults to system`() {
        val preferences = preferencesOf(ThemePreferenceKeys.THEME_MODE to "sepia")

        assertEquals(AppThemeMode.SYSTEM, preferences.toAppThemeMode())
    }

    @Test
    fun `all theme modes survive persistence representation round trip`() {
        AppThemeMode.entries.forEach { mode ->
            val preferences = preferencesOf(
                ThemePreferenceKeys.THEME_MODE to mode.storageValue,
            )

            assertEquals(mode, preferences.toAppThemeMode())
        }
    }
}
