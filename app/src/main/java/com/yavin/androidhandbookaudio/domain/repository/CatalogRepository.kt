package com.yavin.androidhandbookaudio.domain.repository

import com.yavin.androidhandbookaudio.domain.model.Playlist
import com.yavin.androidhandbookaudio.domain.model.PlaylistManifest

interface CatalogRepository {
    suspend fun getPlaylists(): List<Playlist>

    suspend fun getPlaylist(playlistId: String): PlaylistManifest
}
