package com.yavin.androidhandbookaudio.playback

import com.yavin.androidhandbookaudio.domain.model.TimedTranscript
import com.yavin.androidhandbookaudio.domain.model.Track
import com.yavin.androidhandbookaudio.domain.model.TranscriptSegment
import com.yavin.androidhandbookaudio.domain.model.PlaybackBookmark
import com.yavin.androidhandbookaudio.domain.repository.PlaybackPreferencesRepository
import com.yavin.androidhandbookaudio.domain.repository.TranscriptRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SwitchPlaybackLanguageTest {
    @Test
    fun `maps current phrase index to target phrase start`() {
        val current = transcript(1_000, 5_000, 9_000)
        val target = transcript(2_000, 7_000, 13_000)

        assertEquals(13_000L, findEquivalentCueStartMs(current, target, 10_000))
        assertEquals(7_000L, findEquivalentCueStartMs(current, target, 8_000))
    }

    @Test
    fun `uses last started phrase while position is between cues`() {
        val current = transcript(1_000, 10_000, 20_000)
        val target = transcript(2_000, 12_000, 25_000)

        assertEquals(12_000L, findEquivalentCueStartMs(current, target, 19_000))
    }

    @Test
    fun `cannot map before first cue or when cue counts differ`() {
        assertNull(findEquivalentCueStartMs(transcript(1_000), transcript(2_000), 500))
        assertNull(findEquivalentCueStartMs(transcript(1_000, 2_000), transcript(3_000), 2_500))
    }

    @Test
    fun `switches to equivalent target cue when both transcripts match`() = runTest {
        val controller = RecordingLanguageController(
            state = playbackState(positionMs = 9_500),
            target = targetMetadata(),
        )
        val current = transcript(1_000, 5_000, 9_000)
        val target = transcript(2_000, 7_000, 13_000)
        val useCase = SwitchPlaybackLanguage(
            controller,
            MapTranscriptRepository(mapOf("ru.srt" to target)),
            RecordingPlaybackPreferencesRepository(),
        )

        useCase("ru", loadedCurrentTranscript = current)

        assertEquals("ru", controller.switchedLanguage)
        assertEquals(13_000L, controller.switchPositionMs)
    }

    @Test
    fun `falls back to zero when transcript is unavailable or incompatible`() = runTest {
        val controller = RecordingLanguageController(
            state = playbackState(positionMs = 9_500),
            target = targetMetadata(),
        )
        val useCase = SwitchPlaybackLanguage(
            controller,
            MapTranscriptRepository(mapOf("ru.srt" to transcript(2_000))),
            RecordingPlaybackPreferencesRepository(),
        )

        useCase("ru", loadedCurrentTranscript = transcript(1_000, 5_000))

        assertEquals(0L, controller.switchPositionMs)
    }

    @Test
    fun `invalid target transcript is not used for cue based language mapping`() = runTest {
        val controller = RecordingLanguageController(
            state = playbackState(positionMs = 9_500),
            target = targetMetadata(),
        )
        val useCase = SwitchPlaybackLanguage(
            controller,
            object : TranscriptRepository {
                override suspend fun getTimedTranscript(
                    url: String,
                    format: String,
                ): TimedTranscript = throw IllegalArgumentException("Out-of-order SRT")
            },
            RecordingPlaybackPreferencesRepository(),
        )

        useCase("ru", loadedCurrentTranscript = transcript(1_000, 5_000, 9_000))

        assertEquals(0L, controller.switchPositionMs)
    }

    @Test
    fun `persists global preference even when current rendition falls back`() = runTest {
        val preferences = RecordingPlaybackPreferencesRepository()
        val controller = RecordingLanguageController(
            state = playbackState(positionMs = 9_500),
            target = targetMetadata().copy(language = "en"),
        )
        val useCase = SwitchPlaybackLanguage(
            controller,
            MapTranscriptRepository(emptyMap()),
            preferences,
        )

        useCase("ru")

        assertEquals("ru", preferences.preferredLanguage.value)
        assertEquals("ru", controller.switchedLanguage)
        assertEquals(9_500L, controller.switchPositionMs)
    }

    private fun playbackState(positionMs: Long) = PlaybackState(
        currentTrackId = "track",
        currentLanguage = "en",
        timedTranscriptUrl = "https://example.com/en.srt",
        timedTranscriptFormat = "srt",
        positionMs = positionMs,
    )

    private fun targetMetadata() = PlaybackRenditionMetadata(
        language = "ru",
        timedTranscriptUrl = "https://example.com/ru.srt",
        timedTranscriptFormat = "srt",
    )

    private fun transcript(vararg starts: Long) = TimedTranscript(
        starts.mapIndexed { index, start ->
            TranscriptSegment(start, start + 1_000, "Cue $index")
        },
    )
}

private class RecordingLanguageController(
    state: PlaybackState,
    private val target: PlaybackRenditionMetadata?,
) : PlaybackController {
    private val mutableState = MutableStateFlow(state)
    override val state: StateFlow<PlaybackState> = mutableState
    var switchedLanguage: String? = null
    var switchPositionMs: Long? = null

    override fun getCurrentTrackRendition(preferredLanguage: String) = target

    override fun switchLanguage(preferredLanguage: String, startPositionMs: Long) {
        switchedLanguage = preferredLanguage
        switchPositionMs = startPositionMs
    }

    override fun playPlaylist(
        tracks: List<Track>,
        selectedTrackId: String,
        playlistTitle: String?,
        preferredLanguage: String,
    ) = Unit

    override fun updatePlaylistTracks(tracks: List<Track>) = Unit
    override fun play() = Unit
    override fun pause() = Unit
    override fun retry() = Unit
    override fun seekTo(positionMs: Long) = Unit
    override fun next() = Unit
    override fun previous() = Unit
    override fun setPlaybackSpeed(speed: Float) = Unit
}

private class RecordingPlaybackPreferencesRepository : PlaybackPreferencesRepository {
    override val bookmark = MutableStateFlow<PlaybackBookmark?>(null)
    override val preferredLanguage = MutableStateFlow("en")

    override suspend fun saveBookmark(bookmark: PlaybackBookmark) = Unit

    override suspend fun savePreferredLanguage(language: String) {
        preferredLanguage.value = language.trim().lowercase()
    }
}

private class MapTranscriptRepository(
    private val transcripts: Map<String, TimedTranscript>,
) : TranscriptRepository {
    override suspend fun getTimedTranscript(url: String, format: String): TimedTranscript =
        transcripts.entries.firstOrNull { (suffix, _) -> url.endsWith(suffix) }?.value
            ?: error("No transcript for $url")
}
