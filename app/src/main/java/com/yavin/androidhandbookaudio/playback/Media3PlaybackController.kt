package com.yavin.androidhandbookaudio.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.yavin.androidhandbookaudio.domain.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Singleton
class Media3PlaybackController @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : PlaybackController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val controllerFuture = MediaController.Builder(
        context,
        SessionToken(context, ComponentName(context, PlaybackService::class.java)),
    ).buildAsync()
    private var controller: MediaController? = null
    private var pendingPlayback: PendingPlayback? = null
    private var languagesByTrackId: Map<String, String> = emptyMap()
    private var playbackError: String? = null

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (player.isPlaying || player.playbackState == Player.STATE_READY) {
                playbackError = null
            }
            updateState(player)
        }

        override fun onPlayerError(error: PlaybackException) {
            playbackError = error.message ?: "Playback failed"
            updateState(controller)
        }
    }

    init {
        controllerFuture.addListener(
            {
                runCatching { controllerFuture.get() }
                    .onSuccess { mediaController ->
                        controller = mediaController
                        mediaController.addListener(playerListener)
                        updateState(mediaController)
                        pendingPlayback?.let(::startPlayback)
                        pendingPlayback = null
                    }
                    .onFailure { error ->
                        _state.value = _state.value.copy(
                            error = error.message ?: "Could not connect to playback service",
                        )
                    }
            },
            ContextCompat.getMainExecutor(context),
        )
        scope.launch {
            while (isActive) {
                delay(POSITION_UPDATE_INTERVAL_MS)
                controller?.let(::updateState)
            }
        }
    }

    override fun playPlaylist(
        tracks: List<Track>,
        selectedTrackId: String,
        preferredLanguage: String?,
    ) {
        val pending = PendingPlayback(
            queue = buildPlaybackQueue(tracks, preferredLanguage),
            selectedTrackId = selectedTrackId,
        )
        if (controller == null) {
            pendingPlayback = pending
        } else {
            startPlayback(pending)
        }
    }

    override fun play() {
        controller?.play()
    }

    override fun pause() {
        controller?.pause()
    }

    override fun seekTo(positionMs: Long) {
        controller?.let { mediaController ->
            val targetPosition = clampSeekPosition(
                positionMs = positionMs,
                durationMs = mediaController.duration.takeIf { it != C.TIME_UNSET && it >= 0 },
            )
            mediaController.seekTo(targetPosition)
            _state.value = _state.value.copy(positionMs = targetPosition)
        }
    }

    override fun next() {
        controller?.seekToNextMediaItem()
    }

    override fun previous() {
        controller?.seekToPreviousMediaItem()
    }

    override fun setPlaybackSpeed(speed: Float) {
        controller?.let { mediaController ->
            mediaController.setPlaybackSpeed(speed)
            updateState(mediaController)
        }
    }

    private fun startPlayback(pending: PendingPlayback) {
        val selectedIndex = pending.queue.indexOfFirst { it.trackId == pending.selectedTrackId }
        if (selectedIndex == -1 || pending.queue.isEmpty()) {
            _state.value = _state.value.copy(error = "Selected track is unavailable")
            return
        }
        languagesByTrackId = pending.queue.associate { it.trackId to it.language }
        playbackError = null
        controller?.apply {
            setMediaItems(pending.queue.map(PlaybackQueueItem::toMediaItem), selectedIndex, 0)
            prepare()
            play()
        }
    }

    private fun updateState(player: Player?) {
        if (player == null) return
        val currentTrackId = player.currentMediaItem?.mediaId
        _state.value = PlaybackState(
            currentTrackId = currentTrackId,
            currentTitle = player.currentMediaItem?.mediaMetadata?.title?.toString(),
            currentLanguage = currentTrackId?.let(languagesByTrackId::get),
            isPlaying = player.isPlaying,
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
            positionMs = player.currentPosition.coerceAtLeast(0),
            durationMs = player.duration.takeIf { it != C.TIME_UNSET && it >= 0 },
            bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0),
            playbackSpeed = player.playbackParameters.speed,
            hasPrevious = player.hasPreviousMediaItem(),
            hasNext = player.hasNextMediaItem(),
            error = playbackError,
        )
    }

    private data class PendingPlayback(
        val queue: List<PlaybackQueueItem>,
        val selectedTrackId: String,
    )

    private companion object {
        const val POSITION_UPDATE_INTERVAL_MS = 500L
    }
}
