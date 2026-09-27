package com.yavin.androidhandbookaudio.data.repository

import com.yavin.androidhandbookaudio.data.remote.CatalogApi
import com.yavin.androidhandbookaudio.data.remote.dto.CatalogDto
import com.yavin.androidhandbookaudio.data.remote.dto.PlaylistDto
import com.yavin.androidhandbookaudio.data.remote.dto.PlaylistManifestDto
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RemoteCatalogRepositoryTest {
    @Test
    fun `returns mapped playlists from remote catalog`() = runTest {
        val repository = RemoteCatalogRepository(
            catalogApi = FakeCatalogApi(
                result = Result.success(
                    CatalogDto(
                        playlists = listOf(
                            PlaylistDto(
                                id = "shorts",
                                title = mapOf("en" to "Short Audio Notes"),
                                availableLanguages = listOf("en", "ru"),
                                manifestUrl = "https://example.com/manifest.json",
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertEquals("shorts", repository.getPlaylists().single().id)
    }

    @Test
    fun `propagates remote error to caller`() {
        val repository = RemoteCatalogRepository(
            catalogApi = FakeCatalogApi(Result.failure(IOException("offline"))),
        )

        assertThrows(IOException::class.java) {
            runTest { repository.getPlaylists() }
        }
    }

    @Test
    fun `resolves playlist ID through catalog and loads manifest URL`() = runTest {
        val api = FakeCatalogApi(
            result = Result.success(
                CatalogDto(
                    playlists = listOf(
                        PlaylistDto(
                            id = "shorts",
                            title = mapOf("en" to "Shorts"),
                            availableLanguages = listOf("en"),
                            manifestUrl = "https://example.com/shorts.json",
                        ),
                    ),
                ),
            ),
            manifestResult = Result.success(
                PlaylistManifestDto(
                    id = "shorts",
                    title = mapOf("en" to "Shorts"),
                ),
            ),
        )
        val repository = RemoteCatalogRepository(api)

        assertEquals("shorts", repository.getPlaylist("shorts").id)
        assertEquals("https://example.com/shorts.json", api.requestedManifestUrl)
    }
}

private class FakeCatalogApi(
    private val result: Result<CatalogDto>,
    private val manifestResult: Result<PlaylistManifestDto> = Result.success(PlaylistManifestDto()),
) : CatalogApi {
    var requestedManifestUrl: String? = null

    override suspend fun getCatalog(): CatalogDto = result.getOrThrow()

    override suspend fun getPlaylistManifest(manifestUrl: String): PlaylistManifestDto {
        requestedManifestUrl = manifestUrl
        return manifestResult.getOrThrow()
    }
}
