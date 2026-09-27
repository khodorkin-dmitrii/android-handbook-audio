package com.yavin.androidhandbookaudio.di

import com.yavin.androidhandbookaudio.playback.Media3PlaybackController
import com.yavin.androidhandbookaudio.playback.PlaybackController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlaybackModule {
    @Binds
    @Singleton
    abstract fun bindPlaybackController(implementation: Media3PlaybackController): PlaybackController
}
