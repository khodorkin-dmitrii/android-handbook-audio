package com.yavin.androidhandbookaudio.data.mapper

import com.yavin.androidhandbookaudio.data.remote.dto.CatalogDto
import com.yavin.androidhandbookaudio.data.remote.dto.PlaylistDto
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogMapperTest {
    @Test
    fun `maps real catalog shape to domain playlist`() {
        val catalog = CatalogDto(
            schemaVersion = 1,
            playlists = listOf(
                PlaylistDto(
                    id = "shorts",
                    title = mapOf("en" to "Short Audio Notes", "ru" to "Short Audio Notes"),
                    availableLanguages = listOf("en", "ru"),
                    manifestUrl = "https://example.com/shorts/manifest.json",
                ),
            ),
        )

        val playlist = catalog.toDomainPlaylists().single()

        assertEquals("shorts", playlist.id)
        assertEquals("Short Audio Notes", playlist.titles["en"])
        assertEquals(listOf("en", "ru"), playlist.availableLanguages)
        assertEquals("https://example.com/shorts/manifest.json", playlist.manifestUrl)
    }

    @Test
    fun `skips invalid playlist without losing valid entries`() {
        val catalog = CatalogDto(
            playlists = listOf(
                PlaylistDto(id = null),
                PlaylistDto(
                    id = "valid",
                    title = mapOf("en" to "Valid"),
                    availableLanguages = listOf("en"),
                    manifestUrl = "https://example.com/manifest.json",
                ),
            ),
        )

        assertEquals(listOf("valid"), catalog.toDomainPlaylists().map { it.id })
    }
}
