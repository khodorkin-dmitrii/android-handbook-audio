package com.yavin.androidhandbookaudio.domain.model

data class PlaylistManifest(
    val id: String,
    val titles: Map<String, String>,
    val availableLanguages: List<String>,
    val tracks: List<Track>,
)

data class Track(
    val id: String,
    val order: Int,
    val titles: Map<String, String>,
    val renditions: Map<String, MediaRendition>,
) {
    val availableLanguages: List<String>
        get() = renditions.keys.toList()
}

data class MediaRendition(
    val language: String,
    val audioUrl: String,
    val transcriptUrl: String?,
    val timedTranscriptUrl: String?,
    val timedTranscriptFormat: String? = null,
)
