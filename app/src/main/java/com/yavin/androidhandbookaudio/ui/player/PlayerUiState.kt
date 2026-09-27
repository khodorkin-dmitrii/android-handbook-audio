package com.yavin.androidhandbookaudio.ui.player

import com.yavin.androidhandbookaudio.playback.PlaybackState

sealed interface PlayerUiState {
    data class NoActiveMedia(val errorMessage: String? = null) : PlayerUiState

    data class Active(
        val trackId: String,
        val title: String,
        val language: String?,
        val status: PlayerStatus,
        val positionMs: Long,
        val durationMs: Long?,
        val bufferedPositionMs: Long,
        val playbackSpeed: Float,
        val hasPrevious: Boolean,
        val hasNext: Boolean,
        val errorMessage: String?,
    ) : PlayerUiState
}

enum class PlayerStatus {
    BUFFERING,
    PLAYING,
    PAUSED,
    ERROR,
}

data class MiniPlayerUiState(
    val trackId: String,
    val title: String,
    val language: String?,
    val isPlaying: Boolean,
    val isBuffering: Boolean,
    val progress: Float?,
)

val PlaybackSpeedOptions = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)

fun PlaybackState.toPlayerUiState(): PlayerUiState {
    val trackId = currentTrackId ?: return PlayerUiState.NoActiveMedia(error)
    val status = when {
        error != null -> PlayerStatus.ERROR
        isBuffering -> PlayerStatus.BUFFERING
        isPlaying -> PlayerStatus.PLAYING
        else -> PlayerStatus.PAUSED
    }
    return PlayerUiState.Active(
        trackId = trackId,
        title = currentTitle ?: "Unknown track",
        language = currentLanguage?.uppercase(),
        status = status,
        positionMs = positionMs,
        durationMs = durationMs,
        bufferedPositionMs = bufferedPositionMs,
        playbackSpeed = playbackSpeed,
        hasPrevious = hasPrevious,
        hasNext = hasNext,
        errorMessage = error,
    )
}

fun PlayerUiState.toMiniPlayerUiState(): MiniPlayerUiState? {
    val active = this as? PlayerUiState.Active ?: return null
    val progress = active.durationMs
        ?.takeIf { it > 0 }
        ?.let { duration -> (active.positionMs.toFloat() / duration).coerceIn(0f, 1f) }
    return MiniPlayerUiState(
        trackId = active.trackId,
        title = active.title,
        language = active.language,
        isPlaying = active.status == PlayerStatus.PLAYING,
        isBuffering = active.status == PlayerStatus.BUFFERING,
        progress = progress,
    )
}

fun selectPlaybackSpeed(requestedSpeed: Float): Float = PlaybackSpeedOptions.minBy { option ->
    kotlin.math.abs(option - requestedSpeed)
}

fun formatPlaybackTime(positionMs: Long): String {
    val totalSeconds = positionMs.coerceAtLeast(0) / 1_000
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
