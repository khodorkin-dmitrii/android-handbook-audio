package com.yavin.androidhandbookaudio.ui.tracks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yavin.androidhandbookaudio.domain.model.PlaylistManifest
import com.yavin.androidhandbookaudio.domain.model.Track
import com.yavin.androidhandbookaudio.domain.repository.CatalogRepository
import com.yavin.androidhandbookaudio.domain.repository.PlaybackPreferencesRepository
import com.yavin.androidhandbookaudio.playback.PlaybackController
import com.yavin.androidhandbookaudio.playback.PlaybackState
import com.yavin.androidhandbookaudio.playback.SwitchPlaybackLanguage
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
    private val switchPlaybackLanguage: SwitchPlaybackLanguage,
) : ViewModel() {
    private val _uiState = MutableStateFlow<TrackListUiState>(TrackListUiState.Loading)
    val uiState: StateFlow<TrackListUiState> = _uiState.asStateFlow()

    private var playlist: PlaylistManifest? = null
    private var playbackState = PlaybackState()
    private var preferredLanguage: String? = null
    private var loadJob: Job? = null
    private var languageSwitchJob: Job? = null

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
                playlist?.let(::showPlaylist)
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
            preferredLanguage = preferredLanguage,
        )
    }

    fun playLanguage(trackId: String, language: String) {
        val loadedPlaylist = playlist ?: return
        val normalizedLanguage = language.trim().lowercase()
        val track = loadedPlaylist.tracks.firstOrNull { it.id == trackId } ?: return
        if (normalizedLanguage !in track.renditions) return

        if (playbackState.currentTrackId == trackId) {
            if (playbackState.currentLanguage == normalizedLanguage) {
                if (!playbackState.isPlaying) playbackController.play()
            } else {
                languageSwitchJob?.cancel()
                languageSwitchJob = viewModelScope.launch {
                    switchPlaybackLanguage(normalizedLanguage)
                }
            }
        } else {
            playbackController.playPlaylist(
                tracks = loadedPlaylist.tracks,
                selectedTrackId = trackId,
                preferredLanguage = preferredLanguage,
                selectedLanguage = normalizedLanguage,
            )
        }
    }

    fun setPreferredLanguage(language: String) {
        viewModelScope.launch {
            playbackPreferencesRepository.savePreferredLanguage(language)
        }
    }

    private fun showPlaylist(playlist: PlaylistManifest) {
        _uiState.value = if (playlist.tracks.isEmpty()) {
            TrackListUiState.Empty
        } else {
            TrackListUiState.Content(
                playlistTitle = playlist.titles["en"] ?: playlist.titles.values.first(),
                preferredLanguage = preferredLanguage,
                availableLanguages = playlist.tracks
                    .flatMap(Track::availableLanguages)
                    .distinct(),
                tracks = playlist.tracks.map { track -> track.toUiModel(playbackState) },
            )
        }
    }
}

private fun Track.toUiModel(playbackState: PlaybackState): TrackUiModel {
    val isCurrent = playbackState.currentTrackId == id
    return TrackUiModel(
        id = id,
        order = order,
        title = titles["en"] ?: titles.values.first(),
        languages = availableLanguages,
        activeLanguage = playbackState.currentLanguage.takeIf { isCurrent },
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
