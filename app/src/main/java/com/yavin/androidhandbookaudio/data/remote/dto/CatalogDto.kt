package com.yavin.androidhandbookaudio.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CatalogDto(
    val schemaVersion: Int? = null,
    val playlists: List<PlaylistDto> = emptyList(),
)

@Serializable
data class PlaylistDto(
    val id: String? = null,
    val title: Map<String, String> = emptyMap(),
    val availableLanguages: List<String> = emptyList(),
    val manifestUrl: String? = null,
)
