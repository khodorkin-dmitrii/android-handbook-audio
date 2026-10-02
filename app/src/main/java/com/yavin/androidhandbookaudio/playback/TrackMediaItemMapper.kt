package com.yavin.androidhandbookaudio.playback

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.yavin.androidhandbookaudio.domain.model.PlaybackBookmark
import com.yavin.androidhandbookaudio.domain.model.MediaRendition
import com.yavin.androidhandbookaudio.domain.model.Track
import com.yavin.androidhandbookaudio.domain.repository.DEFAULT_PLAYBACK_LANGUAGE

data class PlaybackQueueItem(
    val trackId: String,
    val title: String,
    val playlistTitle: String? = null,
    val language: String,
    val audioUrl: String,
    val timedTranscriptUrl: String? = null,
    val timedTranscriptFormat: String? = null,
)

fun Track.selectRendition(
    preferredLanguage: String = DEFAULT_PLAYBACK_LANGUAGE,
): MediaRendition? {
    val normalizedPreferred = preferredLanguage.trim().lowercase()
    return renditions[normalizedPreferred]
        ?: renditions["en"]
        ?: renditions.values.firstOrNull()
}

fun buildPlaybackQueue(
    tracks: List<Track>,
    playlistTitle: String? = null,
    preferredLanguage: String = DEFAULT_PLAYBACK_LANGUAGE,
): List<PlaybackQueueItem> = tracks.sortedBy(Track::order).mapNotNull { track ->
    val rendition = track.selectRendition(preferredLanguage) ?: return@mapNotNull null
    track.toPlaybackQueueItem(rendition, playlistTitle)
}

fun Track.toPlaybackQueueItem(
    rendition: MediaRendition,
    playlistTitle: String? = null,
): PlaybackQueueItem =
    PlaybackQueueItem(
        trackId = id,
        title = titles[rendition.language]
            ?: titles["en"]
            ?: titles.values.first(),
        playlistTitle = playlistTitle,
        language = rendition.language,
        audioUrl = rendition.audioUrl,
        timedTranscriptUrl = rendition.timedTranscriptUrl,
        timedTranscriptFormat = rendition.timedTranscriptFormat,
    )

fun PlaybackQueueItem.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(trackId)
    .setUri(audioUrl)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setAlbumTitle(playlistTitle)
            .setSubtitle(language.uppercase())
            .setExtras(
                Bundle().apply {
                    timedTranscriptUrl?.let { putString(TIMED_TRANSCRIPT_URL_KEY, it) }
                    timedTranscriptFormat?.let { putString(TIMED_TRANSCRIPT_FORMAT_KEY, it) }
                },
            )
            .build(),
    )
    .build()

fun PlaybackBookmark.toMediaItem(): MediaItem = PlaybackQueueItem(
    trackId = trackId,
    title = title,
    playlistTitle = playlistTitle,
    language = language,
    audioUrl = audioUrl,
    timedTranscriptUrl = timedTranscriptUrl,
    timedTranscriptFormat = timedTranscriptFormat,
).toMediaItem()

internal const val TIMED_TRANSCRIPT_URL_KEY =
    "com.yavin.androidhandbookaudio.media.TIMED_TRANSCRIPT_URL"
internal const val TIMED_TRANSCRIPT_FORMAT_KEY =
    "com.yavin.androidhandbookaudio.media.TIMED_TRANSCRIPT_FORMAT"
