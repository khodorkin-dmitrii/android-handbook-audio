package com.yavin.androidhandbookaudio.data.repository

import androidx.datastore.preferences.core.preferencesOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackPreferencesMappingTest {
    @Test
    fun `empty preferences do not restore playback`() {
        assertNull(preferencesOf().toPlaybackBookmark())
    }

    @Test
    fun `maps persisted track rendition position and speed`() {
        val bookmark = preferencesOf(
            PlaybackPreferenceKeys.TRACK_ID to "shorts.kotlin",
            PlaybackPreferenceKeys.TITLE to "Kotlin",
            PlaybackPreferenceKeys.LANGUAGE to "ru",
            PlaybackPreferenceKeys.AUDIO_URL to "https://example.com/kotlin-ru.mp3",
            PlaybackPreferenceKeys.POSITION_MS to 42_000L,
            PlaybackPreferenceKeys.PLAYBACK_SPEED to 1.5f,
        ).toPlaybackBookmark()

        requireNotNull(bookmark)
        assertEquals("shorts.kotlin", bookmark.trackId)
        assertEquals("ru", bookmark.language)
        assertEquals(42_000L, bookmark.positionMs)
        assertEquals(1.5f, bookmark.playbackSpeed)
    }

    @Test
    fun `invalid persisted values use safe defaults`() {
        val bookmark = preferencesOf(
            PlaybackPreferenceKeys.TRACK_ID to "shorts.kotlin",
            PlaybackPreferenceKeys.TITLE to "Kotlin",
            PlaybackPreferenceKeys.LANGUAGE to "en",
            PlaybackPreferenceKeys.AUDIO_URL to "https://example.com/kotlin.mp3",
            PlaybackPreferenceKeys.POSITION_MS to -1L,
            PlaybackPreferenceKeys.PLAYBACK_SPEED to 10f,
        ).toPlaybackBookmark()

        requireNotNull(bookmark)
        assertEquals(0L, bookmark.positionMs)
        assertEquals(1f, bookmark.playbackSpeed)
    }
}
