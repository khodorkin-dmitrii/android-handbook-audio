package com.yavin.androidhandbookaudio.ui.player

import com.yavin.androidhandbookaudio.domain.model.TimedTranscript
import com.yavin.androidhandbookaudio.domain.model.TranscriptSegment
import com.yavin.androidhandbookaudio.domain.repository.DEFAULT_PLAYBACK_LANGUAGE
import com.yavin.androidhandbookaudio.playback.PlaybackState

sealed interface PlayerUiState {
    data class NoActiveMedia(val errorMessage: String? = null) : PlayerUiState

    data class Active(
        val trackId: String,
        val title: String,
        val playlistTitle: String?,
        val language: String?,
        val preferredLanguage: String,
        val availableLanguages: List<String>,
        val status: PlayerStatus,
        val positionMs: Long,
        val durationMs: Long?,
        val bufferedPositionMs: Long,
        val playbackSpeed: Float,
        val hasPrevious: Boolean,
        val hasNext: Boolean,
        val errorMessage: String?,
        val transcript: TranscriptUiState = TranscriptUiState.Unavailable,
    ) : PlayerUiState
}

sealed interface TranscriptUiState {
    data object Unavailable : TranscriptUiState
    data object Loading : TranscriptUiState
    data class Content(
        val segments: List<TranscriptSegment>,
        val activeSegmentIndex: Int?,
    ) : TranscriptUiState
    data class Error(val message: String = "Transcript unavailable") : TranscriptUiState
}

data class TimedTranscriptSource(
    val url: String,
    val format: String,
)

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
    val hasError: Boolean,
    val progress: Float?,
    val positionMs: Long? = null,
    val durationMs: Long? = null,
)

val PlaybackSpeedOptions = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)

fun PlaybackState.toPlayerUiState(
    transcript: TranscriptUiState = TranscriptUiState.Unavailable,
    preferredLanguage: String = DEFAULT_PLAYBACK_LANGUAGE,
): PlayerUiState {
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
        playlistTitle = currentPlaylistTitle,
        language = currentLanguage,
        preferredLanguage = preferredLanguage,
        availableLanguages = (availableLanguages + preferredLanguage + listOfNotNull(currentLanguage))
            .distinct(),
        status = status,
        positionMs = positionMs,
        durationMs = durationMs,
        bufferedPositionMs = bufferedPositionMs,
        playbackSpeed = playbackSpeed,
        hasPrevious = hasPrevious,
        hasNext = hasNext,
        errorMessage = error,
        transcript = transcript,
    )
}

fun PlaybackState.toTimedTranscriptSourceOrNull(): TimedTranscriptSource? {
    val url = timedTranscriptUrl?.trim()?.takeIf(String::isNotEmpty) ?: return null
    val format = timedTranscriptFormat?.trim()?.lowercase()?.takeIf { it == "srt" } ?: return null
    return TimedTranscriptSource(url = url, format = format)
}

fun TimedTranscript.toUiState(positionMs: Long): TranscriptUiState = if (segments.isEmpty()) {
    TranscriptUiState.Unavailable
} else {
    TranscriptUiState.Content(
        segments = segments,
        activeSegmentIndex = findActiveTranscriptSegmentIndex(segments, positionMs),
    )
}

fun findActiveTranscriptSegmentIndex(
    segments: List<TranscriptSegment>,
    positionMs: Long,
): Int? {
    var low = 0
    var high = segments.lastIndex
    var candidate = -1
    while (low <= high) {
        val middle = (low + high).ushr(1)
        if (segments[middle].startMs <= positionMs) {
            candidate = middle
            low = middle + 1
        } else {
            high = middle - 1
        }
    }
    return candidate.takeIf { index ->
        index >= 0 && positionMs < segments[index].endMs
    }
}

fun PlayerUiState.toMiniPlayerUiState(): MiniPlayerUiState? {
    val active = this as? PlayerUiState.Active ?: return null
    val progress = active.durationMs
        ?.takeIf { it > 0 }
        ?.let { duration -> (active.positionMs.toFloat() / duration).coerceIn(0f, 1f) }
    return MiniPlayerUiState(
        trackId = active.trackId,
        title = active.title,
        language = active.language?.uppercase(),
        isPlaying = active.status == PlayerStatus.PLAYING,
        isBuffering = active.status == PlayerStatus.BUFFERING,
        hasError = active.status == PlayerStatus.ERROR,
        progress = progress,
        positionMs = active.positionMs,
        durationMs = active.durationMs,
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
