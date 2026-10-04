package com.yavin.androidhandbookaudio.playback

import com.yavin.androidhandbookaudio.domain.model.MediaRendition
import com.yavin.androidhandbookaudio.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackMediaItemMapperTest {
    @Test
    fun `available preferred language is selected`() {
        assertEquals("ru", track.selectRendition(preferredLanguage = "ru")?.language)
    }

    @Test
    fun `unavailable preferred language falls back to English`() {
        val englishOnly = track.copy(renditions = mapOf("en" to rendition("en")))

        assertEquals("en", englishOnly.selectRendition(preferredLanguage = "ru")?.language)
    }

    @Test
    fun `first rendition is used when preferred and English are unavailable`() {
        assertEquals(
            "fr",
            track.copy(renditions = mapOf("fr" to rendition("fr")))
                .selectRendition("ru")
                ?.language,
        )
    }

    @Test
    fun `single language track selects its only rendition`() {
        val single = track.copy(renditions = mapOf("ru" to rendition("ru")))

        assertEquals("ru", single.selectRendition(preferredLanguage = "de")?.language)
    }

    @Test
    fun `builds queue in track order with stable IDs and selected rendition URL`() {
        val queue = buildPlaybackQueue(
            tracks = listOf(track.copy(id = "second", order = 2), track.copy(id = "first", order = 1)),
            preferredLanguage = "ru",
        )

        assertEquals(listOf("first", "second"), queue.map { it.trackId })
        assertEquals(listOf("ru", "ru"), queue.map { it.language })
        assertEquals("https://example.com/ru.mp3", queue.first().audioUrl)
    }

    @Test
    fun `maps track to queue item with stable logical ID title language and URL`() {
        val queueItem = buildPlaybackQueue(
            tracks = listOf(track),
            playlistId = "shorts",
            playlistTitle = "Short Audio Notes",
        ).single()

        assertEquals("track", queueItem.trackId)
        assertEquals("shorts", queueItem.playlistId)
        assertEquals("Track", queueItem.title)
        assertEquals("Short Audio Notes", queueItem.playlistTitle)
        assertEquals("en", queueItem.language)
        assertEquals("https://example.com/en.mp3", queueItem.audioUrl)
        assertEquals("https://example.com/en.srt", queueItem.timedTranscriptUrl)
        assertEquals("srt", queueItem.timedTranscriptFormat)
    }

    @Test
    fun `preferred language applies to every track in queue`() {
        val queue = buildPlaybackQueue(
            tracks = listOf(track.copy(id = "first"), track.copy(id = "second", order = 2)),
            preferredLanguage = "ru",
        )

        assertEquals(listOf("ru", "ru"), queue.map(PlaybackQueueItem::language))
    }

    @Test
    fun `queue uses preferred language with per-track English and first-rendition fallback`() {
        val queue = buildPlaybackQueue(
            tracks = listOf(
                track.copy(id = "has-ru", order = 1),
                track.copy(
                    id = "english-only",
                    order = 2,
                    renditions = mapOf("en" to rendition("en")),
                ),
                track.copy(
                    id = "french-only",
                    order = 3,
                    renditions = mapOf("fr" to rendition("fr")),
                ),
            ),
            preferredLanguage = "ru",
        )

        assertEquals(listOf("ru", "en", "fr"), queue.map(PlaybackQueueItem::language))
    }

    private companion object {
        fun rendition(language: String) = MediaRendition(
            language = language,
            audioUrl = "https://example.com/$language.mp3",
            transcriptUrl = null,
            timedTranscriptUrl = null,
            timedTranscriptFormat = null,
        )

        val track = Track(
            id = "track",
            order = 1,
            titles = mapOf("en" to "Track", "ru" to "Трек"),
            renditions = mapOf(
                "en" to rendition("en").copy(
                    timedTranscriptUrl = "https://example.com/en.srt",
                    timedTranscriptFormat = "srt",
                ),
                "ru" to rendition("ru").copy(
                    timedTranscriptUrl = "https://example.com/ru.srt",
                    timedTranscriptFormat = "srt",
                ),
            ),
        )
    }
}
