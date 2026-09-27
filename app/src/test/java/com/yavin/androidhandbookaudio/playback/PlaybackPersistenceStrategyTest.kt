package com.yavin.androidhandbookaudio.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackPersistenceStrategyTest {
    @Test
    fun `periodic checkpoint waits for configured position interval`() {
        assertFalse(shouldCheckpointPosition(14_999, 0))
        assertTrue(shouldCheckpointPosition(15_000, 0))
        assertTrue(shouldCheckpointPosition(5_000, 20_000))
    }

    @Test
    fun `cold restore requires persistence and never overrides user playback`() {
        assertTrue(
            shouldRestorePlayback(
                isPersistenceLoaded = true,
                hasUserRequestedPlayback = false,
                hasPendingPlayback = false,
                hasCurrentMedia = false,
            ),
        )
        assertFalse(
            shouldRestorePlayback(
                isPersistenceLoaded = true,
                hasUserRequestedPlayback = true,
                hasPendingPlayback = false,
                hasCurrentMedia = false,
            ),
        )
        assertFalse(
            shouldRestorePlayback(
                isPersistenceLoaded = true,
                hasUserRequestedPlayback = false,
                hasPendingPlayback = true,
                hasCurrentMedia = false,
            ),
        )
    }
}
