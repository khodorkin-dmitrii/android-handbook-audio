package com.yavin.androidhandbookaudio.di

import com.yavin.androidhandbookaudio.data.repository.RemoteCatalogRepository
import com.yavin.androidhandbookaudio.domain.repository.CatalogRepository
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
}
