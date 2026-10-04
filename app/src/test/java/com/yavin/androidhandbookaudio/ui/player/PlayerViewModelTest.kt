package com.yavin.androidhandbookaudio.ui.player

import com.yavin.androidhandbookaudio.data.transcript.SrtParser
import com.yavin.androidhandbookaudio.domain.model.MediaRendition
import com.yavin.androidhandbookaudio.domain.model.Playlist
import com.yavin.androidhandbookaudio.domain.model.PlaylistManifest
import com.yavin.androidhandbookaudio.domain.model.Track
import com.yavin.androidhandbookaudio.domain.model.PlaybackBookmark
import com.yavin.androidhandbookaudio.domain.model.TimedTranscript
import com.yavin.androidhandbookaudio.domain.model.TranscriptSegment
import com.yavin.androidhandbookaudio.domain.repository.PlaybackPreferencesRepository
import com.yavin.androidhandbookaudio.domain.repository.CatalogRepository
import com.yavin.androidhandbookaudio.domain.repository.TranscriptRepository
import com.yavin.androidhandbookaudio.playback.PlaybackController
import com.yavin.androidhandbookaudio.playback.PlaybackRenditionMetadata
import com.yavin.androidhandbookaudio.playback.PlaybackState
import com.yavin.androidhandbookaudio.playback.SwitchPlaybackLanguage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {
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
    fun `maps state and delegates bounded player commands`() = runTest(dispatcher) {
        val controller = RecordingPlaybackController(
            PlaybackState(
                currentTrackId = "track",
                currentTitle = "Track",
                currentLanguage = "en",
                isPlaying = true,
                positionMs = 5_000,
                durationMs = 12_000,
                hasPrevious = false,
                hasNext = true,
            ),
        )
        val viewModel = createViewModel(controller, FakeTranscriptRepository())
        advanceUntilIdle()

        assertEquals(PlayerStatus.PLAYING, (viewModel.uiState.value as PlayerUiState.Active).status)
        viewModel.seekBackward()
        assertEquals(0L, controller.lastSeekPosition)
        viewModel.seekForward()
        assertEquals(12_000L, controller.lastSeekPosition)
        viewModel.previous()
        assertEquals(0, controller.previousCalls)
        viewModel.next()
        assertEquals(1, controller.nextCalls)
        viewModel.setPlaybackSpeed(1.3f)
        assertEquals(1.25f, controller.lastSpeed)
        viewModel.seekToTranscriptSegment(4_000)
        assertEquals(4_000L, controller.lastSeekPosition)
        viewModel.selectLanguage("ru")
        advanceUntilIdle()
        assertEquals("ru", controller.lastLanguage)
        assertEquals("ru", (viewModel.uiState.value as PlayerUiState.Active).preferredLanguage)
    }

    @Test
    fun `play action retries after playback error`() = runTest(dispatcher) {
        val controller = RecordingPlaybackController(
            PlaybackState(
                currentTrackId = "track",
                currentTitle = "Track",
                error = "Network error",
            ),
        )
        val viewModel = createViewModel(controller, FakeTranscriptRepository())
        advanceUntilIdle()

        viewModel.playOrPause()

        assertEquals(1, controller.retryCalls)
    }

    @Test
    fun `cold restored playback hydrates languages from its playlist`() = runTest(dispatcher) {
        val controller = RecordingPlaybackController(
            PlaybackState(
                currentTrackId = "shorts.android-basics",
                currentTitle = "Android Basics",
                currentPlaylistId = "shorts",
                currentLanguage = "en",
            ),
        )
        val viewModel = createViewModel(
            controller = controller,
            transcriptRepository = FakeTranscriptRepository(),
            catalogRepository = FakePlayerCatalogRepository(shortsManifest()),
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value as PlayerUiState.Active
        assertEquals(listOf("en", "ru"), state.availableLanguages)
        assertEquals("shorts", controller.updatedPlaylistId)
    }

    @Test
    fun `legacy bookmark finds playlist by stable track id`() = runTest(dispatcher) {
        val controller = RecordingPlaybackController(
            PlaybackState(
                currentTrackId = "shorts.android-basics",
                currentTitle = "Android Basics",
                currentLanguage = "en",
            ),
        )
        val viewModel = createViewModel(
            controller = controller,
            transcriptRepository = FakeTranscriptRepository(),
            catalogRepository = FakePlayerCatalogRepository(shortsManifest()),
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value as PlayerUiState.Active
        assertEquals(listOf("en", "ru"), state.availableLanguages)
        assertEquals("shorts", controller.updatedPlaylistId)
    }

    @Test
    fun `loads transcript and maps active segment without blocking playback`() = runTest(dispatcher) {
        val response = CompletableDeferred<TimedTranscript>()
        val controller = RecordingPlaybackController(playbackState("one.srt", positionMs = 1_500))
        val viewModel = createViewModel(
            controller,
            FakeTranscriptRepository { _, _ -> response.await() },
        )
        runCurrent()

        assertEquals(
            TranscriptUiState.Loading,
            (viewModel.uiState.value as PlayerUiState.Active).transcript,
        )

        response.complete(transcript("First"))
        advanceUntilIdle()

        val transcriptState = (viewModel.uiState.value as PlayerUiState.Active).transcript
            as TranscriptUiState.Content
        assertEquals("First", transcriptState.segments.single().text)
        assertEquals(0, transcriptState.activeSegmentIndex)
    }

    @Test
    fun `transcript failure is non blocking`() = runTest(dispatcher) {
        val controller = RecordingPlaybackController(playbackState("broken.srt"))
        val viewModel = createViewModel(
            controller,
            FakeTranscriptRepository { _, _ -> error("Network failure") },
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value as PlayerUiState.Active
        assertEquals(PlayerStatus.PAUSED, state.status)
        assertEquals(TranscriptUiState.Error(), state.transcript)
    }

    @Test
    fun `out of order transcript is rejected without affecting playback`() = runTest(dispatcher) {
        val controller = RecordingPlaybackController(playbackState("out-of-order.srt"))
        val viewModel = createViewModel(
            controller,
            FakeTranscriptRepository { _, _ ->
                SrtParser().parse(
                    """
                    1
                    00:00:05,000 --> 00:00:06,000
                    Later cue first

                    2
                    00:00:01,000 --> 00:00:02,000
                    Earlier cue second
                    """.trimIndent(),
                )
            },
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value as PlayerUiState.Active
        assertEquals(PlayerStatus.PAUSED, state.status)
        assertEquals(TranscriptUiState.Error(), state.transcript)
    }

    @Test
    fun `rendition change ignores stale transcript result`() = runTest(dispatcher) {
        val firstResponse = CompletableDeferred<TimedTranscript>()
        val secondResponse = CompletableDeferred<TimedTranscript>()
        val controller = RecordingPlaybackController(playbackState("en.srt"))
        val viewModel = createViewModel(
            controller,
            FakeTranscriptRepository { url, _ ->
                if (url.endsWith("en.srt")) firstResponse.await() else secondResponse.await()
            },
        )
        runCurrent()

        controller.update(playbackState("ru.srt").copy(currentLanguage = "ru"))
        runCurrent()
        secondResponse.complete(transcript("Russian"))
        advanceUntilIdle()
        firstResponse.complete(transcript("Stale English"))
        advanceUntilIdle()

        val transcriptState = (viewModel.uiState.value as PlayerUiState.Active).transcript
            as TranscriptUiState.Content
        assertEquals("Russian", transcriptState.segments.single().text)
    }

    @Test
    fun `rendition without transcript clears previous transcript immediately`() =
        runTest(dispatcher) {
            val controller = RecordingPlaybackController(playbackState("en.srt"))
            val viewModel = createViewModel(
                controller,
                FakeTranscriptRepository { _, _ -> transcript("English") },
            )
            advanceUntilIdle()
            assertEquals(
                "English",
                ((viewModel.uiState.value as PlayerUiState.Active).transcript
                    as TranscriptUiState.Content).segments.single().text,
            )

            controller.update(
                playbackState("en.srt").copy(
                    currentLanguage = "ru",
                    timedTranscriptUrl = null,
                    timedTranscriptFormat = null,
                ),
            )
            runCurrent()

            assertEquals(
                TranscriptUiState.Unavailable,
                (viewModel.uiState.value as PlayerUiState.Active).transcript,
            )
        }

    private fun createViewModel(
        controller: PlaybackController,
        transcriptRepository: TranscriptRepository,
        catalogRepository: CatalogRepository = FakePlayerCatalogRepository(),
    ): PlayerViewModel {
        val preferences = FakePlayerPreferencesRepository()
        return PlayerViewModel(
            playbackController = controller,
            catalogRepository = catalogRepository,
            transcriptRepository = transcriptRepository,
            playbackPreferencesRepository = preferences,
            switchPlaybackLanguage = SwitchPlaybackLanguage(
                controller,
                transcriptRepository,
                preferences,
            ),
        )
    }

    private fun playbackState(url: String, positionMs: Long = 0) = PlaybackState(
        currentTrackId = "track",
        currentTitle = "Track",
        currentLanguage = "en",
        timedTranscriptUrl = "https://example.com/$url",
        timedTranscriptFormat = "srt",
        positionMs = positionMs,
    )

    private fun transcript(text: String) = TimedTranscript(
        listOf(TranscriptSegment(startMs = 1_000, endMs = 2_000, text = text)),
    )

    private fun shortsManifest() = PlaylistManifest(
        id = "shorts",
        titles = mapOf("en" to "Short Audio Notes"),
        availableLanguages = listOf("en", "ru"),
        tracks = listOf(
            Track(
                id = "shorts.android-basics",
                order = 1,
                titles = mapOf("en" to "Android Basics"),
                renditions = listOf("en", "ru").associateWith { language ->
                    MediaRendition(
                        language = language,
                        audioUrl = "https://example.com/android-basics-$language.mp3",
                        transcriptUrl = null,
                        timedTranscriptUrl = null,
                    )
                },
            ),
        ),
    )
}

private class RecordingPlaybackController(initialState: PlaybackState) : PlaybackController {
    private val mutableState = MutableStateFlow(initialState)
    override val state: StateFlow<PlaybackState> = mutableState
    var lastSeekPosition: Long? = null
    var previousCalls = 0
    var nextCalls = 0
    var retryCalls = 0
    var lastSpeed: Float? = null
    var lastLanguage: String? = null
    var updatedPlaylistId: String? = null

    fun update(state: PlaybackState) {
        mutableState.value = state
    }

    override fun playPlaylist(
        tracks: List<Track>,
        selectedTrackId: String,
        playlistId: String,
        playlistTitle: String?,
        preferredLanguage: String,
    ) = Unit

    override fun updatePlaylistTracks(playlistId: String, tracks: List<Track>) {
        updatedPlaylistId = playlistId
        val currentTrackId = mutableState.value.currentTrackId
        val currentTrack = tracks.firstOrNull { it.id == currentTrackId } ?: return
        mutableState.value = mutableState.value.copy(
            currentPlaylistId = playlistId,
            availableLanguages = currentTrack.availableLanguages,
        )
    }

    override fun getCurrentTrackRendition(preferredLanguage: String) = PlaybackRenditionMetadata(
        language = preferredLanguage,
        timedTranscriptUrl = null,
        timedTranscriptFormat = null,
    )

    override fun switchLanguage(preferredLanguage: String, startPositionMs: Long) {
        lastLanguage = preferredLanguage
    }

    override fun play() = Unit
    override fun pause() = Unit
    override fun retry() {
        retryCalls++
    }
    override fun seekTo(positionMs: Long) {
        lastSeekPosition = positionMs
    }

    override fun next() {
        nextCalls++
    }

    override fun previous() {
        previousCalls++
    }

    override fun setPlaybackSpeed(speed: Float) {
        lastSpeed = speed
    }
}

private class FakePlayerCatalogRepository(
    vararg manifests: PlaylistManifest,
) : CatalogRepository {
    private val manifestsById = manifests.associateBy(PlaylistManifest::id)

    override suspend fun getPlaylists(): List<Playlist> = manifestsById.values.map { manifest ->
        Playlist(
            id = manifest.id,
            titles = manifest.titles,
            availableLanguages = manifest.availableLanguages,
            manifestUrl = "https://example.com/${manifest.id}.json",
        )
    }

    override suspend fun getPlaylist(playlistId: String): PlaylistManifest =
        manifestsById.getValue(playlistId)
}

private class FakePlayerPreferencesRepository : PlaybackPreferencesRepository {
    override val bookmark = MutableStateFlow<PlaybackBookmark?>(null)
    override val preferredLanguage = MutableStateFlow("en")

    override suspend fun saveBookmark(bookmark: PlaybackBookmark) = Unit

    override suspend fun savePreferredLanguage(language: String) {
        preferredLanguage.value = language.trim().lowercase()
    }
}

private class FakeTranscriptRepository(
    private val loader: suspend (String, String) -> TimedTranscript = { _, _ ->
        TimedTranscript(emptyList())
    },
) : TranscriptRepository {
    override suspend fun getTimedTranscript(url: String, format: String): TimedTranscript =
        loader(url, format)
}
