package com.yavin.androidhandbookaudio.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistManifestDto(
    val schemaVersion: Int? = null,
    val id: String? = null,
    val title: Map<String, String> = emptyMap(),
    val availableLanguages: List<String> = emptyList(),
    val tracks: List<TrackDto> = emptyList(),
)

@Serializable
data class TrackDto(
    val id: String? = null,
    val order: Int? = null,
    val title: Map<String, String> = emptyMap(),
    val media: Map<String, MediaRenditionDto> = emptyMap(),
)

@Serializable
data class MediaRenditionDto(
    val audioUrl: String? = null,
    val transcriptUrl: String? = null,
    val timedTranscriptUrl: String? = null,
    val timedTranscriptFormat: String? = null,
)
