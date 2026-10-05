package com.yavin.androidhandbookaudio.ui.tracks

import com.yavin.androidhandbookaudio.domain.model.MediaRendition
import com.yavin.androidhandbookaudio.domain.model.Playlist
import com.yavin.androidhandbookaudio.domain.model.PlaylistManifest
import com.yavin.androidhandbookaudio.domain.model.Track
import com.yavin.androidhandbookaudio.domain.repository.CatalogRepository
import com.yavin.androidhandbookaudio.domain.model.PlaybackBookmark
import com.yavin.androidhandbookaudio.domain.repository.PlaybackPreferencesRepository
import com.yavin.androidhandbookaudio.playback.PlaybackController
import com.yavin.androidhandbookaudio.playback.PlaybackRenditionMetadata
import com.yavin.androidhandbookaudio.playback.PlaybackState
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
class TrackListViewModelTest {
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
    fun `moves from loading to ordered content`() = runTest(dispatcher) {
        val viewModel = createViewModel(
            catalogRepository = FakeTrackCatalogRepository(Result.success(playlist)),
            playbackController = FakePlaybackController(),
            playbackPreferencesRepository = FakePlaybackPreferencesRepository(),
        )

        viewModel.loadPlaylist("shorts")
        assertSame(TrackListUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()

        val content = viewModel.uiState.value as TrackListUiState.Content
        assertEquals("Short Audio Notes", content.playlistTitle)
        assertEquals(listOf("shorts.first", "shorts.second"), content.tracks.map { it.id })
        assertEquals(listOf("en", "ru"), content.tracks.first().languages)
    }

    @Test
    fun `moves from loading to error`() = runTest(dispatcher) {
        val viewModel = createViewModel(
            catalogRepository = FakeTrackCatalogRepository(Result.failure(IOException("offline"))),
            playbackController = FakePlaybackController(),
            playbackPreferencesRepository = FakePlaybackPreferencesRepository(),
        )

        viewModel.loadPlaylist("shorts")
        advanceUntilIdle()

        assertSame(TrackListUiState.Error, viewModel.uiState.value)
    }

    @Test
    fun `moves from loading to empty`() = runTest(dispatcher) {
        val viewModel = createViewModel(
            catalogRepository = FakeTrackCatalogRepository(
                Result.success(playlist.copy(tracks = emptyList())),
            ),
            playbackController = FakePlaybackController(),
            playbackPreferencesRepository = FakePlaybackPreferencesRepository(),
        )

        viewModel.loadPlaylist("shorts")
        advanceUntilIdle()

        assertSame(TrackListUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `starts selected track and reflects active playback`() = runTest(dispatcher) {
        val playbackController = FakePlaybackController()
        val viewModel = createViewModel(
            catalogRepository = FakeTrackCatalogRepository(Result.success(playlist)),
            playbackController = playbackController,
            playbackPreferencesRepository = FakePlaybackPreferencesRepository(),
        )
        viewModel.loadPlaylist("shorts")
        advanceUntilIdle()

        viewModel.playOrPause("shorts.second")
        assertEquals("shorts.second", playbackController.selectedTrackId)
        assertEquals("shorts", playbackController.playlistId)
        assertEquals("Short Audio Notes", playbackController.playlistTitle)
        playbackController.mutableState.value = PlaybackState(
            currentTrackId = "shorts.second",
            currentLanguage = "en",
            isPlaying = true,
            positionMs = 25_000,
            durationMs = 60_000,
        )
        advanceUntilIdle()

        val tracks = (viewModel.uiState.value as TrackListUiState.Content).tracks
        assertEquals(true, tracks.single { it.id == "shorts.second" }.isPlaying)
        assertEquals(
            TrackPlaybackStatus.PLAYING,
            tracks.single { it.id == "shorts.second" }.playbackStatus,
        )
        assertEquals(25_000L, tracks.single { it.id == "shorts.second" }.positionMs)
        assertEquals(60_000L, tracks.single { it.id == "shorts.second" }.durationMs)
        assertEquals(null, tracks.single { it.id == "shorts.first" }.durationMs)
    }

    @Test
    fun `uses global preferred language when opening a track`() =
        runTest(dispatcher) {
            val playbackController = FakePlaybackController()
            val preferences = FakePlaybackPreferencesRepository("ru")
            val viewModel = createViewModel(
                catalogRepository = FakeTrackCatalogRepository(Result.success(playlist)),
                playbackController = playbackController,
                playbackPreferencesRepository = preferences,
            )
            viewModel.loadPlaylist("shorts")
            advanceUntilIdle()

            viewModel.playOrPause("shorts.first")
            assertEquals("ru", playbackController.preferredLanguage)
        }

    @Test
    fun `uses English when no preference has been explicitly stored`() =
        runTest(dispatcher) {
            val playbackController = FakePlaybackController()
            val viewModel = createViewModel(
                catalogRepository = FakeTrackCatalogRepository(Result.success(playlist)),
                playbackController = playbackController,
                playbackPreferencesRepository = FakePlaybackPreferencesRepository(),
            )
            viewModel.loadPlaylist("shorts")
            advanceUntilIdle()

            viewModel.playOrPause("shorts.first")

            assertEquals("en", playbackController.preferredLanguage)
        }

    private fun createViewModel(
        catalogRepository: CatalogRepository,
        playbackController: PlaybackController,
        playbackPreferencesRepository: PlaybackPreferencesRepository,
    ) = TrackListViewModel(
        catalogRepository = catalogRepository,
        playbackController = playbackController,
        playbackPreferencesRepository = playbackPreferencesRepository,
    )

    private companion object {
        fun track(id: String, order: Int) = Track(
            id = id,
            order = order,
            titles = mapOf("en" to "Track $order"),
            renditions = mapOf(
                "en" to MediaRendition("en", "https://example.com/en.mp3", null, null),
                "ru" to MediaRendition("ru", "https://example.com/ru.mp3", null, null),
            ),
        )

        val playlist = PlaylistManifest(
            id = "shorts",
            titles = mapOf("en" to "Short Audio Notes"),
            availableLanguages = listOf("en", "ru"),
            tracks = listOf(track("shorts.first", 1), track("shorts.second", 2)),
        )
    }
}

private class FakeTrackCatalogRepository(
    private val playlistResult: Result<PlaylistManifest>,
) : CatalogRepository {
    override suspend fun getPlaylists(): List<Playlist> = emptyList()

    override suspend fun getPlaylist(playlistId: String): PlaylistManifest = playlistResult.getOrThrow()
}

private class FakePlaybackController : PlaybackController {
    val mutableState = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = mutableState
    var selectedTrackId: String? = null
    var playlistId: String? = null
    var playlistTitle: String? = null
    var preferredLanguage: String? = null

    override fun playPlaylist(
        tracks: List<Track>,
        selectedTrackId: String,
        playlistId: String,
        playlistTitle: String?,
        preferredLanguage: String,
    ) {
        this.selectedTrackId = selectedTrackId
        this.playlistId = playlistId
        this.playlistTitle = playlistTitle
        this.preferredLanguage = preferredLanguage
    }

    override fun updatePlaylistTracks(playlistId: String, tracks: List<Track>) = Unit
    override fun getCurrentTrackRendition(preferredLanguage: String) = PlaybackRenditionMetadata(
        language = preferredLanguage,
        timedTranscriptUrl = null,
        timedTranscriptFormat = null,
    )
    override fun switchLanguage(preferredLanguage: String, startPositionMs: Long) {
    }

    override fun play() = Unit
    override fun pause() = Unit
    override fun retry() = Unit
    override fun seekTo(positionMs: Long) = Unit
    override fun next() = Unit
    override fun previous() = Unit
    override fun setPlaybackSpeed(speed: Float) = Unit
}

private class FakePlaybackPreferencesRepository(
    initialPreferredLanguage: String = "en",
) : PlaybackPreferencesRepository {
    override val bookmark = MutableStateFlow<PlaybackBookmark?>(null)
    override val preferredLanguage = MutableStateFlow(initialPreferredLanguage)

    override suspend fun saveBookmark(bookmark: PlaybackBookmark) = Unit

    override suspend fun savePreferredLanguage(language: String) {
        preferredLanguage.value = language.lowercase()
    }
}
