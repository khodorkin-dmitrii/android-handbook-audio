package com.yavin.androidhandbookaudio.playback

import com.yavin.androidhandbookaudio.domain.model.MediaRendition
import com.yavin.androidhandbookaudio.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackMediaItemMapperTest {
    @Test
    fun `explicit language wins when available`() {
        assertEquals("ru", track.selectRendition("ru", "en")?.language)
    }

    @Test
    fun `unavailable explicit language falls back to preferred language`() {
        assertEquals("ru", track.selectRendition("de", "ru")?.language)
    }

    @Test
    fun `available preferred language is selected`() {
        assertEquals("ru", track.selectRendition(preferredLanguage = "ru")?.language)
    }

    @Test
    fun `unavailable preferred language falls back to English`() {
        assertEquals("en", track.selectRendition(preferredLanguage = "de")?.language)
    }

    @Test
    fun `first rendition is used when explicit preferred and English are unavailable`() {
        assertEquals(
            "fr",
            track.copy(renditions = mapOf("fr" to rendition("fr")))
                .selectRendition("de", "ru")
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
        val queueItem = buildPlaybackQueue(listOf(track)).single()

        assertEquals("track", queueItem.trackId)
        assertEquals("Track", queueItem.title)
        assertEquals("en", queueItem.language)
        assertEquals("https://example.com/en.mp3", queueItem.audioUrl)
        assertEquals("https://example.com/en.srt", queueItem.timedTranscriptUrl)
        assertEquals("srt", queueItem.timedTranscriptFormat)
    }

    @Test
    fun `explicit language applies only to selected logical track`() {
        val queue = buildPlaybackQueue(
            tracks = listOf(track.copy(id = "first"), track.copy(id = "second", order = 2)),
            preferredLanguage = "en",
            selectedTrackId = "second",
            selectedLanguage = "ru",
        )

        assertEquals(listOf("en", "ru"), queue.map(PlaybackQueueItem::language))
    }

    @Test
    fun `rendition switch keeps logical ID updates media and resets position preserving state`() {
        val plan = createRenditionSwitchPlan(
            track = track,
            language = "ru",
            playWhenReady = true,
            playbackSpeed = 1.5f,
        )

        requireNotNull(plan)
        assertEquals("track", plan.item.trackId)
        assertEquals("ru", plan.item.language)
        assertEquals("https://example.com/ru.mp3", plan.item.audioUrl)
        assertEquals("https://example.com/ru.srt", plan.item.timedTranscriptUrl)
        assertEquals("srt", plan.item.timedTranscriptFormat)
        assertEquals(true, plan.playWhenReady)
        assertEquals(1.5f, plan.playbackSpeed)
    }

    @Test
    fun `paused rendition switch remains paused`() {
        val plan = createRenditionSwitchPlan(track, "ru", false, 1.25f)

        requireNotNull(plan)
        assertEquals(false, plan.playWhenReady)
        assertEquals(1.25f, plan.playbackSpeed)
    }

    @Test
    fun `rendition without transcript maps transcript metadata to null`() {
        val noTranscriptTrack = track.copy(
            renditions = track.renditions + ("ru" to rendition("ru")),
        )

        val plan = createRenditionSwitchPlan(noTranscriptTrack, "ru", true, 1f)

        requireNotNull(plan)
        assertEquals(null, plan.item.timedTranscriptUrl)
        assertEquals(null, plan.item.timedTranscriptFormat)
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
