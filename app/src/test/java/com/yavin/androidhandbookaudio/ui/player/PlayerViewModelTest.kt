package com.yavin.androidhandbookaudio.ui.player

import com.yavin.androidhandbookaudio.domain.model.Track
import com.yavin.androidhandbookaudio.playback.PlaybackController
import com.yavin.androidhandbookaudio.playback.PlaybackState
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
                isPlaying = true,
                positionMs = 5_000,
                durationMs = 12_000,
                hasPrevious = false,
                hasNext = true,
            ),
        )
        val viewModel = PlayerViewModel(controller)
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
    }
}

private class RecordingPlaybackController(initialState: PlaybackState) : PlaybackController {
    private val mutableState = MutableStateFlow(initialState)
    override val state: StateFlow<PlaybackState> = mutableState
    var lastSeekPosition: Long? = null
    var previousCalls = 0
    var nextCalls = 0
    var lastSpeed: Float? = null

    override fun playPlaylist(
        tracks: List<Track>,
        selectedTrackId: String,
        preferredLanguage: String?,
    ) = Unit

    override fun play() = Unit
    override fun pause() = Unit
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
