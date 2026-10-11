package com.yavin.androidhandbookaudio.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptFollowPolicyTest {
    @Test
    fun `active cue stays put inside central band`() {
        assertFalse(isOutsideTranscriptFollowBand(250f, 0, 1_000))
        assertFalse(isOutsideTranscriptFollowBand(500f, 0, 1_000))
        assertFalse(isOutsideTranscriptFollowBand(750f, 0, 1_000))
    }

    @Test
    fun `active cue outside central band needs follow scroll`() {
        assertTrue(isOutsideTranscriptFollowBand(249f, 0, 1_000))
        assertTrue(isOutsideTranscriptFollowBand(751f, 0, 1_000))
        assertFalse(isOutsideTranscriptFollowBand(500f, 500, 500))
    }

    @Test
    fun `auto follow resumes ten seconds after last manual scroll and after fling`() {
        val lastScrollAt = 50_000L
        assertFalse(canResumeTranscriptFollowing(59_999L, lastScrollAt, false))
        assertFalse(canResumeTranscriptFollowing(60_000L, 55_000L, false))
        assertFalse(canResumeTranscriptFollowing(60_000L, lastScrollAt, true))
        assertTrue(canResumeTranscriptFollowing(60_000L, lastScrollAt, false))
        assertTrue(canResumeTranscriptFollowing(60_500L, lastScrollAt, false))
    }

    @Test
    fun `distant cue scroll estimate works in both directions`() {
        val visible = listOf(
            TranscriptCuePosition(index = 5, offset = 0, size = 100),
            TranscriptCuePosition(index = 6, offset = 100, size = 100),
            TranscriptCuePosition(index = 7, offset = 200, size = 100),
        )

        assertEquals(300f, estimateTranscriptScrollDistance(9, visible, 0, 150f)!!, 0.01f)
        assertEquals(-400f, estimateTranscriptScrollDistance(2, visible, 0, 150f)!!, 0.01f)
        assertEquals(0f, estimateTranscriptScrollDistance(6, visible, 0, 150f)!!, 0.01f)
        assertNull(estimateTranscriptScrollDistance(2, emptyList(), 0, 150f))
    }

    @Test
    fun `single visible cue uses its size and spacing for distant estimate`() {
        val visible = listOf(TranscriptCuePosition(index = 0, offset = 0, size = 80))

        assertEquals(216f, estimateTranscriptScrollDistance(3, visible, 12, 100f)!!, 0.01f)
    }
}
