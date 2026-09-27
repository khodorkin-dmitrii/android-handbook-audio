package com.yavin.androidhandbookaudio.data.remote

import com.yavin.androidhandbookaudio.data.remote.dto.CatalogDto
import com.yavin.androidhandbookaudio.data.remote.dto.PlaylistManifestDto
import retrofit2.http.GET
import retrofit2.http.Url

interface CatalogApi {
    @GET("assets/audio/catalog.json")
    suspend fun getCatalog(): CatalogDto

    @GET
    suspend fun getPlaylistManifest(@Url manifestUrl: String): PlaylistManifestDto
}
