package com.yavin.androidhandbookaudio.playback

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.yavin.androidhandbookaudio.domain.model.PlaybackBookmark
import com.yavin.androidhandbookaudio.domain.model.MediaRendition
import com.yavin.androidhandbookaudio.domain.model.Track

data class PlaybackQueueItem(
    val trackId: String,
    val title: String,
    val language: String,
    val audioUrl: String,
    val timedTranscriptUrl: String? = null,
    val timedTranscriptFormat: String? = null,
)

fun Track.selectRendition(preferredLanguage: String? = null): MediaRendition? {
    val normalizedPreferred = preferredLanguage?.trim()?.lowercase()
    return normalizedPreferred?.let(renditions::get)
        ?: renditions["en"]
        ?: renditions.values.firstOrNull()
}

fun buildPlaybackQueue(
    tracks: List<Track>,
    preferredLanguage: String? = null,
): List<PlaybackQueueItem> = tracks.sortedBy(Track::order).mapNotNull { track ->
    val rendition = track.selectRendition(preferredLanguage) ?: return@mapNotNull null
    PlaybackQueueItem(
        trackId = track.id,
        title = track.titles[rendition.language]
            ?: track.titles["en"]
            ?: track.titles.values.first(),
        language = rendition.language,
        audioUrl = rendition.audioUrl,
        timedTranscriptUrl = rendition.timedTranscriptUrl,
        timedTranscriptFormat = rendition.timedTranscriptFormat,
    )
}

fun PlaybackQueueItem.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(trackId)
    .setUri(audioUrl)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
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
    language = language,
    audioUrl = audioUrl,
    timedTranscriptUrl = timedTranscriptUrl,
    timedTranscriptFormat = timedTranscriptFormat,
).toMediaItem()

internal const val TIMED_TRANSCRIPT_URL_KEY =
    "com.yavin.androidhandbookaudio.media.TIMED_TRANSCRIPT_URL"
internal const val TIMED_TRANSCRIPT_FORMAT_KEY =
    "com.yavin.androidhandbookaudio.media.TIMED_TRANSCRIPT_FORMAT"
