package com.yavin.androidhandbookaudio.ui.player

import com.yavin.androidhandbookaudio.playback.PlaybackState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerUiStateTest {
    @Test
    fun `maps active playback state and queue boundaries`() {
        val state = PlaybackState(
            currentTrackId = "shorts.kotlin",
            currentTitle = "Kotlin",
            currentLanguage = "en",
            isPlaying = true,
            positionMs = 15_000,
            durationMs = 60_000,
            playbackSpeed = 1.25f,
            hasPrevious = false,
            hasNext = true,
        ).toPlayerUiState() as PlayerUiState.Active

        assertEquals(PlayerStatus.PLAYING, state.status)
        assertEquals("Kotlin", state.title)
        assertEquals("EN", state.language)
        assertEquals(false, state.hasPrevious)
        assertEquals(true, state.hasNext)
        assertEquals(1.25f, state.playbackSpeed)
    }

    @Test
    fun `maps buffering paused error and no active states`() {
        assertEquals(
            PlayerStatus.BUFFERING,
            (PlaybackState(currentTrackId = "track", isBuffering = true).toPlayerUiState()
                as PlayerUiState.Active).status,
        )
        assertEquals(
            PlayerStatus.PAUSED,
            (PlaybackState(currentTrackId = "track").toPlayerUiState()
                as PlayerUiState.Active).status,
        )
        assertEquals(
            PlayerStatus.ERROR,
            (PlaybackState(currentTrackId = "track", error = "failed").toPlayerUiState()
                as PlayerUiState.Active).status,
        )
        assertTrue(PlaybackState().toPlayerUiState() is PlayerUiState.NoActiveMedia)
    }

    @Test
    fun `mini player is visible only for active media and maps progress`() {
        assertNull(PlaybackState().toPlayerUiState().toMiniPlayerUiState())

        val miniPlayer = PlaybackState(
            currentTrackId = "track",
            currentTitle = "Track",
            isPlaying = true,
            positionMs = 25_000,
            durationMs = 100_000,
        ).toPlayerUiState().toMiniPlayerUiState()

        assertEquals("Track", miniPlayer?.title)
        assertEquals(0.25f, miniPlayer?.progress)
        assertEquals(true, miniPlayer?.isPlaying)
    }

    @Test
    fun `formats playback time and chooses supported speed`() {
        assertEquals("0:00", formatPlaybackTime(-1))
        assertEquals("1:01", formatPlaybackTime(61_999))
        assertEquals("1:01:01", formatPlaybackTime(3_661_000))
        assertEquals(1.25f, selectPlaybackSpeed(1.3f))
        assertEquals(2f, selectPlaybackSpeed(3f))
    }
}
