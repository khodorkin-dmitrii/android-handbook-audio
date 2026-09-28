package com.yavin.androidhandbookaudio.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yavin.androidhandbookaudio.domain.model.TimedTranscript
import com.yavin.androidhandbookaudio.domain.repository.TranscriptRepository
import com.yavin.androidhandbookaudio.playback.PlaybackController
import com.yavin.androidhandbookaudio.playback.clampSeekPosition
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackController: PlaybackController,
    private val transcriptRepository: TranscriptRepository,
) : ViewModel() {
    private val transcriptLoadState = MutableStateFlow<TranscriptLoadState>(
        TranscriptLoadState.Unavailable,
    )

    val uiState: StateFlow<PlayerUiState> = combine(
        playbackController.state,
        transcriptLoadState,
    ) { playbackState, transcriptState ->
        val transcriptUiState = when (transcriptState) {
            TranscriptLoadState.Unavailable -> TranscriptUiState.Unavailable
            TranscriptLoadState.Loading -> TranscriptUiState.Loading
            is TranscriptLoadState.Content -> transcriptState.transcript.toUiState(
                playbackState.positionMs,
            )
            TranscriptLoadState.Error -> TranscriptUiState.Error()
        }
        playbackState.toPlayerUiState(transcriptUiState)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = playbackController.state.value.toPlayerUiState(),
        )

    init {
        viewModelScope.launch {
            playbackController.state
                .map { playbackState -> playbackState.toTimedTranscriptSourceOrNull() }
                .distinctUntilChanged()
                .collectLatest { source ->
                    if (source == null) {
                        transcriptLoadState.value = TranscriptLoadState.Unavailable
                        return@collectLatest
                    }
                    transcriptLoadState.value = TranscriptLoadState.Loading
                    transcriptLoadState.value = try {
                        val transcript = transcriptRepository.getTimedTranscript(
                            url = source.url,
                            format = source.format,
                        )
                        TranscriptLoadState.Content(transcript)
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Exception) {
                        TranscriptLoadState.Error
                    }
                }
        }
    }

    fun playOrPause() {
        val active = uiState.value as? PlayerUiState.Active ?: return
        if (active.status == PlayerStatus.PLAYING) {
            playbackController.pause()
        } else if (active.status == PlayerStatus.ERROR) {
            playbackController.retry()
        } else {
            playbackController.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val active = uiState.value as? PlayerUiState.Active ?: return
        playbackController.seekTo(clampSeekPosition(positionMs, active.durationMs))
    }

    fun seekToTranscriptSegment(startMs: Long) {
        seekTo(startMs)
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

    private sealed interface TranscriptLoadState {
        data object Unavailable : TranscriptLoadState
        data object Loading : TranscriptLoadState
        data class Content(val transcript: TimedTranscript) : TranscriptLoadState
        data object Error : TranscriptLoadState
    }
}
