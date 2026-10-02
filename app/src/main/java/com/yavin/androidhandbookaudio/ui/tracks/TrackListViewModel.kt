package com.yavin.androidhandbookaudio.ui.tracks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yavin.androidhandbookaudio.domain.model.PlaylistManifest
import com.yavin.androidhandbookaudio.domain.model.Track
import com.yavin.androidhandbookaudio.domain.repository.CatalogRepository
import com.yavin.androidhandbookaudio.domain.repository.DEFAULT_PLAYBACK_LANGUAGE
import com.yavin.androidhandbookaudio.domain.repository.PlaybackPreferencesRepository
import com.yavin.androidhandbookaudio.playback.PlaybackController
import com.yavin.androidhandbookaudio.playback.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class TrackListViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val playbackController: PlaybackController,
    private val playbackPreferencesRepository: PlaybackPreferencesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<TrackListUiState>(TrackListUiState.Loading)
    val uiState: StateFlow<TrackListUiState> = _uiState.asStateFlow()

    private var playlist: PlaylistManifest? = null
    private var playbackState = PlaybackState()
    private var preferredLanguage: String = DEFAULT_PLAYBACK_LANGUAGE
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            playbackController.state.collect { state ->
                playbackState = state
                playlist?.let(::showPlaylist)
            }
        }
        viewModelScope.launch {
            playbackPreferencesRepository.preferredLanguage.collect { language ->
                preferredLanguage = language
            }
        }
    }

    fun loadPlaylist(playlistId: String) {
        if (playlist?.id == playlistId) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = TrackListUiState.Loading
            try {
                val loadedPlaylist = catalogRepository.getPlaylist(playlistId)
                playlist = loadedPlaylist
                playbackController.updatePlaylistTracks(loadedPlaylist.tracks)
                showPlaylist(loadedPlaylist)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                playlist = null
                _uiState.value = TrackListUiState.Error
            }
        }
    }

    fun retry(playlistId: String) {
        playlist = null
        loadPlaylist(playlistId)
    }

    fun playOrPause(trackId: String) {
        val loadedPlaylist = playlist ?: return
        if (playbackState.currentTrackId == trackId) {
            when {
                playbackState.isPlaying -> playbackController.pause()
                playbackState.error != null -> playbackController.retry()
                else -> playbackController.play()
            }
            return
        }
        playbackController.playPlaylist(
            tracks = loadedPlaylist.tracks,
            selectedTrackId = trackId,
            playlistTitle = loadedPlaylist.displayTitle(),
            preferredLanguage = preferredLanguage,
        )
    }

    private fun showPlaylist(playlist: PlaylistManifest) {
        _uiState.value = if (playlist.tracks.isEmpty()) {
            TrackListUiState.Empty
        } else {
            TrackListUiState.Content(
                playlistTitle = playlist.displayTitle(),
                tracks = playlist.tracks.map { track -> track.toUiModel(playbackState) },
            )
        }
    }
}

private fun PlaylistManifest.displayTitle(): String = titles["en"] ?: titles.values.first()

private fun Track.toUiModel(playbackState: PlaybackState): TrackUiModel {
    val isCurrent = playbackState.currentTrackId == id
    return TrackUiModel(
        id = id,
        order = order,
        title = titles["en"] ?: titles.values.first(),
        languages = availableLanguages,
        isCurrent = isCurrent,
        isPlaying = isCurrent && playbackState.isPlaying,
        playbackStatus = if (!isCurrent) {
            null
        } else {
            when {
                playbackState.error != null -> TrackPlaybackStatus.ERROR
                playbackState.isBuffering -> TrackPlaybackStatus.BUFFERING
                playbackState.isPlaying -> TrackPlaybackStatus.PLAYING
                else -> TrackPlaybackStatus.PAUSED
            }
        },
    )
}
