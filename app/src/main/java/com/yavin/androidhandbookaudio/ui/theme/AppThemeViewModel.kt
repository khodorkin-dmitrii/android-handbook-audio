package com.yavin.androidhandbookaudio.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yavin.androidhandbookaudio.domain.model.AppThemeMode
import com.yavin.androidhandbookaudio.domain.repository.ThemePreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AppThemeViewModel @Inject constructor(
    private val themePreferencesRepository: ThemePreferencesRepository,
) : ViewModel() {
    private val mutableThemeMode = MutableStateFlow<AppThemeMode?>(null)
    val themeMode: StateFlow<AppThemeMode?> = mutableThemeMode.asStateFlow()

    init {
        viewModelScope.launch {
            themePreferencesRepository.themeMode.collect(mutableThemeMode::emit)
        }
    }

    fun cycleThemeMode() {
        val nextMode = mutableThemeMode.value?.next() ?: return
        mutableThemeMode.value = nextMode
        viewModelScope.launch {
            themePreferencesRepository.saveThemeMode(nextMode)
        }
    }
}
