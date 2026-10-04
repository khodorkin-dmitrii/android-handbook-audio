package com.yavin.androidhandbookaudio.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yavin.androidhandbookaudio.domain.model.PlaylistManifest
import com.yavin.androidhandbookaudio.domain.repository.CatalogRepository
import com.yavin.androidhandbookaudio.domain.model.TimedTranscript
import com.yavin.androidhandbookaudio.domain.repository.DEFAULT_PLAYBACK_LANGUAGE
import com.yavin.androidhandbookaudio.domain.repository.PlaybackPreferencesRepository
import com.yavin.androidhandbookaudio.domain.repository.TranscriptRepository
import com.yavin.androidhandbookaudio.playback.PlaybackController
import com.yavin.androidhandbookaudio.playback.SwitchPlaybackLanguage
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackController: PlaybackController,
    private val catalogRepository: CatalogRepository,
    private val transcriptRepository: TranscriptRepository,
    playbackPreferencesRepository: PlaybackPreferencesRepository,
    private val switchPlaybackLanguage: SwitchPlaybackLanguage,
) : ViewModel() {
    private val transcriptLoadState = MutableStateFlow<TranscriptLoadState>(
        TranscriptLoadState.Unavailable,
    )
    private var languageSwitchJob: Job? = null

    val uiState: StateFlow<PlayerUiState> = combine(
        playbackController.state,
        transcriptLoadState,
        playbackPreferencesRepository.preferredLanguage,
    ) { playbackState, transcriptState, preferredLanguage ->
        val currentSource = playbackState.toTimedTranscriptSourceOrNull()
        val transcriptUiState = when {
            currentSource == null -> TranscriptUiState.Unavailable
            transcriptState.source != currentSource -> TranscriptUiState.Loading
            transcriptState is TranscriptLoadState.Loading -> TranscriptUiState.Loading
            transcriptState is TranscriptLoadState.Content -> transcriptState.transcript.toUiState(
                playbackState.positionMs,
            )
            transcriptState is TranscriptLoadState.Error -> TranscriptUiState.Error()
            else -> TranscriptUiState.Unavailable
        }
        playbackState.toPlayerUiState(transcriptUiState, preferredLanguage)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = playbackController.state.value.toPlayerUiState(
                preferredLanguage = DEFAULT_PLAYBACK_LANGUAGE,
            ),
        )

    init {
        viewModelScope.launch {
            playbackController.state
                .map { playbackState ->
                    playbackState.currentTrackId
                        ?.takeIf { playbackState.availableLanguages.isEmpty() }
                        ?.let { trackId ->
                            PlaybackMetadataRequest(
                                trackId = trackId,
                                playlistId = playbackState.currentPlaylistId,
                            )
                        }
                }
                .distinctUntilChanged()
                .collectLatest { request ->
                    request ?: return@collectLatest
                    findPlaylistFor(request)?.let { playlist ->
                        playbackController.updatePlaylistTracks(playlist.id, playlist.tracks)
                    }
                }
        }
        viewModelScope.launch {
            playbackController.state
                .map { playbackState -> playbackState.toTimedTranscriptSourceOrNull() }
                .distinctUntilChanged()
                .collectLatest { source ->
                    if (source == null) {
                        transcriptLoadState.value = TranscriptLoadState.Unavailable
                        return@collectLatest
                    }
                    transcriptLoadState.value = TranscriptLoadState.Loading(source)
                    transcriptLoadState.value = try {
                        val transcript = transcriptRepository.getTimedTranscript(
                            url = source.url,
                            format = source.format,
                        )
                        TranscriptLoadState.Content(source, transcript)
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Exception) {
                        TranscriptLoadState.Error(source)
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

    fun selectLanguage(language: String) {
        val currentSource = playbackController.state.value.toTimedTranscriptSourceOrNull()
        val loadedTranscript = (transcriptLoadState.value as? TranscriptLoadState.Content)
            ?.takeIf { it.source == currentSource }
            ?.transcript
        languageSwitchJob?.cancel()
        languageSwitchJob = viewModelScope.launch {
            switchPlaybackLanguage(language, loadedTranscript)
        }
    }

    companion object {
        const val SEEK_INTERVAL_MS = 10_000L
    }

    private suspend fun findPlaylistFor(request: PlaybackMetadataRequest): PlaylistManifest? {
        request.playlistId?.let { playlistId ->
            return loadPlaylistOrNull(playlistId)
                ?.takeIf { playlist -> playlist.tracks.any { it.id == request.trackId } }
        }

        val playlists = try {
            catalogRepository.getPlaylists()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            return null
        }
        playlists.forEach { playlist ->
            val manifest = loadPlaylistOrNull(playlist.id) ?: return@forEach
            if (manifest.tracks.any { it.id == request.trackId }) return manifest
        }
        return null
    }

    private suspend fun loadPlaylistOrNull(playlistId: String): PlaylistManifest? = try {
        catalogRepository.getPlaylist(playlistId)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        null
    }

    private sealed interface TranscriptLoadState {
        val source: TimedTranscriptSource?

        data object Unavailable : TranscriptLoadState {
            override val source: TimedTranscriptSource? = null
        }
        data class Loading(override val source: TimedTranscriptSource) : TranscriptLoadState
        data class Content(
            override val source: TimedTranscriptSource,
            val transcript: TimedTranscript,
        ) : TranscriptLoadState
        data class Error(override val source: TimedTranscriptSource) : TranscriptLoadState
    }

    private data class PlaybackMetadataRequest(
        val trackId: String,
        val playlistId: String?,
    )
}
