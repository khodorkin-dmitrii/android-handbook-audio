package com.yavin.androidhandbookaudio.navigation

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigationTest {
    @Test
    fun `mini player is shown on playlists when media is active`() {
        assertTrue(shouldShowMiniPlayer(PlaylistsKey, hasActiveMedia = true))
    }

    @Test
    fun `mini player is hidden outside playlists or without active media`() {
        assertFalse(shouldShowMiniPlayer(TrackListKey("shorts"), hasActiveMedia = true))
        assertFalse(shouldShowMiniPlayer(PlayerKey, hasActiveMedia = true))
        assertFalse(shouldShowMiniPlayer(PlaylistsKey, hasActiveMedia = false))
    }

    @Test
    fun `repeated playlist navigation does not duplicate top destination`() {
        val backStack = mutableListOf<NavKey>(PlaylistsKey)
        val destination = TrackListKey("shorts")

        backStack.pushIfNotTop(destination)
        backStack.pushIfNotTop(destination)

        assertEquals(listOf(PlaylistsKey, destination), backStack)
    }

    @Test
    fun `repeated mini player navigation does not duplicate player destination`() {
        val backStack = mutableListOf<NavKey>(PlaylistsKey, TrackListKey("shorts"))

        backStack.pushIfNotTop(PlayerKey)
        backStack.pushIfNotTop(PlayerKey)

        assertEquals(listOf(PlaylistsKey, TrackListKey("shorts"), PlayerKey), backStack)
    }

    @Test
    fun `repeated back callback only removes its own destination once`() {
        val trackList = TrackListKey("shorts")
        val backStack = mutableListOf<NavKey>(PlaylistsKey, trackList, PlayerKey)

        backStack.popIfTop(PlayerKey)
        backStack.popIfTop(PlayerKey)

        assertEquals(listOf(PlaylistsKey, trackList), backStack)
    }
}
