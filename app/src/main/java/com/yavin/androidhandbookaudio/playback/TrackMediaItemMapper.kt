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

fun Track.selectRendition(
    explicitlySelectedLanguage: String? = null,
    preferredLanguage: String? = null,
): MediaRendition? {
    val normalizedExplicit = explicitlySelectedLanguage?.trim()?.lowercase()
    val normalizedPreferred = preferredLanguage?.trim()?.lowercase()
    return normalizedExplicit?.let(renditions::get)
        ?: normalizedPreferred?.let(renditions::get)
        ?: renditions["en"]
        ?: renditions.values.firstOrNull()
}

fun buildPlaybackQueue(
    tracks: List<Track>,
    preferredLanguage: String? = null,
    selectedTrackId: String? = null,
    selectedLanguage: String? = null,
): List<PlaybackQueueItem> = tracks.sortedBy(Track::order).mapNotNull { track ->
    val rendition = track.selectRendition(
        explicitlySelectedLanguage = selectedLanguage.takeIf { track.id == selectedTrackId },
        preferredLanguage = preferredLanguage,
    ) ?: return@mapNotNull null
    track.toPlaybackQueueItem(rendition)
}

fun Track.toPlaybackQueueItem(rendition: MediaRendition): PlaybackQueueItem =
    PlaybackQueueItem(
        trackId = id,
        title = titles[rendition.language]
            ?: titles["en"]
            ?: titles.values.first(),
        language = rendition.language,
        audioUrl = rendition.audioUrl,
        timedTranscriptUrl = rendition.timedTranscriptUrl,
        timedTranscriptFormat = rendition.timedTranscriptFormat,
    )

data class RenditionSwitchPlan(
    val item: PlaybackQueueItem,
    val playWhenReady: Boolean,
    val playbackSpeed: Float,
)

fun createRenditionSwitchPlan(
    track: Track,
    language: String,
    playWhenReady: Boolean,
    playbackSpeed: Float,
): RenditionSwitchPlan? {
    val rendition = track.selectRendition(explicitlySelectedLanguage = language)
        ?.takeIf { it.language.equals(language.trim(), ignoreCase = true) }
        ?: return null
    return RenditionSwitchPlan(
        item = track.toPlaybackQueueItem(rendition),
        playWhenReady = playWhenReady,
        playbackSpeed = playbackSpeed,
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
