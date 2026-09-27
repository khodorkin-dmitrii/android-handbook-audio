package com.yavin.androidhandbookaudio.ui.playlists

import com.yavin.androidhandbookaudio.domain.model.Playlist
import com.yavin.androidhandbookaudio.domain.model.PlaylistManifest
import com.yavin.androidhandbookaudio.domain.repository.CatalogRepository
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads content from repository`() = runTest(dispatcher) {
        val viewModel = PlaylistsViewModel(FakeCatalogRepository(Result.success(listOf(playlist))))

        assertSame(PlaylistsUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()

        val content = viewModel.uiState.value as PlaylistsUiState.Content
        assertEquals("shorts", content.playlists.single().id)
        assertEquals("Short Audio Notes", content.playlists.single().title)
        assertEquals(listOf("EN", "RU"), content.playlists.single().languages)
    }

    @Test
    fun `moves from loading to error when repository fails`() = runTest(dispatcher) {
        val viewModel = PlaylistsViewModel(
            FakeCatalogRepository(Result.failure(IOException("offline"))),
        )

        assertSame(PlaylistsUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()

        assertSame(PlaylistsUiState.Error, viewModel.uiState.value)
    }

    @Test
    fun `moves from loading to empty when repository returns no playlists`() = runTest(dispatcher) {
        val viewModel = PlaylistsViewModel(FakeCatalogRepository(Result.success(emptyList())))

        assertSame(PlaylistsUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()

        assertSame(PlaylistsUiState.Empty, viewModel.uiState.value)
    }

    private companion object {
        val playlist = Playlist(
            id = "shorts",
            titles = mapOf("en" to "Short Audio Notes"),
            availableLanguages = listOf("en", "ru"),
            manifestUrl = "https://example.com/manifest.json",
        )
    }
}

private class FakeCatalogRepository(
    private val result: Result<List<Playlist>>,
) : CatalogRepository {
    override suspend fun getPlaylists(): List<Playlist> = result.getOrThrow()

    override suspend fun getPlaylist(playlistId: String): PlaylistManifest =
        error("Not used by PlaylistsViewModel")
}
