package com.yavin.androidhandbookaudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.yavin.androidhandbookaudio.navigation.AndroidHandbookAudioNavHost
import com.yavin.androidhandbookaudio.ui.theme.AndroidHandbookAudioTheme
import com.yavin.androidhandbookaudio.ui.playlists.PlaylistsViewModel
import com.yavin.androidhandbookaudio.ui.player.PlayerViewModel
import com.yavin.androidhandbookaudio.ui.tracks.TrackListViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val playlistsViewModel: PlaylistsViewModel by viewModels()
    private val trackListViewModel: TrackListViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidHandbookAudioTheme {
                AndroidHandbookAudioNavHost(
                    playlistsViewModel = playlistsViewModel,
                    trackListViewModel = trackListViewModel,
                    playerViewModel = playerViewModel,
                )
            }
        }
    }
}
