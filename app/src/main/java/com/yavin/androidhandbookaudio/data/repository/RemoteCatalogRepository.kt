package com.yavin.androidhandbookaudio.data.repository

import com.yavin.androidhandbookaudio.data.mapper.toDomainPlaylists
import com.yavin.androidhandbookaudio.data.mapper.toDomain
import com.yavin.androidhandbookaudio.data.remote.CatalogApi
import com.yavin.androidhandbookaudio.domain.model.Playlist
import com.yavin.androidhandbookaudio.domain.model.PlaylistManifest
import com.yavin.androidhandbookaudio.domain.repository.CatalogRepository
import javax.inject.Inject

class RemoteCatalogRepository @Inject constructor(
    private val catalogApi: CatalogApi,
) : CatalogRepository {
    override suspend fun getPlaylists(): List<Playlist> = catalogApi.getCatalog().toDomainPlaylists()

    override suspend fun getPlaylist(playlistId: String): PlaylistManifest {
        val playlist = getPlaylists().firstOrNull { it.id == playlistId }
            ?: throw NoSuchElementException("Playlist not found: $playlistId")
        val manifest = catalogApi.getPlaylistManifest(playlist.manifestUrl).toDomain()
            ?: throw IllegalArgumentException("Invalid playlist manifest: $playlistId")
        require(manifest.id == playlistId) {
            "Playlist manifest ID '${manifest.id}' does not match '$playlistId'"
        }
        return manifest
    }
}
