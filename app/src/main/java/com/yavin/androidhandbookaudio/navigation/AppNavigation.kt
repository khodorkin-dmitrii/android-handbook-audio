package com.yavin.androidhandbookaudio.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.yavin.androidhandbookaudio.ui.playlists.PlaylistsScreen
import com.yavin.androidhandbookaudio.ui.playlists.PlaylistsViewModel
import com.yavin.androidhandbookaudio.ui.player.MiniPlayer
import com.yavin.androidhandbookaudio.ui.player.PlayerScreen
import com.yavin.androidhandbookaudio.ui.player.PlayerViewModel
import com.yavin.androidhandbookaudio.ui.player.toMiniPlayerUiState
import com.yavin.androidhandbookaudio.ui.tracks.TrackListScreen
import com.yavin.androidhandbookaudio.ui.tracks.TrackListViewModel
import kotlinx.serialization.Serializable

@Serializable
data object PlaylistsKey : NavKey

@Serializable
data class TrackListKey(val playlistId: String) : NavKey

@Serializable
data object PlayerKey : NavKey

@Composable
fun AndroidHandbookAudioNavHost(
    playlistsViewModel: PlaylistsViewModel,
    trackListViewModel: TrackListViewModel,
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(PlaylistsKey)
    val state by playlistsViewModel.uiState.collectAsStateWithLifecycle()
    val trackListState by trackListViewModel.uiState.collectAsStateWithLifecycle()
    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val miniPlayerState = playerState.toMiniPlayerUiState()

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (miniPlayerState != null && backStack.lastOrNull() !is PlayerKey) {
                MiniPlayer(
                    state = miniPlayerState,
                    onOpenPlayer = { backStack.pushIfNotTop(PlayerKey) },
                    onPlayOrPause = playerViewModel::playOrPause,
                    modifier = Modifier.navigationBarsPadding(),
                )
            }
        },
    ) { outerPadding ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier
                .padding(outerPadding)
                .consumeWindowInsets(outerPadding),
            entryProvider = entryProvider {
                entry<PlaylistsKey> {
                    PlaylistsScreen(
                        state = state,
                        onRetry = playlistsViewModel::retry,
                        onPlaylistClick = { playlistId ->
                            backStack.pushIfNotTop(TrackListKey(playlistId))
                        },
                    )
                }
                entry<TrackListKey> { key ->
                    LaunchedEffect(key.playlistId) {
                        trackListViewModel.loadPlaylist(key.playlistId)
                    }
                    TrackListScreen(
                        state = trackListState,
                        onBack = dropUnlessResumed {
                            backStack.popIfTop(key)
                        },
                        onRetry = { trackListViewModel.retry(key.playlistId) },
                        onPlayOrPause = trackListViewModel::playOrPause,
                    )
                }
                entry<PlayerKey> {
                    PlayerScreen(
                        state = playerState,
                        onBack = dropUnlessResumed {
                            backStack.popIfTop(PlayerKey)
                        },
                        onPlayOrPause = playerViewModel::playOrPause,
                        onSeekTo = playerViewModel::seekTo,
                        onSeekBackward = playerViewModel::seekBackward,
                        onSeekForward = playerViewModel::seekForward,
                        onPrevious = playerViewModel::previous,
                        onNext = playerViewModel::next,
                        onSpeedSelected = playerViewModel::setPlaybackSpeed,
                        onLanguageSelected = playerViewModel::selectLanguage,
                        onTranscriptSegmentClick = playerViewModel::seekToTranscriptSegment,
                    )
                }
            },
        )
    }
}

internal fun <T> MutableList<T>.pushIfNotTop(key: T) {
    if (lastOrNull() != key) add(key)
}

internal fun <T> MutableList<T>.popIfTop(key: T) {
    if (size > 1 && lastOrNull() == key) removeAt(lastIndex)
}
