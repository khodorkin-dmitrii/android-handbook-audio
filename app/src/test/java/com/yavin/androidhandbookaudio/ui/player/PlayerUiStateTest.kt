package com.yavin.androidhandbookaudio.ui.player

import com.yavin.androidhandbookaudio.playback.PlaybackState
import com.yavin.androidhandbookaudio.domain.model.TranscriptSegment
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
            currentPlaylistTitle = "Short Audio Notes",
            currentLanguage = "en",
            availableLanguages = listOf("en", "ru"),
            isPlaying = true,
            positionMs = 15_000,
            durationMs = 60_000,
            playbackSpeed = 1.25f,
            hasPrevious = false,
            hasNext = true,
        ).toPlayerUiState() as PlayerUiState.Active

        assertEquals(PlayerStatus.PLAYING, state.status)
        assertEquals("Kotlin", state.title)
        assertEquals("Short Audio Notes", state.playlistTitle)
        assertEquals("en", state.language)
        assertEquals(listOf("en", "ru"), state.availableLanguages)
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
    fun `keeps preferred language distinct from fallback rendition`() {
        val state = PlaybackState(
            currentTrackId = "english-only",
            currentLanguage = "en",
            availableLanguages = listOf("en"),
        ).toPlayerUiState(preferredLanguage = "ru") as PlayerUiState.Active

        assertEquals("ru", state.preferredLanguage)
        assertEquals("en", state.language)
        assertEquals(listOf("en", "ru"), state.availableLanguages)
    }

    @Test
    fun `transcript panel remains available while rendition transcript loads or is unavailable`() {
        val playback = PlaybackState(currentTrackId = "track", hasTimedTranscript = true)

        assertEquals(
            true,
            (playback.toPlayerUiState(TranscriptUiState.Loading) as PlayerUiState.Active)
                .hasTranscriptPanel,
        )
        assertEquals(
            true,
            (playback.toPlayerUiState(TranscriptUiState.Unavailable) as PlayerUiState.Active)
                .hasTranscriptPanel,
        )
        assertEquals(
            false,
            (playback.copy(hasTimedTranscript = false)
                .toPlayerUiState(TranscriptUiState.Unavailable) as PlayerUiState.Active)
                .hasTranscriptPanel,
        )
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
        assertEquals(25_000L, miniPlayer?.positionMs)
        assertEquals(100_000L, miniPlayer?.durationMs)
        assertEquals(true, miniPlayer?.isPlaying)
    }

    @Test
    fun `mini player exposes retry state for playback errors`() {
        val miniPlayer = PlaybackState(
            currentTrackId = "track",
            error = "failed",
        ).toPlayerUiState().toMiniPlayerUiState()

        assertEquals(true, miniPlayer?.hasError)
        assertEquals(false, miniPlayer?.isPlaying)
        assertEquals(false, miniPlayer?.isBuffering)
    }

    @Test
    fun `formats playback time and chooses supported speed`() {
        assertEquals("0:00", formatPlaybackTime(-1))
        assertEquals("1:01", formatPlaybackTime(61_999))
        assertEquals("1:01:01", formatPlaybackTime(3_661_000))
        assertEquals(1.25f, selectPlaybackSpeed(1.3f))
        assertEquals(2f, selectPlaybackSpeed(3f))
    }

    @Test
    fun `maps only supported complete transcript source`() {
        assertEquals(
            TimedTranscriptSource("https://example.com/file.srt", "srt"),
            PlaybackState(
                timedTranscriptUrl = "https://example.com/file.srt",
                timedTranscriptFormat = "SRT",
            ).toTimedTranscriptSourceOrNull(),
        )
        assertNull(
            PlaybackState(
                timedTranscriptUrl = "https://example.com/file.vtt",
                timedTranscriptFormat = "vtt",
            ).toTimedTranscriptSourceOrNull(),
        )
        assertNull(PlaybackState(timedTranscriptFormat = "srt").toTimedTranscriptSourceOrNull())
    }

    @Test
    fun `finds active segment and returns null outside cues and in gaps`() {
        val segments = listOf(
            TranscriptSegment(1_000, 2_000, "One"),
            TranscriptSegment(3_000, 4_000, "Two"),
        )

        assertNull(findActiveTranscriptSegmentIndex(segments, 999))
        assertEquals(0, findActiveTranscriptSegmentIndex(segments, 1_000))
        assertEquals(0, findActiveTranscriptSegmentIndex(segments, 1_999))
        assertNull(findActiveTranscriptSegmentIndex(segments, 2_000))
        assertNull(findActiveTranscriptSegmentIndex(segments, 2_500))
        assertEquals(1, findActiveTranscriptSegmentIndex(segments, 3_500))
        assertNull(findActiveTranscriptSegmentIndex(segments, 4_000))
    }
}
