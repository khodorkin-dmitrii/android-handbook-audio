package com.yavin.androidhandbookaudio.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.yavin.androidhandbookaudio.domain.model.PlaybackBookmark
import com.yavin.androidhandbookaudio.domain.model.Track
import com.yavin.androidhandbookaudio.domain.repository.PlaybackPreferencesRepository
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Singleton
class Media3PlaybackController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val playbackPreferencesRepository: PlaybackPreferencesRepository,
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
    private var playlistIdsByTrackId: Map<String, String> = emptyMap()
    private var tracksById: Map<String, Track> = emptyMap()
    private var playbackError: String? = null
    private var restoredBookmark: PlaybackBookmark? = null
    private var isPersistenceLoaded = false
    private var hasUserRequestedPlayback = false
    private var persistenceJob: Job? = null
    private var lastPeriodicCheckpointPositionMs = 0L

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
            controller?.let(::checkpointPlayback)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!isPlaying) controller?.let(::checkpointPlayback)
        }

        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
            controller?.let(::checkpointPlayback)
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            controller?.takeUnless(Player::isPlaying)?.let(::checkpointPlayback)
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
                        pendingPlayback?.let(::startPlayback) ?: restoreIfReady()
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
            restoredBookmark = playbackPreferencesRepository.bookmark.first()
            isPersistenceLoaded = true
            restoreIfReady()
        }
        scope.launch {
            while (isActive) {
                delay(POSITION_UPDATE_INTERVAL_MS)
                controller?.let { mediaController ->
                    updateState(mediaController)
                    if (mediaController.isPlaying && shouldCheckpointPosition(
                            currentPositionMs = mediaController.currentPosition,
                            lastCheckpointPositionMs = lastPeriodicCheckpointPositionMs,
                        )
                    ) {
                        checkpointPlayback(mediaController)
                    }
                }
            }
        }
    }

    override fun playPlaylist(
        tracks: List<Track>,
        selectedTrackId: String,
        playlistId: String,
        playlistTitle: String?,
        preferredLanguage: String,
    ) {
        hasUserRequestedPlayback = true
        updatePlaylistTracks(playlistId, tracks)
        val pending = PendingPlayback(
            queue = buildPlaybackQueue(
                tracks = tracks,
                playlistId = playlistId,
                playlistTitle = playlistTitle,
                preferredLanguage = preferredLanguage,
            ),
            selectedTrackId = selectedTrackId,
        )
        if (controller == null) {
            pendingPlayback = pending
        } else {
            startPlayback(pending)
        }
    }

    override fun updatePlaylistTracks(playlistId: String, tracks: List<Track>) {
        tracksById = tracksById + tracks.associateBy(Track::id)
        playlistIdsByTrackId = playlistIdsByTrackId + tracks.associate { it.id to playlistId }
        updateState(controller)
        controller?.let { mediaController ->
            if (tracks.any { it.id == mediaController.currentMediaItem?.mediaId }) {
                checkpointPlayback(mediaController)
            }
        }
    }

    override fun getCurrentTrackRendition(
        preferredLanguage: String,
    ): PlaybackRenditionMetadata? {
        val currentTrackId = controller?.currentMediaItem?.mediaId
            ?: state.value.currentTrackId
            ?: return null
        val rendition = tracksById[currentTrackId]
            ?.selectRendition(preferredLanguage = preferredLanguage)
            ?: return null
        return PlaybackRenditionMetadata(
            language = rendition.language,
            timedTranscriptUrl = rendition.timedTranscriptUrl,
            timedTranscriptFormat = rendition.timedTranscriptFormat,
        )
    }

    override fun switchLanguage(preferredLanguage: String, startPositionMs: Long) {
        val mediaController = controller ?: return
        val currentTrackId = mediaController.currentMediaItem?.mediaId ?: return
        val track = tracksById[currentTrackId] ?: return
        val playlistTitle = mediaController.currentMediaItem?.mediaMetadata?.albumTitle?.toString()
        if (track.selectRendition(preferredLanguage) == null) return
        val playWhenReady = mediaController.playWhenReady
        val playbackSpeed = mediaController.playbackParameters.speed
        val queueTracks = (0 until mediaController.mediaItemCount)
            .mapNotNull { index ->
                tracksById[mediaController.getMediaItemAt(index).mediaId]
            }
            .ifEmpty { listOf(track) }
        val queue = buildPlaybackQueue(
            tracks = queueTracks,
            playlistId = playlistIdsByTrackId[currentTrackId],
            playlistTitle = playlistTitle,
            preferredLanguage = preferredLanguage,
        )
        val currentIndex = queue.indexOfFirst { it.trackId == currentTrackId }
        if (currentIndex == -1) return

        checkpointPlayback(mediaController)
        playbackError = null
        languagesByTrackId = queue.associate { it.trackId to it.language }
        val targetPositionMs = startPositionMs.coerceAtLeast(0)
        mediaController.setMediaItems(queue.map(PlaybackQueueItem::toMediaItem), currentIndex, targetPositionMs)
        mediaController.setPlaybackSpeed(playbackSpeed)
        mediaController.prepare()
        mediaController.playWhenReady = playWhenReady
        updateState(mediaController)
        checkpointPlayback(mediaController)
    }

    override fun play() {
        hasUserRequestedPlayback = true
        controller?.play()
    }

    override fun pause() {
        controller?.let { mediaController ->
            mediaController.pause()
            checkpointPlayback(mediaController)
        }
    }

    override fun retry() {
        controller?.let { mediaController ->
            hasUserRequestedPlayback = true
            playbackError = null
            mediaController.prepare()
            mediaController.play()
            updateState(mediaController)
        }
    }

    override fun seekTo(positionMs: Long) {
        controller?.let { mediaController ->
            val targetPosition = clampSeekPosition(
                positionMs = positionMs,
                durationMs = mediaController.duration.takeIf { it != C.TIME_UNSET && it >= 0 },
            )
            mediaController.seekTo(targetPosition)
            _state.value = _state.value.copy(positionMs = targetPosition)
            if (!mediaController.isPlaying) checkpointPlayback(mediaController)
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
            checkpointPlayback(mediaController)
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
            checkpointPlayback(this)
            setMediaItems(pending.queue.map(PlaybackQueueItem::toMediaItem), selectedIndex, 0)
            setPlaybackSpeed(restoredBookmark?.playbackSpeed ?: DEFAULT_PLAYBACK_SPEED)
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
            currentPlaylistId = player.currentMediaItem?.mediaMetadata?.extras
                ?.getString(PLAYLIST_ID_KEY)
                ?: currentTrackId?.let(playlistIdsByTrackId::get),
            currentPlaylistTitle = player.currentMediaItem?.mediaMetadata?.albumTitle?.toString(),
            currentLanguage = player.currentMediaItem?.mediaMetadata?.subtitle
                ?.toString()
                ?.lowercase()
                ?: currentTrackId?.let(languagesByTrackId::get),
            availableLanguages = tracksById.availableLanguagesFor(currentTrackId),
            timedTranscriptUrl = player.currentMediaItem?.mediaMetadata?.extras
                ?.getString(TIMED_TRANSCRIPT_URL_KEY),
            timedTranscriptFormat = player.currentMediaItem?.mediaMetadata?.extras
                ?.getString(TIMED_TRANSCRIPT_FORMAT_KEY),
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

    private fun restoreIfReady() {
        val mediaController = controller ?: return
        val bookmark = restoredBookmark ?: return
        if (!shouldRestorePlayback(
                isPersistenceLoaded = isPersistenceLoaded,
                hasUserRequestedPlayback = hasUserRequestedPlayback,
                hasPendingPlayback = pendingPlayback != null,
                hasCurrentMedia = mediaController.currentMediaItem != null,
            )
        ) return

        languagesByTrackId = mapOf(bookmark.trackId to bookmark.language)
        bookmark.playlistId?.let { playlistId ->
            playlistIdsByTrackId = mapOf(bookmark.trackId to playlistId)
        }
        mediaController.setMediaItem(bookmark.toMediaItem(), bookmark.positionMs)
        mediaController.setPlaybackSpeed(bookmark.playbackSpeed)
        mediaController.prepare()
        updateState(mediaController)
    }

    private fun checkpointPlayback(player: Player) {
        val mediaItem = player.currentMediaItem ?: return
        val localConfiguration = mediaItem.localConfiguration ?: return
        val language = mediaItem.mediaMetadata.subtitle?.toString()?.lowercase()
            ?: languagesByTrackId[mediaItem.mediaId]
            ?: return
        val snapshot = PlaybackBookmark(
            trackId = mediaItem.mediaId,
            title = mediaItem.mediaMetadata.title?.toString() ?: return,
            playlistId = mediaItem.mediaMetadata.extras?.getString(PLAYLIST_ID_KEY)
                ?: playlistIdsByTrackId[mediaItem.mediaId],
            playlistTitle = mediaItem.mediaMetadata.albumTitle?.toString(),
            language = language,
            audioUrl = localConfiguration.uri.toString(),
            positionMs = player.currentPosition.coerceAtLeast(0),
            playbackSpeed = player.playbackParameters.speed,
            timedTranscriptUrl = mediaItem.mediaMetadata.extras
                ?.getString(TIMED_TRANSCRIPT_URL_KEY),
            timedTranscriptFormat = mediaItem.mediaMetadata.extras
                ?.getString(TIMED_TRANSCRIPT_FORMAT_KEY),
        )
        lastPeriodicCheckpointPositionMs = snapshot.positionMs
        val previousPersistenceJob = persistenceJob
        persistenceJob = scope.launch(Dispatchers.IO) {
            previousPersistenceJob?.join()
            playbackPreferencesRepository.saveBookmark(snapshot)
        }
    }

    private data class PendingPlayback(
        val queue: List<PlaybackQueueItem>,
        val selectedTrackId: String,
    )

    private companion object {
        const val POSITION_UPDATE_INTERVAL_MS = 500L
        const val PERIODIC_CHECKPOINT_INTERVAL_MS = 15_000L
        const val DEFAULT_PLAYBACK_SPEED = 1f
    }
}

internal fun Map<String, Track>.availableLanguagesFor(trackId: String?): List<String> =
    trackId?.let { get(it)?.availableLanguages }.orEmpty()

internal fun shouldCheckpointPosition(
    currentPositionMs: Long,
    lastCheckpointPositionMs: Long,
    intervalMs: Long = 15_000L,
): Boolean = kotlin.math.abs(currentPositionMs - lastCheckpointPositionMs) >= intervalMs

internal fun shouldRestorePlayback(
    isPersistenceLoaded: Boolean,
    hasUserRequestedPlayback: Boolean,
    hasPendingPlayback: Boolean,
    hasCurrentMedia: Boolean,
): Boolean = isPersistenceLoaded &&
    !hasUserRequestedPlayback &&
    !hasPendingPlayback &&
    !hasCurrentMedia
