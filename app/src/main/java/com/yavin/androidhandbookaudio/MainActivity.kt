package com.yavin.androidhandbookaudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yavin.androidhandbookaudio.navigation.AndroidHandbookAudioNavHost
import com.yavin.androidhandbookaudio.ui.components.ThemeRevealTransition
import com.yavin.androidhandbookaudio.ui.theme.AppThemeViewModel
import com.yavin.androidhandbookaudio.ui.theme.AndroidHandbookAudioTheme
import com.yavin.androidhandbookaudio.ui.theme.resolveDarkTheme
import com.yavin.androidhandbookaudio.ui.playlists.PlaylistsViewModel
import com.yavin.androidhandbookaudio.ui.player.PlayerViewModel
import com.yavin.androidhandbookaudio.ui.tracks.TrackListViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val playlistsViewModel: PlaylistsViewModel by viewModels()
    private val trackListViewModel: TrackListViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()
    private val appThemeViewModel: AppThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by appThemeViewModel.themeMode.collectAsStateWithLifecycle()
            run {
                val currentThemeMode = themeMode
                val darkTheme = currentThemeMode.resolveDarkTheme(isSystemInDarkTheme())
                ThemeRevealTransition(
                    themeMode = currentThemeMode,
                    darkTheme = darkTheme,
                    onCycleThemeMode = appThemeViewModel::cycleThemeMode,
                ) { requestThemeChange: (Offset) -> Unit ->
                    AndroidHandbookAudioTheme(darkTheme = darkTheme) {
                        AndroidHandbookAudioNavHost(
                            playlistsViewModel = playlistsViewModel,
                            trackListViewModel = trackListViewModel,
                            playerViewModel = playerViewModel,
                            themeMode = currentThemeMode,
                            onCycleThemeMode = requestThemeChange,
                        )
                    }
                }
            }
        }
    }
}
