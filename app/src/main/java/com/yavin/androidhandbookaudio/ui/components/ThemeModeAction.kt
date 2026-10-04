package com.yavin.androidhandbookaudio.ui.components

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.yavin.androidhandbookaudio.domain.model.AppThemeMode

@Composable
fun ThemeModeAction(
    themeMode: AppThemeMode,
    onCycleThemeMode: (Offset) -> Unit,
) {
    var buttonCenter by remember { mutableStateOf(Offset.Unspecified) }
    TextButton(
        onClick = { onCycleThemeMode(buttonCenter) },
        modifier = Modifier.onGloballyPositioned { coordinates ->
            buttonCenter = coordinates.boundsInRoot().center
        },
    ) {
        Text(themeMode.label)
    }
}

private val AppThemeMode.label: String
    get() = when (this) {
        AppThemeMode.SYSTEM -> "System"
        AppThemeMode.LIGHT -> "Light"
        AppThemeMode.DARK -> "Dark"
    }
