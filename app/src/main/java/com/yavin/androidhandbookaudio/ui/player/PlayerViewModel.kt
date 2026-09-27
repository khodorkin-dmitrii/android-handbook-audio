package com.yavin.androidhandbookaudio.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yavin.androidhandbookaudio.playback.PlaybackController
import com.yavin.androidhandbookaudio.playback.clampSeekPosition
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackController: PlaybackController,
) : ViewModel() {
    val uiState: StateFlow<PlayerUiState> = playbackController.state
        .map { playbackState -> playbackState.toPlayerUiState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = playbackController.state.value.toPlayerUiState(),
        )

    fun playOrPause() {
        val active = uiState.value as? PlayerUiState.Active ?: return
        if (active.status == PlayerStatus.PLAYING) {
            playbackController.pause()
        } else {
            playbackController.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val active = uiState.value as? PlayerUiState.Active ?: return
        playbackController.seekTo(clampSeekPosition(positionMs, active.durationMs))
    }

    fun seekBackward() {
        val active = uiState.value as? PlayerUiState.Active ?: return
        seekTo(active.positionMs - SEEK_INTERVAL_MS)
    }

    fun seekForward() {
        val active = uiState.value as? PlayerUiState.Active ?: return
        seekTo(active.positionMs + SEEK_INTERVAL_MS)
    }

    fun previous() {
        if ((uiState.value as? PlayerUiState.Active)?.hasPrevious == true) {
            playbackController.previous()
        }
    }

    fun next() {
        if ((uiState.value as? PlayerUiState.Active)?.hasNext == true) {
            playbackController.next()
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        playbackController.setPlaybackSpeed(selectPlaybackSpeed(speed))
    }

    companion object {
        const val SEEK_INTERVAL_MS = 10_000L
    }
}
