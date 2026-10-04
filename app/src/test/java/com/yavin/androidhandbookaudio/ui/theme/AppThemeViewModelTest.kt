package com.yavin.androidhandbookaudio.ui.theme

import com.yavin.androidhandbookaudio.domain.model.AppThemeMode
import com.yavin.androidhandbookaudio.domain.repository.ThemePreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppThemeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `cycles current mode and persists each selection`() = runTest(dispatcher) {
        val repository = FakeThemePreferencesRepository(AppThemeMode.SYSTEM)
        val viewModel = AppThemeViewModel(repository)
        runCurrent()

        viewModel.cycleThemeMode()
        assertEquals(AppThemeMode.LIGHT, viewModel.themeMode.value)
        advanceUntilIdle()

        viewModel.cycleThemeMode()
        assertEquals(AppThemeMode.DARK, viewModel.themeMode.value)
        advanceUntilIdle()

        viewModel.cycleThemeMode()
        assertEquals(AppThemeMode.SYSTEM, viewModel.themeMode.value)
        advanceUntilIdle()

        assertEquals(
            listOf(AppThemeMode.LIGHT, AppThemeMode.DARK, AppThemeMode.SYSTEM),
            repository.savedModes,
        )
    }
}

private class FakeThemePreferencesRepository(initialMode: AppThemeMode) :
    ThemePreferencesRepository {
    private val mutableThemeMode = MutableStateFlow(initialMode)
    override val themeMode = mutableThemeMode.asStateFlow()
    val savedModes = mutableListOf<AppThemeMode>()

    override suspend fun saveThemeMode(mode: AppThemeMode) {
        savedModes += mode
        mutableThemeMode.value = mode
    }
}
