package com.yavin.androidhandbookaudio.data.mapper

import com.yavin.androidhandbookaudio.data.remote.dto.MediaRenditionDto
import com.yavin.androidhandbookaudio.data.remote.dto.PlaylistManifestDto
import com.yavin.androidhandbookaudio.data.remote.dto.TrackDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaylistManifestMapperTest {
    @Test
    fun `maps stable IDs renditions transcript metadata and manifest order`() {
        val manifest = PlaylistManifestDto(
            id = "shorts",
            title = mapOf("en" to "Short Audio Notes"),
            availableLanguages = listOf("en", "ru"),
            tracks = listOf(
                track(id = "shorts.second", order = 2),
                track(id = "shorts.first", order = 1),
            ),
        )

        val playlist = requireNotNull(manifest.toDomain())

        assertEquals("shorts", playlist.id)
        assertEquals(listOf("shorts.first", "shorts.second"), playlist.tracks.map { it.id })
        assertEquals(listOf(1, 2), playlist.tracks.map { it.order })
        val english = playlist.tracks.first().renditions.getValue("en")
        assertEquals("https://example.com/en.mp3", english.audioUrl)
        assertEquals("https://example.com/transcript.json", english.transcriptUrl)
        assertEquals(null, english.timedTranscriptUrl)
    }

    @Test
    fun `skips malformed tracks but keeps valid tracks`() {
        val manifest = PlaylistManifestDto(
            id = "shorts",
            title = mapOf("en" to "Shorts"),
            tracks = listOf(
                TrackDto(id = null, order = 1),
                track(id = "shorts.valid", order = 2),
            ),
        )

        assertEquals(listOf("shorts.valid"), manifest.toDomain()?.tracks?.map { it.id })
    }

    @Test
    fun `rejects malformed playlist and supports empty track list`() {
        assertNull(PlaylistManifestDto(id = null).toDomain())
        assertEquals(
            emptyList<Any>(),
            PlaylistManifestDto(
                id = "empty",
                title = mapOf("en" to "Empty"),
            ).toDomain()?.tracks,
        )
    }

    private fun track(id: String, order: Int): TrackDto = TrackDto(
        id = id,
        order = order,
        title = mapOf("en" to "Track $order"),
        media = mapOf(
            "en" to MediaRenditionDto(
                audioUrl = "https://example.com/en.mp3",
                transcriptUrl = "https://example.com/transcript.json",
            ),
            "ru" to MediaRenditionDto(audioUrl = "https://example.com/ru.mp3"),
        ),
    )
}
