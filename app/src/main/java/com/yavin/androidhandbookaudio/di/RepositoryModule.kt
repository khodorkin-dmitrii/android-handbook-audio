package com.yavin.androidhandbookaudio.di

import com.yavin.androidhandbookaudio.data.repository.RemoteCatalogRepository
import com.yavin.androidhandbookaudio.data.repository.DataStorePlaybackPreferencesRepository
import com.yavin.androidhandbookaudio.data.repository.DataStoreThemePreferencesRepository
import com.yavin.androidhandbookaudio.data.repository.RemoteTranscriptRepository
import com.yavin.androidhandbookaudio.domain.repository.CatalogRepository
import com.yavin.androidhandbookaudio.domain.repository.PlaybackPreferencesRepository
import com.yavin.androidhandbookaudio.domain.repository.TranscriptRepository
import com.yavin.androidhandbookaudio.domain.repository.ThemePreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindCatalogRepository(implementation: RemoteCatalogRepository): CatalogRepository

    @Binds
    @Singleton
    abstract fun bindPlaybackPreferencesRepository(
        implementation: DataStorePlaybackPreferencesRepository,
    ): PlaybackPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindThemePreferencesRepository(
        implementation: DataStoreThemePreferencesRepository,
    ): ThemePreferencesRepository

    @Binds
    @Singleton
    abstract fun bindTranscriptRepository(
        implementation: RemoteTranscriptRepository,
    ): TranscriptRepository
}
