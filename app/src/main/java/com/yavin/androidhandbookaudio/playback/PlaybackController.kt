package com.yavin.androidhandbookaudio.playback

import com.yavin.androidhandbookaudio.domain.model.Track
import kotlinx.coroutines.flow.StateFlow

data class PlaybackState(
    val currentTrackId: String? = null,
    val currentTitle: String? = null,
    val currentLanguage: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long? = null,
    val bufferedPositionMs: Long = 0,
    val playbackSpeed: Float = 1f,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
    val error: String? = null,
)

interface PlaybackController {
    val state: StateFlow<PlaybackState>

    fun playPlaylist(
        tracks: List<Track>,
        selectedTrackId: String,
        preferredLanguage: String? = null,
    )

    fun play()

    fun pause()

    fun retry()

    fun seekTo(positionMs: Long)

    fun next()

    fun previous()

    fun setPlaybackSpeed(speed: Float)
}

fun clampSeekPosition(positionMs: Long, durationMs: Long?): Long {
    val nonNegativePosition = positionMs.coerceAtLeast(0)
    return durationMs
        ?.takeIf { it >= 0 }
        ?.let(nonNegativePosition::coerceAtMost)
        ?: nonNegativePosition
}
