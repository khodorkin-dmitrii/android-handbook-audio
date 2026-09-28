package com.yavin.androidhandbookaudio.data.mapper

import com.yavin.androidhandbookaudio.data.remote.dto.MediaRenditionDto
import com.yavin.androidhandbookaudio.data.remote.dto.PlaylistManifestDto
import com.yavin.androidhandbookaudio.data.remote.dto.TrackDto
import com.yavin.androidhandbookaudio.domain.model.MediaRendition
import com.yavin.androidhandbookaudio.domain.model.PlaylistManifest
import com.yavin.androidhandbookaudio.domain.model.Track

fun PlaylistManifestDto.toDomain(): PlaylistManifest? {
    val stableId = id.normalizedOrNull() ?: return null
    val localizedTitles = title.normalizedTitles()
    if (localizedTitles.isEmpty()) return null

    return PlaylistManifest(
        id = stableId,
        titles = localizedTitles,
        availableLanguages = availableLanguages.normalizedLanguages(),
        tracks = tracks.mapNotNull(TrackDto::toDomain).sortedBy(Track::order),
    )
}

private fun TrackDto.toDomain(): Track? {
    val stableId = id.normalizedOrNull() ?: return null
    val trackOrder = order?.takeIf { it >= 0 } ?: return null
    val localizedTitles = title.normalizedTitles()
    if (localizedTitles.isEmpty()) return null

    val renditions = media.mapNotNull { (language, dto) ->
        dto.toDomain(language)?.let { rendition -> rendition.language to rendition }
    }.toMap()
    if (renditions.isEmpty()) return null

    return Track(
        id = stableId,
        order = trackOrder,
        titles = localizedTitles,
        renditions = renditions,
    )
}

private fun MediaRenditionDto.toDomain(language: String): MediaRendition? {
    val normalizedLanguage = language.normalizedOrNull()?.lowercase() ?: return null
    val url = audioUrl.normalizedOrNull() ?: return null
    return MediaRendition(
        language = normalizedLanguage,
        audioUrl = url,
        transcriptUrl = transcriptUrl.normalizedOrNull(),
        timedTranscriptUrl = timedTranscriptUrl.normalizedOrNull(),
        timedTranscriptFormat = timedTranscriptFormat.normalizedOrNull()?.lowercase(),
    )
}

private fun Map<String, String>.normalizedTitles(): Map<String, String> = mapNotNull { (language, value) ->
    val key = language.normalizedOrNull()?.lowercase()
    val title = value.normalizedOrNull()
    if (key == null || title == null) null else key to title
}.toMap()

private fun List<String>.normalizedLanguages(): List<String> = mapNotNull { language ->
    language.normalizedOrNull()?.lowercase()
}.distinct()

private fun String?.normalizedOrNull(): String? = this?.trim()?.takeIf(String::isNotEmpty)
