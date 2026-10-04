package com.yavin.androidhandbookaudio.ui.tracks

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackListInteractionTest {
    @Test
    fun `active card opens player without toggling playback`() {
        val events = mutableListOf<String>()

        dispatchTrackItemClick(
            target = TrackItemClickTarget.CARD,
            trackId = "active",
            isCurrent = true,
            onPlayOrPause = { events += "playback:$it" },
            onOpenPlayer = { events += "player" },
        )

        assertEquals(listOf("player"), events)
    }

    @Test
    fun `inactive card starts playback then opens player`() {
        val events = mutableListOf<String>()

        dispatchTrackItemClick(
            target = TrackItemClickTarget.CARD,
            trackId = "inactive",
            isCurrent = false,
            onPlayOrPause = { events += "playback:$it" },
            onOpenPlayer = { events += "player" },
        )

        assertEquals(listOf("playback:inactive", "player"), events)
    }

    @Test
    fun `inactive play control starts playback without navigation`() {
        val events = mutableListOf<String>()

        dispatchTrackItemClick(
            target = TrackItemClickTarget.PLAYBACK_CONTROL,
            trackId = "inactive",
            isCurrent = false,
            onPlayOrPause = { events += "playback:$it" },
            onOpenPlayer = { events += "player" },
        )

        assertEquals(listOf("playback:inactive"), events)
    }

    @Test
    fun `active play pause control toggles playback without navigation`() {
        val events = mutableListOf<String>()

        dispatchTrackItemClick(
            target = TrackItemClickTarget.PLAYBACK_CONTROL,
            trackId = "active",
            isCurrent = true,
            onPlayOrPause = { events += "playback:$it" },
            onOpenPlayer = { events += "player" },
        )

        assertEquals(listOf("playback:active"), events)
    }
}
