package com.yavin.androidhandbookaudio.playback

import com.yavin.androidhandbookaudio.domain.model.MediaRendition
import com.yavin.androidhandbookaudio.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackControllerLogicTest {
    @Test
    fun `seek clamps to zero and known duration`() {
        assertEquals(0L, clampSeekPosition(-5_000, 60_000))
        assertEquals(30_000L, clampSeekPosition(30_000, 60_000))
        assertEquals(60_000L, clampSeekPosition(70_000, 60_000))
    }

    @Test
    fun `seek only clamps lower bound when duration is unknown`() {
        assertEquals(90_000L, clampSeekPosition(90_000, null))
        assertEquals(0L, clampSeekPosition(-1, null))
    }

    @Test
    fun `available languages come only from current logical track`() {
        val tracksById = listOf(
            track("track-a", "en", "ru"),
            track("track-b", "en"),
        ).associateBy(Track::id)

        assertEquals(listOf("en", "ru"), tracksById.availableLanguagesFor("track-a"))
        assertEquals(listOf("en"), tracksById.availableLanguagesFor("track-b"))
    }

    @Test
    fun `only a restored single item queue is hydrated from playlist metadata`() {
        assertEquals(
            true,
            shouldHydrateRestoredQueue(
                hasRestoredBookmark = true,
                hasUserRequestedPlayback = false,
                hasHydratedQueue = false,
                mediaItemCount = 1,
                containsCurrentTrack = true,
            ),
        )
        assertEquals(
            false,
            shouldHydrateRestoredQueue(
                hasRestoredBookmark = true,
                hasUserRequestedPlayback = false,
                hasHydratedQueue = false,
                mediaItemCount = 2,
                containsCurrentTrack = true,
            ),
        )
    }

    private fun track(id: String, vararg languages: String) = Track(
        id = id,
        order = 0,
        titles = mapOf("en" to id),
        renditions = languages.associateWith { language ->
            MediaRendition(
                language = language,
                audioUrl = "https://example.com/$id-$language.mp3",
                transcriptUrl = null,
                timedTranscriptUrl = null,
            )
        },
    )
}
