package com.yavin.androidhandbookaudio.data.mapper

import com.yavin.androidhandbookaudio.data.remote.dto.CatalogDto
import com.yavin.androidhandbookaudio.data.remote.dto.PlaylistDto
import com.yavin.androidhandbookaudio.domain.model.Playlist

fun CatalogDto.toDomainPlaylists(): List<Playlist> = playlists.mapNotNull(PlaylistDto::toDomain)

private fun PlaylistDto.toDomain(): Playlist? {
    val stableId = id?.trim()?.takeIf(String::isNotEmpty) ?: return null
    val url = manifestUrl?.trim()?.takeIf(String::isNotEmpty) ?: return null
    val localizedTitles = title
        .mapKeys { (language, _) -> language.trim().lowercase() }
        .mapValues { (_, value) -> value.trim() }
        .filter { (language, value) -> language.isNotEmpty() && value.isNotEmpty() }
    if (localizedTitles.isEmpty()) return null

    val languages = availableLanguages
        .map(String::trim)
        .filter(String::isNotEmpty)
        .map(String::lowercase)
        .distinct()

    return Playlist(
        id = stableId,
        titles = localizedTitles,
        availableLanguages = languages,
        manifestUrl = url,
    )
}
