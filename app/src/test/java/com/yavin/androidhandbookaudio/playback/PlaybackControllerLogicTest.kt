package com.yavin.androidhandbookaudio.playback

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
}
