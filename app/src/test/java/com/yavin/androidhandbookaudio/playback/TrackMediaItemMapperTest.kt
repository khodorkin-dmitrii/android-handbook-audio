package com.yavin.androidhandbookaudio.playback

import com.yavin.androidhandbookaudio.domain.model.MediaRendition
import com.yavin.androidhandbookaudio.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackMediaItemMapperTest {
    @Test
    fun `selects preferred then English then first rendition`() {
        assertEquals("ru", track.renditionFor("ru")?.language)
        assertEquals("en", track.renditionFor("de")?.language)
        assertEquals(
            "fr",
            track.copy(renditions = mapOf("fr" to rendition("fr"))).renditionFor(null)?.language,
        )
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
        val queueItem = buildPlaybackQueue(listOf(track)).single()

        assertEquals("track", queueItem.trackId)
        assertEquals("Track", queueItem.title)
        assertEquals("en", queueItem.language)
        assertEquals("https://example.com/en.mp3", queueItem.audioUrl)
        assertEquals("https://example.com/en.srt", queueItem.timedTranscriptUrl)
        assertEquals("srt", queueItem.timedTranscriptFormat)
    }

    private fun Track.renditionFor(language: String?) = selectRendition(language)

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
                "ru" to rendition("ru"),
            ),
        )
    }
}
